package net.ssehub.kernel_haven.block_extractor;

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
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link Parser}.
 *
 * @author Adam
 */
public class ParserTest {

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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with a more complex condition.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testIfWithComplexCondition() throws IOException, FormatException {
        String code = "#if (defined(A) && (!defined(B) || defined(C)))\n"
                + " someCode;\n"
                + " moreCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Conjunction(new Variable("A"), new Disjunction(new Negation(new Variable("B")),
                new Variable("C")));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 4, new File("test.c"), condition, condition))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();

        CodeBlock expected = new CodeBlock(1, 7, new File("test.c"), new Variable("A"), new Variable("A"));
        
        expected.addNestedElement(new CodeBlock(3, 5, new File("test.c"), new Variable("B"), 
                new Conjunction(new Variable("A"), new Variable("B"))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(4, 6, new File("test.c"), new Variable("B"), new Variable("B"))
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Negation(new Variable("A"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Disjunction(new Variable("A"), new Variable("C"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock expected = new CodeBlock(1, 5, new File("test.c"), True.INSTANCE, True.INSTANCE);
        
        expected.addNestedElement(new CodeBlock(2, 4, new File("test.c"), new Variable("A"),  new Variable("A")));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        CodeBlock expected = new CodeBlock(1, 8, new File("test.c"), True.INSTANCE, True.INSTANCE);
        
        expected.addNestedElement(new CodeBlock(1, 3, new File("test.c"), new Variable("A"),  new Variable("A")));
        expected.addNestedElement(new CodeBlock(5, 7, new File("test.c"), new Variable("B"),  new Variable("B")));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(2, 4, new File("test.c"), new Variable("A"),  new Variable("A")))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Disjunction(new Variable("A"), new Variable("B"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 4, new File("test.c"), condition, condition))));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 6, new File("test.c"), True.INSTANCE, True.INSTANCE))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with and #else.
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula notA = new Negation(new Variable("A"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 5, new File("test.c"), notA, notA)
        )));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with and #elif.
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula notAandB = new Conjunction(new Negation(new Variable("A")), new Variable("B"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 5, new File("test.c"), notAandB, notAandB)
        )));
        
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula firstElif = new Conjunction(new Negation(new Variable("A")), new Variable("B"));
        Formula secondElif = new Conjunction(new Negation(firstElif), new Variable("C"));
        Formula elseCond = new Negation(secondElif);
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")),
                new CodeBlock(3, 5, new File("test.c"), firstElif, firstElif),
                new CodeBlock(5, 7, new File("test.c"), secondElif, secondElif),
                new CodeBlock(7, 9, new File("test.c"), elseCond, elseCond)
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
        
        Parser parser = new Parser(
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
        String code = "#elif\n"
                + " someElseCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        parser.readBlocks();
        parser.close();
    }
    
    /**
     * Tests a simple #if with condition true.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testConditionLiteralTrue() throws IOException, FormatException {
        String code = "#if 1\n"
                + " someCode;\n"
                + " moreCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = True.INSTANCE;
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 4, new File("test.c"), condition, condition))));
        
        parser.close();
    }
    
    /**
     * Tests a simple #if with condition true.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testConditionLiteralFalse() throws IOException, FormatException {
        String code = "#if 0\n"
                + " someCode;\n"
                + " moreCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = False.INSTANCE;
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 4, new File("test.c"), condition, condition))));
        
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
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
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests an defined (VAR) with a space before the bracket.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testDefinedWithSpace() throws IOException, FormatException {
        String code = "#if defined (A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests an defined VAR without the brackets.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testDefinedWithoutBrackets() throws IOException, FormatException {
        String code = "#if defined A\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"));
        
        List<CodeBlock> result = parser.readBlocks();
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), new Variable("A"), new Variable("A")))));
        
        parser.close();
    }
    
    /**
     * Tests the Linux macro handling for IS_ENABLED(VAR).
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testLinuxReplacementIsEnabled() throws IOException, FormatException {
        String code = "#if IS_ENABLED(A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"), true, false);
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Disjunction(new Variable("A"), new Variable("A_MODULE"));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
        parser.close();
    }
    
    /**
     * Tests the Linux macro handling for IS_BUILTIN(VAR).
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testLinuxReplacementIsBuiltin() throws IOException, FormatException {
        String code = "#if IS_BUILTIN(A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"), true, false);
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Variable("A");
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
        parser.close();
    }
    
    /**
     * Tests the Linux macro handling for IS_MODULE(VAR).
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testLinuxReplacementIsModule() throws IOException, FormatException {
        String code = "#if IS_MODULE(A)\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"), true, false);
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Variable("A_MODULE");
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
        parser.close();
    }
    
    /**
     * Tests fuzzy parsing.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testFuzzyParsing() throws IOException, FormatException {
        String code = "#if (A==2) || (B > 54) || (C >= 3) || (D!=2) || (E<2) || (F <= 5)\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"), false, true);
        
        List<CodeBlock> result = parser.readBlocks();
        
        Formula condition = new Disjunction(new Variable("A_eq_2"),
                new Disjunction(new Variable("B_gt_54"), 
                        new Disjunction(new Variable("C_ge_3"), 
                                new Disjunction(new Variable("D_ne_2"), 
                                        new Disjunction(new Variable("E_lt_2"), new Variable("F_le_5"))))));
        
        assertThat(result, is(Arrays.asList(
                new CodeBlock(1, 3, new File("test.c"), condition, condition))));
        
        parser.close();
    }
    
    /**
     * Tests a case where fuzzy parsing still fails.
     * 
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testFuzzyParsingStillFails() throws IOException, FormatException {
        String code = "#if A + 1 > 5\n"
                + " someCode;\n"
                + "#endif\n";
        
        Parser parser = new Parser(
                new InputStreamReader(new ByteArrayInputStream(code.getBytes())), new File("test.c"), false, true);
        
        parser.readBlocks();
        parser.close();
    }
    
}
