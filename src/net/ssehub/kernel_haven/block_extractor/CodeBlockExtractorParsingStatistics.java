/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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

import java.util.ArrayList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AbstractAnalysis;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An "analysis" that collects statistics for the parsing of of the {@link CodeBlockExtractor}.
 *
 * @author Adam
 */
public class CodeBlockExtractorParsingStatistics extends AbstractAnalysis {

    /**
     * Creates this analysis.
     * 
     * @param config The pipeline configuration.
     */
    public CodeBlockExtractorParsingStatistics(@NonNull Configuration config) {
        super(config);
    }

    @Override
    public void run() {
        List<@NonNull SourceFile<CodeBlock>> files = new ArrayList<>(200000);
        
        long t0 = System.currentTimeMillis();
        try {
            cmProvider.start();
        } catch (SetUpException e) {
            LOGGER.logException("Can't start CM extractor", e);
        }
        
        SourceFile<?> file;
        while ((file = cmProvider.getNextResult()) != null) {
            files.add(file.castTo(CodeBlock.class));
        }
        
        long t1 = System.currentTimeMillis();

        
        // anaylze results
        
        try {
            config.registerSetting(CodeBlockExtractor.INVALID_CONDITION_SETTING);
            if (config.getValue(CodeBlockExtractor.INVALID_CONDITION_SETTING)
                    != InvalidConditionHandling.ERROR_VARIABLE) {
                LOGGER.logWarning("Can't find number of unparseable conditions when "
                        + CodeBlockExtractor.INVALID_CONDITION_SETTING.getKey() + " is not set to "
                        + InvalidConditionHandling.ERROR_VARIABLE.name());
            }
        } catch (SetUpException e) {
            LOGGER.logException("Can't read setting " + CodeBlockExtractor.INVALID_CONDITION_SETTING.getKey(), e);
        }
        
        ErrorVariableCounter counter = new ErrorVariableCounter();
        
        for (SourceFile<CodeBlock> f : files) {
            for (CodeBlock b : f) {
                countInBlok(b, counter);
            }
        }
        
        int numExceptions = 0;
        while (cmProvider.getNextException() != null) {
            numExceptions++;
        }
        
        LOGGER.logInfo("CodeBlockExtractor parsing statistics:",
                "\tRuntime: " + Util.formatDurationMs(t1 - t0),
                "\tNumber of files: " + files.size(),
                "\tNumber of exceptions (unparseable files): " + numExceptions,
                "\tNumber of conditions: " + counter.numConditions,
                "\tNumber of error variables in conditions (unparseable conditions): " + counter.count
        );
    }
    
    /**
     * Runs the given counter on the immediate conditions of the given block and all children.
     * 
     * @param block The block to run on.
     * @param counter The counter to run.
     */
    private static void countInBlok(@NonNull CodeBlock block, @NonNull ErrorVariableCounter counter) {
        if (block.getCondition() != null) {
            counter.numConditions++;
            notNull(block.getCondition()).accept(counter);
        }
        
        for (CodeBlock child : block) {
            countInBlok(child, counter);
        }
    }
    
    /**
     * A visitor that counts the number of {@link CppConditionParser#ERROR_VARIBLE} occurrences.
     */
    private static class ErrorVariableCounter implements IVoidFormulaVisitor {

        private int count;
        
        private int numConditions = 0;
        
        @Override
        public void visitFalse(@NonNull False falseConstant) {
            // do nothing
        }

        @Override
        public void visitTrue(@NonNull True trueConstant) {
            // do nothing
        }

        @Override
        public void visitVariable(@NonNull Variable variable) {
            if (variable.equals(CppConditionParser.ERROR_VARIBLE)) {
                count++;
            }
        }

        @Override
        public void visitNegation(@NonNull Negation formula) {
            formula.getFormula().accept(this);
        }

        @Override
        public void visitDisjunction(@NonNull Disjunction formula) {
            formula.getLeft().accept(this);
            formula.getRight().accept(this);
        }

        @Override
        public void visitConjunction(@NonNull Conjunction formula) {
            formula.getLeft().accept(this);
            formula.getRight().accept(this);
        }
        
    }

}
