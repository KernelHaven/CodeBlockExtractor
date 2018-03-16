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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.logic.parser.VariableCache;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A parser that walks through a file and returns all found {@link CodeBlock}s.
 *
 * @author Adam
 */
class BlockParser implements Closeable {
    
    private static final Pattern DEFINED_WITH_SPACE_PATTERN = Pattern.compile("defined\\s+\\(\\s*(\\w+)\\s*\\)");
    private static final Pattern DEFINED_WITHOUT_BRACKETS_PATTERN = Pattern.compile("defined\\s+(\\w+)");
    
    private static final Pattern LINUX_IS_ENABLED_PATTERN = Pattern.compile("IS_ENABLED\\s*\\(\\s*(\\w+)\\s*\\)");
    private static final Pattern LINUX_IS_BUILTIN_PATTERN = Pattern.compile("IS_BUILTIN\\s*\\(\\s*(\\w+)\\s*\\)");
    private static final Pattern LINUX_IS_MODULE_PATTERN = Pattern.compile("IS_MODULE\\s*\\(\\s*(\\w+)\\s*\\)");
    
    private @NonNull LineNumberReader in;
    
    private @NonNull File sourceFile;
    
    private boolean doLinuxReplacements;
    
    private boolean fuzzyParsing;
    
    private net.ssehub.kernel_haven.util.logic.parser.@NonNull Parser<@NonNull Formula> conditionParser;
    
    /**
     * All blocks that are not nested inside other blocks.
     */
    private @NonNull List<@NonNull CodeBlock> topBlocks;
    
    /**
     * The current nesting hierarchy. May be empty.
     */
    private @NonNull Deque<@NonNull CodeBlock> nesting;
    
    /**
     * The condition of the previous #if or #elif sibling. Used to construct the negated conditions for
     * #elif and #else.
     */
    private @Nullable Formula previousCondition;
    
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
     * 
     * @param in The reader to get the input from. Internally, this will be wrapped into a {@link BufferedReader},
     *      so passing an unbuffered reader here is ok.
     * @param sourceFile The source file to specify in the {@link CodeBlock}s.
     */
    public BlockParser(@NonNull Reader in, @NonNull File sourceFile) {
        this(in, sourceFile, false, false);
    }
    
    /**
     * Creates a parser for the given input.
     * 
     * @param in The reader to get the input from. Internally, this will be wrapped into a {@link BufferedReader},
     *      so passing an unbuffered reader here is ok.
     * @param sourceFile The source file to specify in the {@link CodeBlock}s.
     * @param doLinuxReplacements Whether to replace preprocessor macros found in the Linux Kernel (i.e.
     *      IS_ENABLED, IS_BUILTIN, IS_MODULE).
     * @param fuzzyParsing Whether to do fuzzy parsing for non-boolean integer comparisons.
     */
    public BlockParser(@NonNull Reader in, @NonNull File sourceFile, boolean doLinuxReplacements,
            boolean fuzzyParsing) {
        
        this.in = new LineNumberReader(in);
        this.sourceFile = sourceFile;
        this.doLinuxReplacements = doLinuxReplacements;
        this.fuzzyParsing = fuzzyParsing;
        
        this.conditionParser = new net.ssehub.kernel_haven.util.logic.parser.Parser<>(
                new CppDefinedGrammar(new VariableCache()));
        
        this.topBlocks = new LinkedList<>();
        this.nesting = new LinkedList<>();
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
            line = line.trim();
            
            // line continuation
            if (line.startsWith("#")) {
                while (line.endsWith("\\")) {
                    // remove trailing \
                    line = line.substring(0, line.length() - 1);
                    
                    String next = in.readLine();
                    if (next != null) {
                        line += next;
                    }
                }
            }
            
            line = removeComments(line).trim();
            
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
        for (CodeBlock child : block.iterateNestedBlocks()) {
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
        Formula condition = parse(expression);
        previousCondition = condition;
        
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
        Formula previousCondition = this.previousCondition;
        if (previousCondition == null) {
            throw new FormatException("Found #elif in line " + currentLineNumber + " with on previous #if condition");
        }
        
        Formula condition = parse(expression);
        condition = new Conjunction(new Negation(previousCondition), condition);
        this.previousCondition = condition;
        
        finishBlock(); // finish the previous #if or #elif
        buildBlock(condition);
    }
    
    /**
     * Handles an #else line. Called by the main parsing loop if it is determined that the current line is an #else.
     * 
     * @throws FormatException If handling the #else fails.
     */
    private void handleElse() throws FormatException {
        Formula previousCondition = this.previousCondition;
        if (previousCondition == null) {
            throw new FormatException("Found #else in line " + currentLineNumber + " with on previous #if condition");
        }
        
        this.previousCondition = null; // no more #elifs or #else allowed after this
        
        Formula condition = new Negation(previousCondition);
        
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
        
        previousCondition = null;
        
        finishBlock();
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
                    if (chars[i] == '/' && chars[i - 1] == '*') {
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
        }
        
        return replaced;
    }
    
    /**
     * Parses the given CPP expression.
     * 
     * @param expression The expression to parse.
     * 
     * @return The parsed formula.
     * 
     * @throws FormatException If the expression cannot be parsed.
     */
    private @NonNull Formula parse(@NonNull String expression) throws FormatException {
        if (doLinuxReplacements) {
            expression = doLinuxReplacements(expression);
        }
        
        /*
         * Do replacements needed because of limitation of our grammar
         *   * defined (VAR) -> defined(VAR)     (no spaces allowed)
         *   * defined VAR -> defined(VAR)       (add brackets)
         */
        
        Matcher m = DEFINED_WITH_SPACE_PATTERN.matcher(expression);
        while (m.find()) {
            String var = m.group(1);
            expression = notNull(expression.replace(m.group(),
                    "defined(" + var + ")"));
            m = DEFINED_WITH_SPACE_PATTERN.matcher(expression);
        }
        
        m = DEFINED_WITHOUT_BRACKETS_PATTERN.matcher(expression);
        while (m.find()) {
            String var = m.group(1);
            expression = notNull(expression.replace(m.group(),
                    "defined(" + var + ")"));
            m = DEFINED_WITHOUT_BRACKETS_PATTERN.matcher(expression);
        }
        
        Formula result;
        try {
            result = conditionParser.parse(expression);
        } catch (ExpressionFormatException e) {
            if (!fuzzyParsing) {
                throw new FormatException(e);
            }
            
            // try fuzzy parsing
            try {
                result = conditionParser.parse(fuzzyfy(expression));
            } catch (ExpressionFormatException e1) {
                throw new FormatException(e1);
            }
        }
        
        return result;
    }

    /**
     * Do replacements for preprocesor macros needed for Linux.
     * 
     * @param expression The expression to do replacements for.
     * 
     * @return The expression with replcamenets done.
     */
    private @NonNull String doLinuxReplacements(@NonNull String expression) {
        /*
         * Do replacements needed for Linux:
         *   * IS_ENABLED(VAR) -> defined(VAR) || defined(VAR_MODULE)
         *   * IS_BUILTIN(VAR) -> defined(VAR)
         *   * IS_MODULE(VAR) -> defined(VAR_MODULE)
         */
        
        Matcher m = LINUX_IS_ENABLED_PATTERN.matcher(expression);
        while (m.find()) {
            String var = m.group(1);
            expression = notNull(expression.replace(m.group(),
                    "(defined(" + var + ") || defined(" + var + "_MODULE))"));
            m = LINUX_IS_ENABLED_PATTERN.matcher(expression);
        }
        
        m = LINUX_IS_BUILTIN_PATTERN.matcher(expression);
        while (m.find()) {
            String var = m.group(1);
            expression = notNull(expression.replace(m.group(),
                    "defined(" + var + ")"));
            m = LINUX_IS_BUILTIN_PATTERN.matcher(expression);
        }
        
        m = LINUX_IS_MODULE_PATTERN.matcher(expression);
        while (m.find()) {
            String var = m.group(1);
            expression = notNull(expression.replace(m.group(),
                    "defined(" + var + "_MODULE)"));
            m = LINUX_IS_MODULE_PATTERN.matcher(expression);
        }
        return expression;
    }
    
    /**
     * Replaces comparison operators with escaped versions. This can be used to "fuzzy parse" expressions that are
     * otherwise (pure boolean) unparseable. Also adds defined() calls around the newly added variables.
     * 
     * @param expression The expression to fuzzyfy.
     * @return A fuzzyfied expression.
     */
    private @NonNull String fuzzyfy(@NonNull String expression) {
        Pattern comparisonOperatorPattern = Pattern.compile("(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(\\w+)");
        
        Matcher m = comparisonOperatorPattern.matcher(expression);
        while (m.find()) {
            String newOp;
            switch (m.group(2)) {
            case "==":
                newOp = "_eq_";
                break;
            case "!=":
                newOp = "_ne_";
                break;
            case "<":
                newOp = "_lt_";
                break;
            case "<=":
                newOp = "_le_";
                break;
            case ">":
                newOp = "_gt_";
                break;
            case ">=":
                newOp = "_ge_";
                break;
            default:
                newOp = m.group(2);
                break;
            }
            
            expression = notNull(expression.replace(m.group(), "defined(" + m.group(1) + newOp + m.group(3) + ")"));
            
            m = comparisonOperatorPattern.matcher(expression);
        }
        
        return expression;
    }
    
    /**
     * Closes the input reader that was passed to this parser in the constructor.
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
    
}
