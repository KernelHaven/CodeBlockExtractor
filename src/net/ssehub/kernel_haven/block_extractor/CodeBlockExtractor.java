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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.AbstractCodeModelExtractor;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A code-model extractor that extracts #ifdef blocks from C source files. Creates {@link SourceFile}s with
 * {@link CodeBlock}s.
 *
 * @author Adam
 */
public class CodeBlockExtractor extends AbstractCodeModelExtractor {
    
    public static final @NonNull EnumSetting<@NonNull InvalidConditionHandling> INVALID_CONDITION_SETTING
        = new EnumSetting<>("code.extractor.invalid_condition", InvalidConditionHandling.class, true,
                InvalidConditionHandling.EXCEPTION, "How to handle conditions of blocks that are invalid or not "
                        + "parseable.\n\n- EXCEPTION: Throw an exception. This causes the whole file to not be "
                        + "parseable.\n- TRUE: Replace the invalid condition with true.\n- ERROR_VARIABLE: Replace "
                        + "the invalid condition with a variable called \"PARSING_ERROR\"");

    public static final @NonNull Setting<@NonNull Boolean> HANDLE_LINUX_MACROS = new Setting<>(
        "code.extractor.handle_linux_macros", Type.BOOLEAN, true, "false", "Whether to handle the preprocessor macros "
                + "IS_ENABLED, IS_BUILTIN and IS_MODULE in preprocessor block conditions.");
    
    private File sourceTree;
    
    private boolean handleLinuxMacros;
    
    private boolean fuzzyParsing;
    
    private InvalidConditionHandling invalidConditionHandling;
    
    @Override
    protected void init(@NonNull Configuration config) throws SetUpException {
        config.registerSetting(INVALID_CONDITION_SETTING);
        config.registerSetting(HANDLE_LINUX_MACROS);
        
        this.sourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
        this.fuzzyParsing = config.getValue(DefaultSettings.FUZZY_PARSING);
        this.handleLinuxMacros = config.getValue(HANDLE_LINUX_MACROS);
        this.invalidConditionHandling = config.getValue(INVALID_CONDITION_SETTING);
    }

    @Override
    protected @Nullable SourceFile<CodeBlock> runOnFile(@NonNull File target) throws ExtractorException {
        File absoulteTarget = new File(sourceTree, target.getPath());
        
        SourceFile<CodeBlock> result = new SourceFile<>(target);
        
        try (BlockParser parser = new BlockParser(new FileReader(absoulteTarget), target,
                handleLinuxMacros, fuzzyParsing, notNull(invalidConditionHandling))) {
            
            for (CodeBlock block : parser.readBlocks()) {
                result.addElement(block);
            }
            
        } catch (IOException e) {
            throw (CodeExtractorException)
                new CodeExtractorException(target, "Can't read " + absoulteTarget).initCause(e);
        } catch (FormatException e) {
            throw new CodeExtractorException(target, e);
        }
        
        return result;
    }

    @Override
    protected @NonNull String getName() {
        return "CodeBlockExtractor";
    }

}
