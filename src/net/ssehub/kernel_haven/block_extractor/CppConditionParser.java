package net.ssehub.kernel_haven.block_extractor;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.cpp.parser.CppOperator;
import net.ssehub.kernel_haven.util.cpp.parser.CppParser;
import net.ssehub.kernel_haven.util.cpp.parser.ast.CppExpression;
import net.ssehub.kernel_haven.util.cpp.parser.ast.FunctionCall;
import net.ssehub.kernel_haven.util.cpp.parser.ast.ICppExressionVisitor;
import net.ssehub.kernel_haven.util.cpp.parser.ast.IntegerLiteral;
import net.ssehub.kernel_haven.util.cpp.parser.ast.Operator;
import net.ssehub.kernel_haven.util.cpp.parser.ast.Variable;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Parses boolean CPP conditions.
 *
 * @author Adam
 */
public class CppConditionParser implements ICppExressionVisitor<@NonNull Formula> {

    private boolean handleLinuxMacros;
    
    private boolean fuzzyParsing;
    
    private CppParser cppParser;
    
    /**
     * Creates a new {@link CppConditionParser}.
     * 
     * @param handleLinuxMacros Whether to handle preprocessor macros found in the Linux Kernel (i.e.
     *      IS_ENABLED, IS_BUILTIN, IS_MODULE).
     * @param fuzzyParsing Whether to do fuzzy parsing for non-boolean integer comparisons.
     */
    public CppConditionParser(boolean handleLinuxMacros, boolean fuzzyParsing) {
        this.handleLinuxMacros = handleLinuxMacros;
        this.fuzzyParsing = fuzzyParsing;
        this.cppParser = new CppParser();
    }
    
    /**
     * Parses the given CPP expression into a boolean {@link Formula}.
     * 
     * @param expression The expression to parse.
     * 
     * @return The boolean formula created from the given expression..
     * 
     * @throws FormatException If the expression can not be parsed into a boolean formula.
     */
    public @NonNull Formula parse(@NonNull String expression) throws FormatException {
        Formula result;
        try {
            result = cppParser.parse(expression).accept(this);
        } catch (ExpressionFormatException e) {
            throw new FormatException(e);
        }
        
        return result;
    }

    @Override
    public @NonNull Formula visitFunctionCall(@NonNull FunctionCall call) throws ExpressionFormatException {
        
        String function = call.getFunctionName();
        CppExpression arg = call.getArgument();
        net.ssehub.kernel_haven.util.logic.Variable variable;
        if (arg == null) {
            throw new ExpressionFormatException("Can't handle function " + function + " without argument");
        }
        if (arg instanceof Variable) {
            variable = new net.ssehub.kernel_haven.util.logic.Variable(((Variable) arg).getName());
        } else {
            throw new ExpressionFormatException("defined() call without variable");
        }
        
        Formula result;
        if (function.equals("defined")) {
            result = variable;
            
        } else if (handleLinuxMacros && function.equals("IS_ENABLED")) {
            result = new Disjunction(variable,
                    new net.ssehub.kernel_haven.util.logic.Variable(variable.getName() + "_MODULE"));
            
        } else if (handleLinuxMacros && function.equals("IS_MODULE")) {
            result = new net.ssehub.kernel_haven.util.logic.Variable(variable.getName() + "_MODULE");
            
        } else if (handleLinuxMacros && function.equals("IS_BUILTIN")) {
            result = variable;
            
        } else {
            throw new ExpressionFormatException("Unsupprted function/macro: " + function);
        }
        
        return result;
    }

    @Override
    public @NonNull Formula visitVariable(@NonNull Variable variable) throws ExpressionFormatException {
        if (fuzzyParsing) {
            return new net.ssehub.kernel_haven.util.logic.Variable(variable.getName() + "_ne_0");
        }
        
        throw new ExpressionFormatException("Found variable outside of defined() call: " + variable);
    }

    @Override
    public @NonNull Formula visitOperator(@NonNull Operator operator) throws ExpressionFormatException {
        Formula result;
        
        switch (operator.getOperator()) {
        case BOOL_AND:
            result = new Conjunction(operator.getLeftSide().accept(this),
                    notNull(operator.getRightSide()).accept(this));
            break;
        case BOOL_OR:
            result = new Disjunction(operator.getLeftSide().accept(this),
                    notNull(operator.getRightSide()).accept(this));
            break;
        case BOOL_NOT:
            result = new Negation(operator.getLeftSide().accept(this));
            break;
            
        case CMP_EQ:
        case CMP_NE:
        case CMP_LT:
        case CMP_LE:
        case CMP_GT:
        case CMP_GE:
            result = fuzzyParse(operator);
            break;
            
        case INT_SUB_UNARY:
            if (operator.getLeftSide() instanceof IntegerLiteral) {
                // support -LITERAL, e.g. -2; everything != 0 is TRUE
                result = ((IntegerLiteral) operator.getLeftSide()).getValue() != 0 ? True.INSTANCE : False.INSTANCE;
                
            } else {
                throw new ExpressionFormatException("Unsupported operator: " + operator.getOperator());
            }
            break;
            
        default:
            throw new ExpressionFormatException("Unsupported operator: " + operator.getOperator());
        }
        
        return result;
    }

    @Override
    public @NonNull Formula visitLiteral(@NonNull IntegerLiteral literal) throws ExpressionFormatException {
        Formula result;
        
        if (literal.getValue() == 0) {
            result = False.INSTANCE;
        } else {
            result = True.INSTANCE;
        }
        
        return result;
    }
    
    /**
     * Fuzzy-parses the given operator.
     * 
     * @param op The operator to parse. Must be a comparator operator.
     * 
     * @return The result of the fuzzy parsing.
     * 
     * @throws ExpressionFormatException If the given operator cannot be fuzzy-parsed.
     */
    private @NonNull Formula fuzzyParse(@NonNull Operator op) throws ExpressionFormatException {
        if (!fuzzyParsing) {
            throw new ExpressionFormatException(op.getOperator() + " is only supported if fuzzy parsing is enabled");
        }
        
        CppExpression leftSide = op.getLeftSide();
        CppExpression rightSide = notNull(op.getRightSide());
        
        String variable;
        String opStr;
        String value;
        
        if (leftSide instanceof Variable && rightSide instanceof IntegerLiteral) {
            variable = ((Variable) leftSide).getName();
            value = String.valueOf(((IntegerLiteral) rightSide).getValue());
            opStr = getOpString(op.getOperator(), false);
            
        } else if (leftSide instanceof IntegerLiteral && rightSide instanceof Variable) {
            variable = ((Variable) rightSide).getName();
            value = String.valueOf((((IntegerLiteral) leftSide).getValue()));
            opStr = getOpString(op.getOperator(), true);
            
        } else if (leftSide instanceof Variable && rightSide instanceof Variable) {
            variable = ((Variable) leftSide).getName();
            value = ((Variable) rightSide).getName();
            opStr = getOpString(op.getOperator(), false);
            
        } else {
            throw new ExpressionFormatException("Can only fuzzy-parse variables compared with integer literals "
                    + "or other variables");
        }
        
        return new net.ssehub.kernel_haven.util.logic.Variable(variable + opStr + value);
    }
    
    /**
     * Returns a string to be used in variable names as a replacement for the given operator.
     * 
     * @param op The operator to replace.
     * @param switched Whether greater and less than comparisons should be inverted.
     * 
     * @return A string to be used as a replacement for the given operator.
     * 
     * @throws ExpressionFormatException If the given operator is not a comparator.
     */
    private String getOpString(CppOperator op, boolean switched) throws ExpressionFormatException {
        String opStr;
        
        switch (op) {
        case CMP_EQ:
            opStr = "_eq_";
            break;
        case CMP_NE:
            opStr = "_ne_";
            break;
        case CMP_LT:
            opStr = switched ? "_gt_" : "_lt_";
            break;
        case CMP_LE:
            opStr = switched ? "_ge_" : "_le_";
            break;
        case CMP_GT:
            opStr = switched ? "_lt_" : "_gt_";
            break;
        case CMP_GE:
            opStr = switched ? "_le_" : "_ge_";
            break;
            
        default:
            throw new ExpressionFormatException("Unsupported operator: " + op);
        }
        
        return opStr;
    }
    
}