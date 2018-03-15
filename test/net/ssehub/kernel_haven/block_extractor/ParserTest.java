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
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
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
        String code = "#if defined(A) && (!defined(B) || defined(C))\n"
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
    
}
