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
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.cpp_utils.CppParsingSettings;
import net.ssehub.kernel_haven.cpp_utils.InvalidConditionHandling;
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
    
    public static final @NonNull Setting<@NonNull Boolean> ADD_PSEUDO_BLOCK = new Setting<>(
            "code.extractor.add_pseudo_block", Type.BOOLEAN, true, "true", "If code is found outside of all #ifdef "
                    + "blocks, this setting specifies whether to add a pseudo block for the whole file. This block "
                    + "starts at line 1, ends at the last line of the file and has the condition 'true'.");
    
    private File sourceTree;
    
    private boolean handleLinuxMacros;
    
    private boolean fuzzyParsing;
    
    private InvalidConditionHandling invalidConditionHandling;
    
    private boolean addPseudoBlock;
    
    @Override
    protected void init(@NonNull Configuration config) throws SetUpException {
        config.registerSetting(CppParsingSettings.INVALID_CONDITION_SETTING);
        config.registerSetting(CppParsingSettings.HANDLE_LINUX_MACROS);
        config.registerSetting(ADD_PSEUDO_BLOCK);
        
        this.sourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
        this.fuzzyParsing = config.getValue(DefaultSettings.FUZZY_PARSING);
        this.handleLinuxMacros = config.getValue(CppParsingSettings.HANDLE_LINUX_MACROS);
        this.invalidConditionHandling = config.getValue(CppParsingSettings.INVALID_CONDITION_SETTING);
        this.addPseudoBlock = config.getValue(ADD_PSEUDO_BLOCK);
    }

    @Override
    protected @Nullable SourceFile<CodeBlock> runOnFile(@NonNull File target) throws ExtractorException {
        File absoulteTarget = new File(sourceTree, target.getPath());
        
        SourceFile<CodeBlock> result = new SourceFile<>(target);
        
        try (BlockParser parser = new BlockParser(new FileReader(absoulteTarget), target,
                handleLinuxMacros, fuzzyParsing, notNull(invalidConditionHandling))) {
            parser.setAddPseudoBlock(addPseudoBlock);
            
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
