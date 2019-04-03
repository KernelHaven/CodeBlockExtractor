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

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Test for the complete {@link CodeBlockExtractor}.
 *
 * @author Adam
 */
@SuppressWarnings("null")
public class CodeBlockExtractorTest {

    private static final File TESTDATA = new File("testdata");
    
    /**
     * Tests running the extractor on one simple file.
     * 
     * @throws ExtractorException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimpleFile() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        SourceFile<CodeBlock> result = extractor.runOnFile(new File("simpleIf.c"));
        
        assertThat(result.getPath(), is(new File("simpleIf.c")));
        assertThat(result.getTopElementCount(), is(1));
        
        assertThat(result.getElement(0), is(
                new CodeBlock(2, 3, new File("simpleIf.c"), new Variable("A"), new Variable("A"))));
    }
    
    /**
     * Tests running the extractor on a file using a Linux macro.
     * 
     * @throws ExtractorException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testLinuxMacro() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        config.registerSetting(CodeBlockExtractor.HANDLE_LINUX_MACROS);
        config.setValue(CodeBlockExtractor.HANDLE_LINUX_MACROS, true);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        SourceFile<CodeBlock> result = extractor.runOnFile(new File("linux_macro.c"));
        
        assertThat(result.getPath(), is(new File("linux_macro.c")));
        assertThat(result.getTopElementCount(), is(1));
        
        assertThat(result.getElement(0), is(
                new CodeBlock(2, 3, new File("linux_macro.c"), or("A", "A_MODULE"), or("A", "A_MODULE"))));
    }
    
    /**
     * Tests running the extractor on a file with an invalid condition and replacement enabled.
     * 
     * @throws ExtractorException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testInvalidConditionReplacement() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        config.registerSetting(CodeBlockExtractor.INVALID_CONDITION_SETTING);
        config.setValue(CodeBlockExtractor.INVALID_CONDITION_SETTING, InvalidConditionHandling.ERROR_VARIABLE);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        SourceFile<CodeBlock> result = extractor.runOnFile(new File("invalid_condition.c"));
        
        assertThat(result.getPath(), is(new File("invalid_condition.c")));
        assertThat(result.getTopElementCount(), is(1));
        
        assertThat(result.getElement(0), is(
                new CodeBlock(2, 3, new File("invalid_condition.c"),
                    new Variable("PARSING_ERROR"), new Variable("PARSING_ERROR"))));
    }
    
    /**
     * Tests running the extractor on a not existing file.
     * 
     * @throws ExtractorException wanted.
     * @throws SetUpException unwanted.
     */
    @Test(expected = CodeExtractorException.class)
    public void testNotExistingFile() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        extractor.runOnFile(new File("doesnt_exist.c"));
    }
    
    /**
     * Tests running the extractor on a malformed file.
     * 
     * @throws ExtractorException wanted.
     * @throws SetUpException unwanted.
     */
    @Test(expected = CodeExtractorException.class)
    public void testWrongFormat() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        extractor.runOnFile(new File("invalid.c"));
    }
    
    /**
     * Silly test, but we need it for 100% coverage.
     */
    @Test
    public void testName() {
        assertThat(new CodeBlockExtractor().getName(), is("CodeBlockExtractor"));
    }
    
}
