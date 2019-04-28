/*
 * Copyright 2018-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.block_extractor;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A parser that walks through a file and returns all found {@link CodeBlock}s.
 *
 * @author Adam
 */
public class BlockParser implements Closeable {
    
    private @NonNull LineNumberReader in;
    
    private @NonNull File sourceFile;
    
    private CppConditionParser conditionParser;
    
    /**
     * All blocks that are not nested inside other blocks.
     */
    private @NonNull List<@NonNull CodeBlock> topBlocks;
    
    /**
     * The current nesting hierarchy. May be empty.
     */
    private @NonNull Deque<@NonNull CodeBlock> nesting;
    
    /**
     * The list of conditions of the previous #if and #elif siblings. Used to construct the negated conditions for
     * #elif and #else. A stack to preserve nesting information.
     * <p>
     * A starting #if, #ifdef or #ifndef pushes a new list with its condition as first element.
     * An #else clears the list, so that any following #else or #elifs throw an exception.
     */
    private @NonNull Deque<@NonNull List<@NonNull Formula>> previousConditions;
    
    /**
     * Whether we are currently inside an inline comment.
     */
    private boolean inInlineComment;
    
    /**
     * Use this instead of in.getLineNumber() because we may join multiple lines together because of continuation with
     * '\'.
     */
    private int currentLineNumber;
    
    /**
     * Creates a parser for the given input. Fuzzy parsing and Linux replacements are disabled.
     * Invalid condition handling is set to {@link InvalidConditionHandling#EXCEPTION}.
     * 
     * @param in The reader to get the input from. Internally, this will be wrapped into a {@link BufferedReader},
     *      so passing an unbuffered reader here is ok.
     * @param sourceFile The source file to specify in the {@link CodeBlock}s.
     */
    public BlockParser(@NonNull Reader in, @NonNull File sourceFile) {
        this(in, sourceFile, false, false, InvalidConditionHandling.EXCEPTION);
    }
    
    /**
     * Creates a parser for the given input.
     * 
     * @param in The reader to get the input from. Internally, this will be wrapped into a {@link BufferedReader},
     *      so passing an unbuffered reader here is ok.
     * @param sourceFile The source file to specify in the {@link CodeBlock}s.
     * @param handleLinuxMacros Whether to handle preprocessor macros found in the Linux Kernel (i.e.
     *      IS_ENABLED, IS_BUILTIN, IS_MODULE).
     * @param fuzzyParsing Whether to do fuzzy parsing for non-boolean integer comparisons.
     * @param invalidConditionHandling How to handle unparseable conditions.
     */
    public BlockParser(@NonNull Reader in, @NonNull File sourceFile, boolean handleLinuxMacros,
            boolean fuzzyParsing, @NonNull InvalidConditionHandling invalidConditionHandling) {
        
        this.in = new LineNumberReader(in);
        this.sourceFile = sourceFile;
        
        this.conditionParser = new CppConditionParser(handleLinuxMacros, fuzzyParsing, invalidConditionHandling);
        
        this.topBlocks = new LinkedList<>();
        this.nesting = new LinkedList<>();
        this.previousConditions = new LinkedList<>();
    }
    
    /**
     * Finds all {@link CodeBlock}s in the given input.
     * 
     * @return The list of top-level blocks.
     * 
     * @throws IOException If reading the input reader fails.
     * @throws FormatException If the source file is not formatted correctly.
     */
    public @NonNull List<@NonNull CodeBlock> readBlocks() throws IOException, FormatException {
        boolean foundContentOutsideTopBlocks = false;
        
        String line;
        while ((line = in.readLine()) != null) {
            currentLineNumber = in.getLineNumber();
            StringBuilder lineBuffer = new StringBuilder(line.trim());
            
            if (lineBuffer.length() > 0 && lineBuffer.charAt(0) == '#') {
                // spaces after hash
                while (lineBuffer.length() > 1 && Character.isWhitespace(lineBuffer.charAt(1))) {
                    lineBuffer.replace(1, 2, "");
                }
                
                // line continuation
                while (lineBuffer.charAt(lineBuffer.length() - 1) == '\\') {
                    // remove trailing \
                    lineBuffer.replace(lineBuffer.length() - 1, lineBuffer.length(), "");
                    
                    String next = in.readLine();
                    if (next != null) {
                        lineBuffer.append(next);
                    }
                }
            }
            
            line = removeComments(lineBuffer.toString()).trim();
            
            if (line.startsWith("#ifdef")) {
                handleIf("defined(" + line.substring("#ifdef".length()).trim() + ")");
                
            } else if (line.startsWith("#ifndef")) {
                handleIf("!defined(" + line.substring("#ifndef".length()).trim() + ")");
            
            } else if (line.startsWith("#if")) {
                handleIf(notNull(line.substring("#if".length())));
                
            } else if (line.startsWith("#elif")) {
                handleElif(notNull(line.substring("#elif".length())));
                
            } else if (line.startsWith("#else")) {
                handleElse();
                
            } else if (line.startsWith("#endif")) {
                handleEndif();
                
            } else if (!foundContentOutsideTopBlocks && !line.isEmpty() && nesting.isEmpty()) {
                // we found a non-whitespace character outside of all #if blocks
                foundContentOutsideTopBlocks = true;
            }
        }
        
        if (!nesting.isEmpty()) {
            throw new FormatException("Found opening at line " + notNull(nesting.peek()).getLineStart()
                    + " but no closing #endif");
        }
        
        return buildResult(foundContentOutsideTopBlocks);
    }

    /**
     * Builds the final list of top blocks from {@link #topBlocks}. If foundContentOutsideTopBlocks is
     * <code>true</code>, then a pseudo block is added for the while file and the {@link #topBlocks} are nested
     * inside of it.
     * 
     * @param foundContentOutsideTopBlocks Whether non-whitespace characters were found outside of all blocks.
     * 
     * @return The final list of top-blocks for this file.
     */
    private @NonNull List<@NonNull CodeBlock> buildResult(boolean foundContentOutsideTopBlocks) {
        List<@NonNull CodeBlock> result;
        if (foundContentOutsideTopBlocks) {
            // if we found code outside of #ifdefs, then add a pseudo block for the whole file
            
            // use currentLineNumber + 1 because of trailing \n
            CodeBlock topElement = new CodeBlock(1, currentLineNumber + 1, sourceFile, True.INSTANCE, True.INSTANCE);
            for (CodeBlock element : topBlocks) {
                topElement.addNestedElement(element);
            }
            result = notNull(Arrays.asList(topElement));
            
        } else {
            result = topBlocks;
        }
        return result;
    }
    
    /**
     * Builds a new block with the given condition and adds it to the {@link #nesting}.
     * 
     * @param condition The immediate condition of this block.
     */
    private void buildBlock(@NonNull Formula condition) {
        Formula pc;
        if (!nesting.isEmpty()) {
            pc = new Conjunction(notNull(nesting.peek()).getPresenceCondition(), condition);
        } else {
            pc = condition;
        }
        
        CodeBlock newBlock = new CodeBlock(currentLineNumber, -1, sourceFile, condition, pc);
        nesting.push(newBlock);
    }
    
    /**
     * Call this when the block at the top of {@link #nesting} is finished. Pops it from {@link #nesting}, sets its
     * end line number and adds it to the surround block (or {@link #topBlocks} if its a top block).
     */
    private void finishBlock() {
        CodeBlock block = notNull(nesting.pop());
        
        // copy to set the end line // TODO: this is not ideal....
        List<@NonNull CodeBlock> nested = new ArrayList<>(block.getNestedElementCount());
        for (CodeBlock child : block) {
            nested.add(child);
        }
        block = new CodeBlock(block.getLineStart(), currentLineNumber - 1, sourceFile, block.getCondition(),
                block.getPresenceCondition());
        for (CodeBlock child : nested) {
            block.addNestedElement(child);
        }
        
        if (nesting.isEmpty()) {
            topBlocks.add(block);
        } else {
            notNull(nesting.peek()).addNestedElement(block);
        }
    }
    
    /**
     * Handles an #if line. Called by the main parsing loop if it is determined that the current line is an #if,
     * #ifdef or #ifdef.
     * 
     * @param expression The condition expression containing defined() calls.
     * 
     * @throws FormatException If handling the #if fails.
     */
    private void handleIf(@NonNull String expression) throws FormatException {
        Formula condition;
        try {
            condition = conditionParser.parse(expression);
        } catch (ExpressionFormatException e) {
            throw new FormatException("Can't parse expression in line " + currentLineNumber + ": " + expression, e);
        }
        List<@NonNull Formula> previousConditions = new LinkedList<>();
        previousConditions.add(condition);
        this.previousConditions.push(previousConditions);
        
        buildBlock(condition);
    }
    
    /**
     * Handles an #elif line. Called by the main parsing loop if it is determined that the current line is an #elif.
     * 
     * @param expression The condition expression containing defined() calls.
     * 
     * @throws FormatException If handling the #elif fails.
     */
    private void handleElif(@NonNull String expression) throws FormatException {
        if (previousConditions.isEmpty()) {
            throw new FormatException("Found #elif in line " + currentLineNumber + " with no previous #if condition");
        }
        List<@NonNull Formula> previousConditions = notNull(this.previousConditions.peek());
        if (previousConditions.isEmpty()) {
            throw new FormatException("Found #elif in line " + currentLineNumber + " after an #else condition");
        }
        
        Formula condition;
        try {
            condition = conditionParser.parse(expression);
        } catch (ExpressionFormatException e) {
            throw new FormatException("Can't parse expression in line " + currentLineNumber + ": " + expression, e);
        }

        // build conjunction over all negated previous conditions
        Iterator<@NonNull Formula> previousIterator = previousConditions.iterator();
        Formula notPrevious = new Negation(previousIterator.next());
        while (previousIterator.hasNext()) {
            notPrevious = new Conjunction(notPrevious, new Negation(previousIterator.next()));
        }
        
        // add our immediate condition to the list of previous conditions
        previousConditions.add(condition);
        
        condition = new Conjunction(notPrevious, condition);
        
        finishBlock(); // finish the previous #if or #elif
        buildBlock(condition);
    }
    
    /**
     * Handles an #else line. Called by the main parsing loop if it is determined that the current line is an #else.
     * 
     * @throws FormatException If handling the #else fails.
     */
    private void handleElse() throws FormatException {
        if (previousConditions.isEmpty()) {
            throw new FormatException("Found #else in line " + currentLineNumber + " with no previous #if condition");
        }
        List<@NonNull Formula> previousConditions = notNull(this.previousConditions.peek());
        if (previousConditions.isEmpty()) {
            throw new FormatException("Found #else in line " + currentLineNumber + " after an #else condition");
        }
        
        // build conjunction over all negated previous conditions
        Iterator<@NonNull Formula> previousIterator = previousConditions.iterator();
        Formula notPrevious = new Negation(previousIterator.next());
        while (previousIterator.hasNext()) {
            notPrevious = new Conjunction(notPrevious, new Negation(previousIterator.next()));
        }
        
        // clear previousConditions, because no more #elif or #else is allowed after this
        previousConditions.clear();
        
        Formula condition = notPrevious;
        
        finishBlock(); // finish the previous #if or #elif
        buildBlock(condition);
    }
    
    /**
     * Handles an #endif line. Called by the main parsing loop if it is determined that the current line is an #endif.
     * 
     * @throws FormatException If handling the #endif fails.
     */
    private void handleEndif() throws FormatException {
        if (nesting.isEmpty()) {
            throw new FormatException("Found #endif with no corresponding opening in line "
                    + currentLineNumber);
        }
        
        finishBlock();
        
        previousConditions.pop();
    }
    
    /**
     * Removes inline (/* ... *&#47;)  and line (//) comments from the given line. Uses the {@link #inInlineComment}
     * attribute to keep track whether we are inside an inline comment.
     * 
     * @param line The line to remove the comments from.
     * @return The line with comments removed.
     */
    private String removeComments(String line) {
        String replaced = line;
        
        if (line.indexOf('/') != -1) {
            StringBuilder result = new StringBuilder();
            
            char[] chars = line.toCharArray();
            
            for (int i = 0; i < chars.length; i++) {
                if (inInlineComment) {
                    if (chars[i] == '/' && i > 0 && chars[i - 1] == '*') {
                        inInlineComment = false;
                    }
                    
                } else {
                    if (chars[i] == '/' && i + 1 < chars.length && chars[i + 1] == '/') {
                        break; // line comment, everything from now on is removed
                    } else if (chars[i] == '/' && i + 1 < chars.length && chars[i + 1] == '*') {
                        inInlineComment = true;
                    } else {
                        result.append(chars[i]);
                    }
                }
            }
            
            replaced = result.toString();
            
        } else if (inInlineComment) {
            replaced = "";
        }
        
        return replaced;
    }
    
    /**
     * Closes the input reader that was passed to this parser in the constructor.
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
    
}
