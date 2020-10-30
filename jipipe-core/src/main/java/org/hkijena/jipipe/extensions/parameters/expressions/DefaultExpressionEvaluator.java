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
import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.parameters.expressions.operators.*;

import java.util.*;

/**
 * Describes basic properties of a {@link ExpressionParameter}
 */
public class DefaultExpressionEvaluator extends ExpressionEvaluator {
    public static final Constant CONSTANT_TRUE = new Constant("TRUE");
    public static final Constant CONSTANT_FALSE = new Constant("FALSE");
    public static final ExpressionOperator OPERATOR_NEGATE_SYMBOL = new SymbolLogicalNotOperator();
    public static final ExpressionOperator OPERATOR_NEGATE_TEXT = new TextLogicalNotOperator();
    public static final ExpressionOperator OPERATOR_AND_SYMBOL = new SymbolLogicalAndOperator();
    public static final ExpressionOperator OPERATOR_AND_TEXT = new TextLogicalAndOperator();
    public static final ExpressionOperator OPERATOR_OR_SYMBOL = new SymbolLogicalOrOperator();
    public static final ExpressionOperator OPERATOR_OR_TEXT = new TextLogicalOrOperator();
    public static final ExpressionOperator OPERATOR_XOR_TEXT = new TextLogicalXOrOperator();
    public static final NumericOrStringPredicateOperator OPERATOR_NUMERIC_EQUALS = new NumericOrStringEqualityPredicateOperator();
    public static final NumericOrStringPredicateOperator OPERATOR_NUMERIC_EQUALS_TEXT = new NumericOrStringTextEqualityPredicateOperator();
    public static final NumericOrStringPredicateOperator OPERATOR_NUMERIC_UNEQUALS = new NumericOrStringInEqualityPredicateOperator();
    public static final NumericOrStringPredicateOperator OPERATOR_NUMERIC_UNEQUALS_TEXT = new NumericOrStringTextInEqualityPredicateOperator();
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
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS = new StringContainsOperator();
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS2 = new StringContainsOperator2();

    private final Set<String> knownOperatorTokens = new HashSet<>();
    private final List<String> knownNonAlphanumericOperatorTokens = new ArrayList<>();

    public static Parameters createParameters() {
        Parameters parameters = new Parameters();
        parameters.addFunctionBracket(BracketPair.PARENTHESES);
        parameters.addExpressionBracket(BracketPair.PARENTHESES);

        // Add boolean operators
        parameters.add(OPERATOR_NEGATE_SYMBOL);
        parameters.add(OPERATOR_NEGATE_TEXT);
        parameters.add(OPERATOR_AND_SYMBOL);
        parameters.add(OPERATOR_AND_TEXT);
        parameters.add(OPERATOR_OR_SYMBOL);
        parameters.add(OPERATOR_OR_TEXT);
        parameters.add(OPERATOR_XOR_TEXT);
        parameters.add(OPERATOR_NUMERIC_EQUALS);
        parameters.add(OPERATOR_NUMERIC_LESS_THAN);
        parameters.add(OPERATOR_NUMERIC_GREATER_THAN);
        parameters.add(OPERATOR_NUMERIC_LESS_THAN_OR_EQUAL);
        parameters.add(OPERATOR_NUMERIC_GREATER_THAN_OR_EQUAL);
        parameters.add(OPERATOR_NUMERIC_EQUALS_TEXT);
        parameters.add(OPERATOR_NUMERIC_UNEQUALS);
        parameters.add(OPERATOR_NUMERIC_UNEQUALS_TEXT);

        // Add numeric operators
        parameters.add(OPERATOR_NUMERIC_STRING_PLUS);
        parameters.add(OPERATOR_NUMERIC_MINUS);
        parameters.add(OPERATOR_NUMERIC_MULTIPLY);
        parameters.add(OPERATOR_NUMERIC_DIVIDE);
        parameters.add(OPERATOR_NUMERIC_MODULO);
        parameters.add(OPERATOR_NUMERIC_EXPONENT);
        parameters.add(OPERATOR_NUMERIC_NEGATE);

        // Add string operators
        parameters.add(OPERATOR_STRING_CONTAINS);
        parameters.add(OPERATOR_STRING_CONTAINS2);

        // Add operators from JIPipe (if available)
        if(JIPipe.getInstance() != null) {
            for (JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry : JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().values()) {
                parameters.add(functionEntry.getFunction());
            }
        }

        return parameters;
    }

    public DefaultExpressionEvaluator() {
        super(createParameters());
        knownOperatorTokens.add("TRUE");
        knownOperatorTokens.add("FALSE");
        for (Operator operator : getOperators()) {
            knownOperatorTokens.add(operator.getSymbol());
            if(!operator.getSymbol().matches("[A-Za-z0-9_]+")) {
                knownNonAlphanumericOperatorTokens.add(operator.getSymbol());
            }
        }
        knownNonAlphanumericOperatorTokens.sort(Comparator.comparing(String::length).reversed());
    }

    public List<String> tokenize(String expression, boolean includeQuotesAsToken, boolean includeQuotesIntoToken) {
        StringBuilder buffer = new StringBuilder();
        boolean isQuoted = false;
        boolean escape = false;
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if(c == '"' && !escape) {
                if(!isQuoted) {
                    // Process buffer up until now
                    if (buffer.length() > 0)
                        tokens.add(buffer.toString());
                    buffer.setLength(0);
                    isQuoted = true;
                }
                else {
                    // Add buffer as whole token
                    if(includeQuotesIntoToken) {
                        buffer.insert(0, '"');
                        buffer.append("\"");
                    }
                    tokens.add(buffer.toString());
                    buffer.setLength(0);
                    isQuoted = false;
                }
                if(includeQuotesAsToken) {
                    tokens.add("\"");
                }
            }
            else if(c == ')' || c == '(' || c == ',') {
                if (buffer.length() > 0)
                    tokens.add(buffer.toString());
                buffer.setLength(0);
                tokens.add(""  + c);
            }
            else if(!isQuoted && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
                if (buffer.length() > 0)
                    tokens.add(buffer.toString());
                buffer.setLength(0);
            }
            else if(c == '\\') {
                if(escape)
                    buffer.append(c);
                escape = !escape;
            }
            else if(c == '"') {
                buffer.append(c);
                escape = false;
            }
            else {
                buffer.append(c);
            }
            if(!isQuoted && buffer.length() > 0) {
                String s1 = buffer.toString();
                if(i != expression.length() - 1) {
                    // Workaround <= >=
                    if(s1.endsWith("<") || s1.endsWith(">")) {
                        char next = expression.charAt(i + 1);
                        if(next == '=')
                            continue;
                    }
                }
                for (String s : knownNonAlphanumericOperatorTokens) {
                    int i1 = s1.indexOf(s);
                    if(i1 != -1) {
                        if(i1 > 0)
                            tokens.add(s1.substring(0, i1));
                        tokens.add(s);
                        buffer.setLength(0);
                        break;
                    }
                }
            }
        }

        if(buffer.length() > 0)
            tokens.add(buffer.toString());
        return tokens;
    }

    @Override
    protected Iterator<String> tokenize(String expression) {
        return tokenize(expression, false, true).iterator();
    }

    @Override
    public Object evaluate(String expression, Object evaluationContext) {
        if(expression.trim().isEmpty())
            return true;
        return super.evaluate(expression, evaluationContext);
    }

    @Override
    protected Object evaluate(Function function, Iterator<Object> arguments, Object evaluationContext) {
        if(function instanceof ExpressionFunction) {
            return ((ExpressionFunction) function).evaluate(ImmutableList.copyOf(arguments), (StaticVariableSet<Object>) evaluationContext);
        }
        else {
            throw new UnsupportedOperationException();
        }
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
        StaticVariableSet<Object> variableSet = (StaticVariableSet<Object>) evaluationContext;
        if("TRUE".equals(literal))
            return true;
        else if("FALSE".equals(literal))
            return false;
        else if(NumberUtils.isCreatable(literal))
            return NumberUtils.createDouble(literal);
        else if(literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\""))
            return literal.substring(1, literal.length() - 1);
        else {
            Object variable = variableSet.get(literal);
            if(variable == null) {
                throw new UserFriendlyRuntimeException(new NullPointerException(), "Unable to find variable '" + literal + "' in expression",
                        "Expression parser",
                        "Your expression has a variable '" + literal + "', but it does not exist",
                        "Check if the variable exists. If you intended to create a string, put double quotes around it.");
            }
            return variable;
        }
    }
}
