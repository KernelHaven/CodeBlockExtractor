package net.ssehub.kernel_haven.block_extractor;

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
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
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
        
        SourceFile result = extractor.runOnFile(new File("simpleIf.c"));
        
        assertThat(result.getPath(), is(new File("simpleIf.c")));
        assertThat(result.getTopElementCount(), is(1));
        
        assertThat(result.getElement(0), is(
                new CodeBlock(2, 3, new File("simpleIf.c"), new Variable("A"), new Variable("A"))));
    }
    
    /**
     * Tests running the extractor on one Linux file.
     * 
     * @throws ExtractorException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testLinuxFile() throws ExtractorException, SetUpException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        config.setValue(DefaultSettings.ARCH, "x86"); // this means we analyze Linux
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        SourceFile result = extractor.runOnFile(new File("linux.c"));
        
        assertThat(result.getPath(), is(new File("linux.c")));
        assertThat(result.getTopElementCount(), is(1));
        
        Formula condition = new Disjunction(new Variable("A"), new Variable("A_MODULE"));
        
        assertThat(result.getElement(0), is(
                new CodeBlock(2, 3, new File("linux.c"), condition, condition)));
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
