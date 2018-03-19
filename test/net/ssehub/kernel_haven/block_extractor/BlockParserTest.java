package net.ssehub.kernel_haven.block_extractor;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link BlockParser}.
 *
 * @author Adam
 */
public class BlockParserTest {

    /**
     * Tests a simple #if with a defined() call.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfWithSimpleVariable() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with another simple if nested inside.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfWithNestingIf() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + " #if defined(B)\n"
                + "     moreCode;\n"
                + " #endif\n"
                + " evenMoreCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();

        CodeBlock expected = new CodeBlock(1, 6, new File("test.c"), new Variable("A"), new Variable("A"));
        
        expected.addNestedElement(new CodeBlock(3, 4, new File("test.c"), new Variable("B"), and("A", "B")));
        
        assertThat(result, is(Arrays.asList(expected)));
        
        parser.close();
    }
    
    /**
     * Tests a multiple simple #ifs at the top level.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testMultipleIfAtTopLevel() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#endif\n"
                + "#if defined(B)\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(4, 5, new File("test.c"), new Variable("B"), new Variable("B"))
        )));
        
        parser.close();
    }
    
    /**
     * Tests a simple #ifdef.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfdef() throws IOException, FormatException {
        String code = "#ifdef A\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #ifndef.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfndef() throws IOException, FormatException {
        String code = "#ifndef A\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), not("A"), not("A")))));
        
        parser.close();
    }
    
    /**
     * Tests that too many #endifs throw an exception.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testTooManyEndifs() throws IOException, FormatException {
        String code = "#ifdef A\n"
                + " someCode;\n"
                + "#endif\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests that an #endif before the first #if throws an exception.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testEndifBedoreIf() throws IOException, FormatException {
        String code = "#endif\n" 
                + "#ifdef A\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests that a missing #endif throws an exception.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testMissingEndif() throws IOException, FormatException {
        String code = "#ifdef A\n"
                + " someCode;\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #if with a malformed expression.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIfWithMalformedExpression() throws IOException, FormatException {
        String code = "#if defined(A) ||\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests that comments are handled correctly.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testEverythingCommentedOutWithInline() throws IOException, FormatException {
        String code = "/*#if defined(A)\n"
                + " someCode; /\n"
                + "#endif*/\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList()));
        
        parser.close();
    }
    
    /**
     * Tests that comments are handled correctly.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testEverythingCommentedOutWithLinecomment() throws IOException, FormatException {
        String code = "//#if defined(A)\n"
                + "// someCode;\n"
                + "//#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList()));
        
        parser.close();
    }
    
    /**
     * Tests that comments are handled correctly.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testCommentsInCondition() throws IOException, FormatException {
        String code = "#if defined(A) /* && defined(B) */ || defined(C) // && defined(D) \n"
                + " / someCode; /\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), or("A", "C"), or("A", "C")))));
        
        parser.close();
    }
    
    /**
     * Tests that a pseudo block is added if there is a non-whitespace character is outside of all blocks. 
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testContentOutsideOfAllBlocks() throws IOException, FormatException {
        String code = "a;\n"
                + "#if defined(A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock expected = new CodeBlock(1, 5, new File("test.c"), True.INSTANCE, True.INSTANCE);
        
        expected.addNestedElement(new CodeBlock(2, 3, new File("test.c"), new Variable("A"),  new Variable("A")));
        
        assertThat(result, is(Arrays.asList(expected)));
        
        parser.close();
    }
    
    /**
     * Tests that a pseudo block is added if there is a non-whitespace character is outside of all blocks. 
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testContentOutsideOfAllBlocksWithMultipleBlocks() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#endif\n"
                + "something outside of all blocks;\n"
                + "#if defined(B)\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock expected = new CodeBlock(1, 8, new File("test.c"), True.INSTANCE, True.INSTANCE);
        
        expected.addNestedElement(new CodeBlock(1, 2, new File("test.c"), new Variable("A"),  new Variable("A")));
        expected.addNestedElement(new CodeBlock(5, 6, new File("test.c"), new Variable("B"),  new Variable("B")));
        
        assertThat(result, is(Arrays.asList(expected)));
        
        parser.close();
    }
    
    /**
     * Tests that no pseudo block is added if the content outside of the blocks is commented out.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testsCommentOutContentOutsideBlocks() throws IOException, FormatException {
        String code = " /* only a comment */ \t \n" 
                + "#if defined(A)\n"
                + " someCode;\n"
                + "#endif\n"
                + "  // some commented out text ";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(2, 3, new File("test.c"), new Variable("A"),  new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests that continuation works.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testContinuation() throws IOException, FormatException {
        String code = "#if defined(A) \\\n"
                + "     || defined(B)\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), or("A", "B"), or("A", "B")))));
        
        parser.close();
    }
    
    /**
     * Tests continuation two weird continuations.
     * <ul>
     *  <li>The first causes an #if to disappear and is commented out</li>
     *  <li>The second is at the end of the file</li>
     * </ul>
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testWeirdContinuation() throws IOException, FormatException {
        String code = "#include <something> // this cause the next line to not be an #if -> \\\n"
                + "#if defined(A) \\\n"
                + "     || defined(B)\n"
                + " someCode;\n"
                + "#error \\\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 6, new File("test.c"), True.INSTANCE, True.INSTANCE))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testElse() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#else\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 4, new File("test.c"), not("A"), not("A"))
        )));
        
        parser.close();
    }
    
    /**
     * Tests a simple #ifdef with an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfdefElse() throws IOException, FormatException {
        String code = "#ifdef A\n"
                + " someCode;\n"
                + "#else\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 4, new File("test.c"), not("A"), not("A"))
        )));
        
        parser.close();
    }
    
    /**
     * Tests an #if with an #else and a nested #if before the #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testElseWithNestingBefore() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " #if defined(B)\n"
                + "     someCode;\n"
                + " #endif\n"
                + "#else\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock firstExpected = new CodeBlock(1, 4, new File("test.c"), new Variable("A"), new Variable("A"));
        
        firstExpected.addNestedElement(new CodeBlock(2, 3, new File("test.c"), new Variable("B"), and("A", "B")));
        
        assertThat(result, is(Arrays.asList(firstExpected,
                new CodeBlock(5, 6, new File("test.c"), not("A"), not("A")))));
        
        parser.close();
    }
    
    /**
     * Tests an #if with an #elif and a nested #if before the #elif.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testElifWithNestingBefore() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " #if defined(B)\n"
                + "     someCode;\n"
                + " #endif\n"
                + "#elif defined(C)\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock firstExpected = new CodeBlock(1, 4, new File("test.c"), new Variable("A"), new Variable("A"));
        
        firstExpected.addNestedElement(new CodeBlock(2, 3, new File("test.c"), new Variable("B"), and("A", "B")));
        
        assertThat(result, is(Arrays.asList(firstExpected,
                new CodeBlock(5, 6, new File("test.c"), and(not("A"), "C"), and(not("A"), "C")))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with an #elif.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testElif() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#elif defined(B)\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 4, new File("test.c"), and(not("A"), "B"), and(not("A"), "B"))
        )));
        
        parser.close();
    }
    
    /**
     * Tests an #elif with an invalid condition.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElifInvalidCondition() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#elif defined(B) || \n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests a simple #if with two #elifs and an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testElifElifElse() throws IOException, FormatException {
        String code = "#if defined(A)\n"
                + " someCode;\n"
                + "#elif defined(B)\n"
                + " someElseCode;\n"
                + "#elif defined(C)\n"
                + " someElseCode;\n"
                + "#else\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula firstElif = and(not("A"), "B");
        Formula secondElif = and(and(not("A"), not("B")), "C");
        Formula elseCond = and(and(not("A"), not("B")), not("C"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 4, new File("test.c"), firstElif, firstElif),
                new CodeBlock(5, 6, new File("test.c"), secondElif, secondElif),
                new CodeBlock(7, 8, new File("test.c"), elseCond, elseCond)
        )));
        
        parser.close();
    }
    
    /**
     * Tests an #else without #if.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElseWithoutIf() throws IOException, FormatException {
        String code = "#else\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #elif without #if.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElifWithoutIf() throws IOException, FormatException {
        String code = "#elif defined(B)\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #elif after the #endif.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElifAfterEndif() throws IOException, FormatException {
        String code = "#if defined(A)\n" 
                + " someElseCode;\n"
                + "#endif\n"
                + "#elif defined(B)\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #else after the #endif.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElseAfterEndif() throws IOException, FormatException {
        String code = "#if defined(A)\n" 
                + " someElseCode;\n"
                + "#endif\n"
                + "#else\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #else after the #endif after an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElseAfterEndifWithElse() throws IOException, FormatException {
        String code = "#if defined(A)\n" 
                + " someElseCode;\n"
                + "#else\n"
                + " someElseCode;\n"
                + "#endif\n"
                + "#else\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #elif after an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElifAfterElse() throws IOException, FormatException {
        String code = "#if defined(A)\n" 
                + " someCode;\n"
                + "#else\n"
                + " someCode;\n"
                + "#elif defined(B)\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #else after an #else.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testElseAfterElse() throws IOException, FormatException {
        String code = "#if defined(A)\n" 
                + " someCode;\n"
                + "#else\n"
                + " someCode;\n"
                + "#else\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests a simple #if with condition true.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIfConditionMissingDefined() throws IOException, FormatException {
        String code = "#if A\n"
                + " someCode;\n"
                + " moreCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests a simple #if with condition true.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIfdefConditionDoubleDefine() throws IOException, FormatException {
        String code = "#ifdef defined(A)\n"
                + " someCode;\n"
                + " moreCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests an #if with no following space.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfWithoutSpace() throws IOException, FormatException {
        String code = "#if(defined(A))\n"
                + " someCode;\n"
                + "#endif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests that comments are handled correctly. This was a test case that used to crash the parser.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testInlineCommentLineStartingWithSlash() throws IOException, FormatException {
        String code = "/*\n"
                + "/\n"
                + "*/\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList()));
        
        parser.close();
    }
    
    /**
     * Tests whether preprocessor directives with whitespace characters between the hash and the directive are detected.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testSpaceAfterHash() throws IOException, FormatException {
        String code = "# if defined(A)\n"
                + " someCode;\n"
                + "#\tendif\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests that a hash followed by only whitespaces doesn't crash.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testEmptyPreprocessorHash() throws IOException, FormatException {
        String code = " # \n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 2, new File("test.c"), True.INSTANCE, True.INSTANCE))));
        
        parser.close();
    }
    
    /**
     * Tests that an empty line is handled correclty.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testEmptyLine() throws IOException, FormatException {
        String code = "something\n"
                + "\n"
                + "something\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 4, new File("test.c"), True.INSTANCE, True.INSTANCE))));
        
        parser.close();
    }
    
    /**
     * Tests that a (multi line) commented out line without a slash is also removed by removeComments().
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testCommentedOutLineWithoutSlash() throws IOException, FormatException {
        String code = "/*\n"
                + " * This comment line has on slash\n"
                + " */\n";
        
        BlockParser parser = new BlockParser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList()));
        
        parser.close();
    }
    
}
