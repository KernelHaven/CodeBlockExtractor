package net.ssehub.kernel_haven.block_extractor;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * All tests for this project.
 *
 * @author Adam
 */
@RunWith(Suite.class)
@SuiteClasses({
    CodeBlockExtractorTest.class,
    BlockParserTest.class,
    CppConditionParserTest.class,
    ScenarioTests.class,
    })
public class AllTests {

}
