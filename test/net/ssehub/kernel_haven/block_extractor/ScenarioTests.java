package net.ssehub.kernel_haven.block_extractor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.CodeModelCache;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.FormatException;

/**
 * "Scenario" tests for the {@link CodeBlockExtractor} with "real-world" C files.
 * Sadly we can't use Linux' source files here, because they are GPL.
 *
 * @author Adam
 */
@SuppressWarnings("null")
public class ScenarioTests {
    

    private static final File TESTDATA = new File("testdata/scenario");
    
    /**
     * First scenario test.
     * 
     * @throws ExtractorException unwanted.
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void test1() throws ExtractorException, SetUpException, IOException, FormatException {
        Configuration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.SOURCE_TREE, TESTDATA);
        
        CodeBlockExtractor extractor = new CodeBlockExtractor();
        extractor.init(config);
        
        SourceFile result = extractor.runOnFile(new File("test1.c"));
        
        // compare with cache that was manually verified
        CodeModelCache cache = new CodeModelCache(TESTDATA);
        SourceFile expected = cache.read(new File("test1.c"));
        
        assertThat(result.getPath(), is(expected.getPath()));
        for (int i = 0; i < expected.getTopElementCount(); i++) {
            assertThat(result.getElement(i), is(expected.getElement(i)));
        }
        assertThat(result.getTopElementCount(), is(expected.getTopElementCount()));
        
//        cache.write(result); // this was used to create the valid cache to verify against
    }

}
