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

package org.hkijena.jipipe.extensions.expressions;

import com.fathzer.soft.javaluator.*;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.expressions.constants.*;
import org.hkijena.jipipe.extensions.expressions.operators.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Describes basic properties of a {@link ExpressionParameter}
 */
public class DefaultExpressionEvaluator extends ExpressionEvaluator {
    public static final ExpressionConstant CONSTANT_NULL = new NullConstant();
    public static final ExpressionConstant CONSTANT_TRUE = new BooleanTrueConstant();
    public static final ExpressionConstant CONSTANT_FALSE = new BooleanFalseConstant();
    public static final ExpressionConstant CONSTANT_PI = new NumericPiConstant();
    public static final ExpressionConstant CONSTANT_TAU = new NumericTauConstant();
    public static final ExpressionConstant CONSTANT_E = new NumericEulerConstant();
    public static final ExpressionOperator OPERATOR_NEGATE_SYMBOL = new LogicalNotOperator("!");
    public static final ExpressionOperator OPERATOR_NEGATE_TEXT = new LogicalNotOperator("NOT");
    public static final ExpressionOperator OPERATOR_AND_SYMBOL = new LogicalAndOperator("&");
    public static final ExpressionOperator OPERATOR_AND_TEXT = new LogicalAndOperator("AND");
    public static final ExpressionOperator OPERATOR_OR_SYMBOL = new LogicalOrOperator("|");
    public static final ExpressionOperator OPERATOR_OR_TEXT = new LogicalOrOperator("OR");
    public static final ExpressionOperator OPERATOR_XOR_TEXT = new LogicalXOrOperator("XOR");
    public static final ExpressionOperator OPERATOR_NUMERIC_EQUALS = new EqualityPredicateOperator("==");
    public static final ExpressionOperator OPERATOR_NUMERIC_EQUALS_TEXT = new EqualityPredicateOperator("EQUALS");
    public static final ExpressionOperator OPERATOR_NUMERIC_UNEQUALS = new InequalityPredicateOperator("!=");
    public static final ExpressionOperator OPERATOR_NUMERIC_UNEQUALS_TEXT = new InequalityPredicateOperator("UNEQUAL");
    public static final ExpressionOperator OPERATOR_NUMERIC_LESS_THAN = new NumericLessThanPredicateOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_GREATER_THAN = new NumericGreaterThanPredicateOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_LESS_THAN_OR_EQUAL = new NumericLessThanOrEqualPredicateOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_GREATER_THAN_OR_EQUAL = new NumericGreaterThanOrEqualPredicateOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_STRING_PLUS = new AdditionFunctionOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_MINUS = new SubtractionFunctionOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_MULTIPLY = new NumericMultiplyFunctionOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_DIVIDE = new NumericDivideFunctionOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_MODULO = new NumericModuloFunctionOperator();
    public static final ExpressionOperator OPERATOR_NUMERIC_EXPONENT = new NumericExponentFunctionOperator();
    public static final Operator OPERATOR_NUMERIC_NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 8);
    public static final Operator OPERATOR_NUMERIC_NEGATE_HIGH = new Operator("-", 1, Operator.Associativity.RIGHT, 10);
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS = new ContainsOperator();
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS2 = new ContainsOperator2();
    public static final ExpressionOperator OPERATOR_VARIABLE_EXISTS = new VariableExistsOperator();
    public static final ExpressionOperator OPERATOR_VARIABLE_RESOLVE = new ResolveVariableOperator();
    public static final ExpressionOperator OPERATOR_ELEMENT_ACCESS_TEXT = new ElementAccessOperator("AT");
    public static final ExpressionOperator OPERATOR_ELEMENT_ACCESS_SYMBOL = new ElementAccessOperator("@");
    public static final ExpressionOperator OPERATOR_STATEMENT = new StatementOperator();

    private final Set<String> knownOperatorTokens = new HashSet<>();
    private final List<String> knownNonAlphanumericOperatorTokens = new ArrayList<>();

    public DefaultExpressionEvaluator() {
        super(createParameters());
        for (Operator operator : getOperators()) {
            knownOperatorTokens.add(operator.getSymbol());
            if (!operator.getSymbol().matches("[A-Za-z0-9_]+")) {
                knownNonAlphanumericOperatorTokens.add(operator.getSymbol());
            }
        }
        knownNonAlphanumericOperatorTokens.sort(Comparator.comparing(String::length).reversed());
    }

    public static Parameters createParameters() {
        Parameters parameters = new Parameters();
        parameters.addFunctionBracket(BracketPair.PARENTHESES);
        parameters.addExpressionBracket(BracketPair.PARENTHESES);
        parameters.add(OPERATOR_STATEMENT);

        parameters.add(CONSTANT_NULL);
        parameters.add(CONSTANT_TRUE);
        parameters.add(CONSTANT_FALSE);
        parameters.add(CONSTANT_E);
        parameters.add(CONSTANT_PI);
        parameters.add(CONSTANT_TAU);

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

        // Add variable operators
        parameters.add(OPERATOR_VARIABLE_EXISTS);
        parameters.add(OPERATOR_VARIABLE_RESOLVE);
        parameters.add(OPERATOR_ELEMENT_ACCESS_TEXT);
        parameters.add(OPERATOR_ELEMENT_ACCESS_SYMBOL);

        // Add operators from JIPipe (if available)
        if (JIPipe.getInstance() != null) {
            for (JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry : JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().values()) {
                parameters.add(functionEntry.getFunction());
            }
        }

        return parameters;
    }

    /**
     * Escapes a variable name into a valid expression
     *
     * @param variableName the variable name
     * @return an expression that evaluates the variable
     */
    public static String escapeVariable(String variableName) {
        if (variableName.contains(" ") || variableName.contains("(") || variableName.contains(")"))
            variableName = "$\"" + DefaultExpressionEvaluator.escapeString(variableName) + "\"";
        else {
            List<String> tokens = DefaultExpressionParameter.getEvaluatorInstance().getKnownNonAlphanumericOperatorTokens();
            boolean processed = false;
            for (String token : tokens) {
                if (variableName.contains(token)) {
                    variableName = "$\"" + DefaultExpressionEvaluator.escapeString(variableName) + "\"";
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                for (Operator operator : DefaultExpressionParameter.getEvaluatorInstance().getOperators()) {
                    if (operator.getSymbol().equals(variableName)) {
                        variableName = "$\"" + DefaultExpressionEvaluator.escapeString(variableName) + "\"";
                        processed = true;
                        break;
                    }
                }
            }
            if (!processed) {
                for (Constant constant : DefaultExpressionParameter.getEvaluatorInstance().getConstants()) {
                    if (constant.getName().equals(variableName)) {
                        variableName = "$\"" + DefaultExpressionEvaluator.escapeString(variableName) + "\"";
                        processed = true;
                        break;
                    }
                }
            }
        }
        return variableName;
    }

    /**
     * Escapes a string, so it can be used within quotes
     *
     * @param string the string
     * @return string with quotes and backslashes escaped
     */
    public static String escapeString(String string) {
        return string.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Deep-copies an object.
     * Supports handling {@link Collection} and {@link Map}
     *
     * @param object the object
     * @return the copy
     */
    public static Object deepCopyObject(Object object) {
        if (object == null)
            return null;
        if (object instanceof Collection) {
            List<Object> result = new ArrayList<>();
            Collection<?> collection = (Collection<?>) object;
            for (Object item : collection) {
                result.add(deepCopyObject(item));
            }
            return result;
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            Map<Object, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(deepCopyObject(entry.getKey()), deepCopyObject(entry.getValue()));
            }
            return result;
        } else {
            return object;
        }
    }

    public List<String> tokenize(String expression, boolean includeQuotesAsToken, boolean includeQuotesIntoToken) {
        StringBuilder buffer = new StringBuilder();
        boolean isQuoted = false;
        boolean escape = false;
        AtomicBoolean resolveVariable = new AtomicBoolean(false);
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '"' && !escape) {
                if (!isQuoted) {
                    // Process buffer up until now
                    flushBufferToToken(buffer, tokens, resolveVariable);
                    isQuoted = true;
                } else {
                    // Add buffer as whole token
                    if (includeQuotesIntoToken) {
                        buffer.insert(0, '"');
                        buffer.append("\"");
                    }
                    flushBufferToToken(buffer, tokens, resolveVariable);
                    isQuoted = false;
                }
                if (includeQuotesAsToken) {
                    tokens.add("\"");
                }
            } else if (!isQuoted && (c == ')' || c == '(' || c == ',')) {
                flushBufferToToken(buffer, tokens, resolveVariable);
                tokens.add("" + c);
            } else if (!isQuoted && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
                flushBufferToToken(buffer, tokens, resolveVariable);
            } else if (c == '\\') {
                if (escape)
                    buffer.append(c);
                escape = !escape;
            } else if (c == '"') {
                buffer.append(c);
                escape = false;
            } else if (!isQuoted && c == '$') {
                flushBufferToToken(buffer, tokens, resolveVariable);
                resolveVariable.set(true);
                tokens.add("" + c);
            } else {
                buffer.append(c);
            }
            if (!isQuoted && buffer.length() > 0) {
                String s1 = buffer.toString();
                if (i != expression.length() - 1) {
                    // Workaround <= >=
                    if (s1.endsWith("<") || s1.endsWith(">")) {
                        char next = expression.charAt(i + 1);
                        if (next == '=')
                            continue;
                    }
                    // Workaround !=
                    if (s1.endsWith("!")) {
                        char next = expression.charAt(i + 1);
                        if (next == '=')
                            continue;
                    }
                }
                for (String s : knownNonAlphanumericOperatorTokens) {
                    int i1 = s1.indexOf(s);
                    if (i1 != -1) {
                        if (i1 > 0)
                            tokens.add(s1.substring(0, i1));
                        tokens.add(s);
                        buffer.setLength(0);
                        break;
                    }
                }
            }
        }

        flushBufferToToken(buffer, tokens, resolveVariable);
        return tokens;
    }

    private void flushBufferToToken(StringBuilder buffer, List<String> tokens, AtomicBoolean resolveVariable) {
        if (buffer.length() > 0) {
            if (resolveVariable.get()) {
                // Escape on resolving a variable
                if (buffer.charAt(0) != '"' && buffer.charAt(buffer.length() - 1) != '"') {
                    buffer.insert(0, '"');
                    buffer.append('"');
                }
            }
            tokens.add(buffer.toString());
            buffer.setLength(0);
        }
        resolveVariable.set(false);
    }

    @Override
    protected Iterator<String> tokenize(String expression) {
        return tokenize(expression, false, true).iterator();
    }

    @Override
    public Object evaluate(String expression, Object evaluationContext) {
        ExpressionVariables expressionVariables = (ExpressionVariables) evaluationContext;
        try {
            expression = StringUtils.stripEnd(expression.trim(), ";");
            if (expression.isEmpty())
                return true;
            return super.evaluate(expression, evaluationContext);
        } catch (Exception e) {
            throw new UserFriendlyRuntimeException(e,
                    "Error while evaluating expression",
                    "Expression: " + expression,
                    "The expression could not be evaluated. Available variables are " + expressionVariables.entrySet().stream()
                            .map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.joining(" ")),
                    "Please check if the expression is correct.");
        }
    }

    @Override
    protected Object evaluate(Function function, Iterator<Object> arguments, Object evaluationContext) {
        if (function instanceof ExpressionFunction) {
            return ((ExpressionFunction) function).evaluate(ImmutableList.copyOf(arguments), (ExpressionVariables) evaluationContext);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected Object evaluate(Constant constant, Object evaluationContext) {
        if (constant instanceof ExpressionConstant) {
            return ((ExpressionConstant) constant).getValue();
        } else {
            throw new UnsupportedOperationException("Unsupported constant: " + constant.getName());
        }
    }

    @Override
    protected Object evaluate(Operator operator, Iterator<Object> operands, Object evaluationContext) {
        if (operator instanceof ExpressionOperator) {
            return ((ExpressionOperator) operator).evaluate(operands, (ExpressionVariables) evaluationContext);
        } else if (operator == OPERATOR_NUMERIC_NEGATE || operator == OPERATOR_NUMERIC_NEGATE_HIGH) {
            return -(double) operands.next();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Checks the syntax of an expression.
     *
     * @return the exception that was thrown if some error was returned. Null if syntax is correct.
     */
    public Exception checkSyntax(String expression) {
        int quoteStack = 0;
        int paraStack = 0;
        boolean canLiteral = true;
        List<String> tokens = tokenize(expression, true, true);
        for (String token : tokens) {
            switch (token) {
                case "(":
                    ++paraStack;
                    canLiteral = true;
                    break;
                case ")":
                    if (paraStack <= 0)
                        return new IllegalArgumentException("Unmatched parentheses!");
                    --paraStack;
                    break;
                case ",":
                    canLiteral = true;
                    break;
                case "\"":
                    if (quoteStack == 0)
                        ++quoteStack;
                    else if (quoteStack == 1)
                        --quoteStack;
                    else
                        return new IllegalArgumentException("Unmatched double quotes!");
                    break;
                default: {
                    if (knownOperatorTokens.contains(token))
                        canLiteral = true;
                    else if (!canLiteral) {
                        return new IllegalArgumentException("Literal follows another literal!");
                    } else {
                        canLiteral = false;
                    }
                }
            }
        }
        if (quoteStack != 0)
            return new IllegalArgumentException("Unmatched double quotes!");
        if (paraStack != 0)
            return new IllegalArgumentException("Unmatched parentheses!");
        return null;
    }

    @Override
    protected Object toValue(String literal, Object evaluationContext) {
        ExpressionVariables variableSet = (ExpressionVariables) evaluationContext;
        if (NumberUtils.isCreatable(literal))
            return NumberUtils.createDouble(literal);
        else if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\""))
            return literal.substring(1, literal.length() - 1);
        else {
            Object variable = variableSet.get(literal);
            if (variable == null) {
                throw new UserFriendlyRuntimeException(new NullPointerException(), "Unable to find variable '" + literal + "' in expression",
                        "Expression parser",
                        "Your expression has a variable '" + literal + "', but it does not exist",
                        "Check if the variable exists. If you intended to create a string, put double quotes around it.");
            }
            return variable;
        }
    }

    public List<String> getKnownNonAlphanumericOperatorTokens() {
        return knownNonAlphanumericOperatorTokens;
    }
}
