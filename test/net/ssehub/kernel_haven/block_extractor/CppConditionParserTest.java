package net.ssehub.kernel_haven.block_extractor;

import static net.ssehub.kernel_haven.block_extractor.InvalidConditionHandling.EXCEPTION;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link CppConditionParser}.
 *
 * @author Adam
 */
public class CppConditionParserTest {
    
    /**
     * Tests a more complex condition that uses all boolean operators.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testComplexCondition() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);
        
        Formula condition = new Conjunction(new Variable("A"), new Disjunction(new Negation(new Variable("B")),
                new Variable("C")));
        
        assertThat(parser.parse("(defined(A) && (!defined(B) || defined(C)))"), is(condition));
    }
    
    /**
     * Tests parsing a literal true.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testConditionLiteralTrue() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);
        
        assertThat(parser.parse("1"), is(True.INSTANCE));
        assertThat(parser.parse("2"), is(True.INSTANCE));
        assertThat(parser.parse("-2"), is(True.INSTANCE));
    }
    
    /**
     * Tests parsing a literal false.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testConditionLiteralFalse() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);
        
        assertThat(parser.parse("0"), is(False.INSTANCE));
        assertThat(parser.parse("-0"), is(False.INSTANCE));
    }

    /**
     * Tests an defined (VAR) with a space before the bracket.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testDefinedWithSpace() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);
        
        assertThat(parser.parse("defined (A)"), is(new Variable("A")));
    }
    
    /**
     * Tests an defined VAR without the brackets.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testDefinedWithoutBrackets() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);
        
        assertThat(parser.parse("defined A"), is(new Variable("A")));
    }
    
    /**
     * Tests the Linux macro handling.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testLinuxMacros() throws FormatException {
        CppConditionParser parser = new CppConditionParser(true, false, EXCEPTION);

        Formula condition = new Disjunction(new Variable("A"), new Variable("A_MODULE"));
        assertThat(parser.parse("IS_ENABLED(A)"), is(condition));
        
        assertThat(parser.parse("IS_BUILTIN(A)"), is(new Variable("A")));
        assertThat(parser.parse("IS_MODULE(A)"), is(new Variable("A_MODULE")));
    }
    

    /**
     * Tests that IS_ENABLED() throws an exception if Linux handling is disabled.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIsEnabledWithoutLinuxEnabled() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("IS_ENABLED(A)");
    }
    
    /**
     * Tests that IS_BUILTIN() throws an exception if Linux handling is disabled.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIsBuiltinWithoutLinuxEnabled() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("IS_BUILTIN(A)");
    }
    
    /**
     * Tests that IS_MODULE() throws an exception if Linux handling is disabled.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testIsModuleWithoutLinuxEnabled() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("IS_MODULE(A)");
    }
    
    /**
     * Tests that an unknown function throws an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testUnknownFunctionWithLinuxEnabled() throws FormatException {
        CppConditionParser parser = new CppConditionParser(true, false, EXCEPTION);

        parser.parse("func(A)");
    }
    
    /**
     * Tests that an unknown function throws an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testUnknownFunction() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("func(A)");
    }
    
    /**
     * Tests fuzzy parsing with variables and literals.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testFuzzyParsingVarAndLiteral() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);
        
        assertThat(parser.parse("A == 2"), is(new Variable("A_eq_2")));
        assertThat(parser.parse("A != 2"), is(new Variable("A_ne_2")));
        assertThat(parser.parse("A >= 2"), is(new Variable("A_ge_2")));
        assertThat(parser.parse("A > 2"), is(new Variable("A_gt_2")));
        assertThat(parser.parse("A < 2"), is(new Variable("A_lt_2")));
        assertThat(parser.parse("A <= 2"), is(new Variable("A_le_2")));
    }
    
    /**
     * Tests fuzzy parsing with literal on the left.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testFuzzyParsingVarAndLiteralReversed() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);
        
        assertThat(parser.parse("2 == A"), is(new Variable("A_eq_2")));
        assertThat(parser.parse("2 != A"), is(new Variable("A_ne_2")));
        assertThat(parser.parse("2 <= A"), is(new Variable("A_ge_2")));
        assertThat(parser.parse("2 < A"), is(new Variable("A_gt_2")));
        assertThat(parser.parse("2 > A"), is(new Variable("A_lt_2")));
        assertThat(parser.parse("2 >= A"), is(new Variable("A_le_2")));
    }
    
    /**
     * Tests fuzzy parsing with two variabels.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testFuzzyParsingVarAndVar() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);
        
        assertThat(parser.parse("A == B"), is(new Variable("A_eq_B")));
        assertThat(parser.parse("A != B"), is(new Variable("A_ne_B")));
        assertThat(parser.parse("A >= B"), is(new Variable("A_ge_B")));
        assertThat(parser.parse("A > B"), is(new Variable("A_gt_B")));
        assertThat(parser.parse("A < B"), is(new Variable("A_lt_B")));
        assertThat(parser.parse("A <= B"), is(new Variable("A_le_B")));
    }
    
    /**
     * Tests a case where fuzzy parsing still fails.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testFuzzyParsingWithNonVarOnLeft() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);

        parser.parse("(A + 1) > 5");
    }
    
    /**
     * Tests a case where fuzzy parsing still fails.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testFuzzyParsingWithNonVarOnRight() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);

        parser.parse("5 > (A + 1)");
    }
    
    /**
     * Tests a case where fuzzy parsing still fails.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testFuzzyParsingWithOneNonVar() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);

        parser.parse("B > (A + 1)");
    }
    
    /**
     * Tests that unsupported operators throw an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testUnsupportedOperators() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("A ^ 1");
    }
    
    /**
     * Tests that an unary - throws an exception if it is not applied to an integer literal.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testUnsupportedUnarySub() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("-A");
    }
    
    /**
     * Tests that a variable without a defined throws an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testVariableWithoutDefined() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("A");
    }
    
    /**
     * Tests that a variable without a defined is translated correctly with fuzzy parsing.
     * 
     * @throws FormatException unwanted..
     */
    @Test
    public void testVariableWithoutDefinedFuzzyParsing() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, true, EXCEPTION);

        assertThat(parser.parse("A"), is(new Variable("A_ne_0")));
    }
    
    /**
     * Tests that a defined() call without a parameter throws an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testDefinedWithoutArgument() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("defined()");
    }
    
    /**
     * Tests that a defined() call on a literal throws an exception.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testDefinedOnLiteral() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("defined(1)");
    }
    
    /**
     * Tests that a comparator operator throws an exception if fuzzy parsing is disabled.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testComparatorWithoutFuzzyParsing() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("A == 2");
    }
    
    /**
     * Tests that an invalid expression throws an exception if {@link InvalidConditionHandling#EXCEPTION} is used.
     * 
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testMalformedException() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, EXCEPTION);

        parser.parse("defined(A) || ");
    }
    
    /**
     * Tests that an invalid expression is replaced by True if {@link InvalidConditionHandling#TRUE} is used.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testMalformedTrue() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, InvalidConditionHandling.TRUE);

        assertThat(parser.parse("defined(A) || "), is(True.INSTANCE));
    }
    
    /**
     * Tests that an invalid expression is replaced by an error variable if {@link InvalidConditionHandling#TRUE} is
     * used.
     * 
     * @throws FormatException unwanted.
     */
    @Test
    public void testMalformedVariable() throws FormatException {
        CppConditionParser parser = new CppConditionParser(false, false, InvalidConditionHandling.ERROR_VARIABLE);

        assertThat(parser.parse("defined(A) || "), is(new Variable("PARSING_ERROR")));
    }
    
}
