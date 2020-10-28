/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.fathzer.soft.javaluator.BracketPair;
import com.fathzer.soft.javaluator.Constant;
import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import com.fathzer.soft.javaluator.Parameters;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.extensions.parameters.expressions.operators.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Describes basic properties of a {@link ExpressionParameter}
 */
public abstract class DefaultAbstractExpressionEvaluator extends ExpressionEvaluator {
    public static final Constant CONSTANT_TRUE = new Constant("TRUE");
    public static final Constant CONSTANT_FALSE = new Constant("FALSE");
    public static final ExpressionOperator OPERATOR_NEGATE_SYMBOL = new SymbolLogicalNotOperator();
    public static final ExpressionOperator OPERATOR_NEGATE_TEXT = new TextLogicalNotOperator();
    public static final ExpressionOperator OPERATOR_AND_SYMBOL = new SymbolLogicalAndOperator();
    public static final ExpressionOperator OPERATOR_AND_TEXT = new TextLogicalAndOperator();
    public static final ExpressionOperator OPERATOR_OR_SYMBOL = new SymbolLogicalOrOperator();
    public static final ExpressionOperator OPERATOR_OR_TEXT = new TextLogicalOrOperator();
    public static final ExpressionOperator OPERATOR_XOR_TEXT = new TextXOrOperator();
    public static final NumericPredicateOperator OPERATOR_NUMERIC_EQUALS = new NumericEqualityPredicateOperator();
    public static final NumericPredicateOperator OPERATOR_NUMERIC_LESS_THAN = new NumericLessThanPredicateOperator();
    public static final NumericPredicateOperator OPERATOR_NUMERIC_GREATER_THAN = new NumericGreaterThanPredicateOperator();
    public static final NumericPredicateOperator OPERATOR_NUMERIC_LESS_THAN_OR_EQUAL = new NumericLessThanOrEqualPredicateOperator();
    public static final NumericPredicateOperator OPERATOR_NUMERIC_GREATER_THAN_OR_EQUAL = new NumericGreaterThanOrEqualPredicateOperator();
    public static final NumericOrStringFunctionOperator OPERATOR_NUMERIC_STRING_PLUS = new NumericPlusFunctionOperator();
    public static final NumericFunctionOperator OPERATOR_NUMERIC_MINUS = new NumericMinusFunctionOperator();
    public static final NumericFunctionOperator OPERATOR_NUMERIC_MULTIPLY = new NumericMultiplyFunctionOperator();
    public static final NumericFunctionOperator OPERATOR_NUMERIC_DIVIDE = new NumericDivideFunctionOperator();
    public static final NumericFunctionOperator OPERATOR_NUMERIC_MODULO = new NumericModuloFunctionOperator();
    public static final NumericFunctionOperator OPERATOR_NUMERIC_EXPONENT = new NumericExponentFunctionOperator();
    public static final Operator OPERATOR_NUMERIC_NEGATE = new Operator("-", 1,Operator.Associativity.RIGHT, 8);
    public static final Operator OPERATOR_NUMERIC_NEGATE_HIGH = new Operator("-", 1,Operator.Associativity.RIGHT, 10);
    public static final Parameters PARAMETERS;

    static {
        PARAMETERS = new Parameters();
        PARAMETERS.addFunctionBracket(BracketPair.PARENTHESES);
        PARAMETERS.addExpressionBracket(BracketPair.PARENTHESES);
        PARAMETERS.add(OPERATOR_NEGATE_SYMBOL);
        PARAMETERS.add(OPERATOR_NEGATE_TEXT);
        PARAMETERS.add(OPERATOR_AND_SYMBOL);
        PARAMETERS.add(OPERATOR_AND_TEXT);
        PARAMETERS.add(OPERATOR_OR_SYMBOL);
        PARAMETERS.add(OPERATOR_OR_TEXT);
        PARAMETERS.add(OPERATOR_XOR_TEXT);
        PARAMETERS.add(OPERATOR_NUMERIC_EQUALS);
        PARAMETERS.add(OPERATOR_NUMERIC_LESS_THAN);
        PARAMETERS.add(OPERATOR_NUMERIC_GREATER_THAN);
        PARAMETERS.add(OPERATOR_NUMERIC_LESS_THAN_OR_EQUAL);
        PARAMETERS.add(OPERATOR_NUMERIC_GREATER_THAN_OR_EQUAL);

        PARAMETERS.add(OPERATOR_NUMERIC_STRING_PLUS);
        PARAMETERS.add(OPERATOR_NUMERIC_MINUS);
        PARAMETERS.add(OPERATOR_NUMERIC_MULTIPLY);
        PARAMETERS.add(OPERATOR_NUMERIC_DIVIDE);
        PARAMETERS.add(OPERATOR_NUMERIC_MODULO);
        PARAMETERS.add(OPERATOR_NUMERIC_EXPONENT);
        PARAMETERS.add(OPERATOR_NUMERIC_NEGATE);
    }

    public DefaultAbstractExpressionEvaluator() {
        super(PARAMETERS);
    }

    @Override
    protected Iterator<String> tokenize(String expression) {
        // Collect all tokens and constants
        Set<String> operators = new HashSet<>();
        operators.add("TRUE");
        operators.add("FALSE");
        for (Operator operator : getOperators()) {
            operators.add(operator.getSymbol());
        }

        List<String> tokens = new ArrayList<>();
        // First split by space
        for (String subToken : expression.split("\\s+")) {
            if (operators.contains(subToken)) {
                // Prevent further tokenization
                tokens.add(subToken);
            } else {
                tokens.addAll(ImmutableList.copyOf(super.tokenize(subToken)));
            }
        }

        return tokens.iterator();
    }

    @Override
    protected Object evaluate(Function function, Iterator<Object> arguments, Object evaluationContext) {
        return super.evaluate(function, arguments, evaluationContext);
    }

    @Override
    protected Object evaluate(Constant constant, Object evaluationContext) {
        if(constant == CONSTANT_TRUE) {
            return true;
        }
        else if(constant == CONSTANT_FALSE) {
            return false;
        }
        else {
            throw new UnsupportedOperationException("Unsupported constant: " + constant.getName());
        }
    }

    @Override
    protected Object evaluate(Operator operator, Iterator<Object> operands, Object evaluationContext) {
        if(operator instanceof ExpressionOperator) {
            return ((ExpressionOperator) operator).evaluate(operands, evaluationContext);
        }
        else if(operator == OPERATOR_NUMERIC_NEGATE || operator == OPERATOR_NUMERIC_NEGATE_HIGH) {
            return -(double)operands.next();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object toValue(String literal, Object evaluationContext) {
        if("TRUE".equals(literal))
            return true;
        else if("FALSE".equals(literal))
            return false;
        else if(NumberUtils.isCreatable(literal))
            return NumberUtils.createDouble(literal);
        return null;
    }

    public static void main(String[] args) {
        DefaultAbstractExpressionEvaluator executor = new DefaultAbstractExpressionEvaluator() {
        };
        System.out.println(executor.evaluate("--10 + 10"));
    }
}
