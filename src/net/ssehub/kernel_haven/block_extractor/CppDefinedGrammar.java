package net.ssehub.kernel_haven.block_extractor;

import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.logic.parser.CStyleBooleanGrammar;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.logic.parser.Grammar;
import net.ssehub.kernel_haven.util.logic.parser.VariableCache;

/**
 * A {@link Grammar} for C preprocessor conditions using defined().
 * 
 * <p>
 * Examples:
 * <ul>
 *      <li><code>defined(CONFIG_X86_64)</code></li>
 *      <li><code>(defined(CONFIG_X86_PAE) && !defined(CONFIG_X86_64) && (defined(CONFIG_X86_64)
 *              || defined(CONFIG_X86_PAE)))</code></li>
 * </ul>
 * </p>
 * 
 * @author Adam
 */
public class CppDefinedGrammar extends CStyleBooleanGrammar {
    // TODO this class was copied from srcML
    
    // CHECKSTYLE:OFF
    // see CStyleBooleanGrammar for explanation why we disable checkstyle
    
    /**
     * Creates this grammar with the given variable cache. The cache is used
     * to create every single {@link Variable}, to ensure that no two different
     * {@link Variable} objects with the same variable name exist.
     * 
     * @param cache The cache to use, or <code>null</code>.
     */
    public CppDefinedGrammar(VariableCache cache) {
        super(cache);
    }

    /**
     * Checks whether <code>i</code> points to a sub-string equal to <code>compareTo</code> in <code>str</code>.
     * 
     * @param str The string, that may contain the sub-string <code>compareTo</code>.
     * @param i The pointer to the position in <code>str</code> to check.
     * @param compareTo The sub-string, that may be present in <code>str</code>.
     * @return <code>true</code> if <code>i</code> points to a sub-string equal to
     *      <code>compareTo</code> in <code>str</code>; <code>false</code> otherwise.
     */
    private static boolean isSubstringEqual(char[] str, int i, String compareTo) {
        if (i < 0) {
            return false;
        }
        
        for (int j = i; j - i < compareTo.length(); j++) {
            if (str[j] != compareTo.charAt(j - i)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean isOpeningBracketChar(char[] str, int i) {
        if (str[i] != '(') {
            return false;
        }
        
        // check that this is not the bracket of a defined()
        if (i >= "defined".length()) {
            if (isSubstringEqual(str, i - "defined".length(), "defined")) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean isClosingBracketChar(char[] str, int i) {
        if (str[i] != ')') {
            return false;
        }
        
        // check that this is not the bracket of a defined()
        int j = i - 1;
        while (j > 0 && super.isIdentifierChar(str, j)) {
            j--;
        }
        
        if (str[j] != '(') {
            return true;
        }
        
        if (isSubstringEqual(str, j - "defined".length(), "defined")) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean isIdentifierChar(char[] str, int i) {
        return super.isIdentifierChar(str, i)
                || (str[i] == '(')
                || (str[i] == ')');
    }
    
    @Override
    public Formula makeIdentifierFormula(String identifier) throws ExpressionFormatException {
        if (identifier.equals("1")) {
            return True.INSTANCE;
        }
        if (identifier.equals("0")) {
            return False.INSTANCE;
        }
        
        if (!identifier.matches("defined\\([a-zA-Z0-9_]+\\)")) {
            throw new ExpressionFormatException("Identifier \"" + identifier
                    + "\" is not valid defined() expression");
        }
        
        identifier = identifier.substring("defined(".length(), identifier.length() - 1);
        
        return super.makeIdentifierFormula(identifier);
    }
    
}
