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
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.expressions.constants.*;
import org.hkijena.jipipe.extensions.expressions.operators.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Describes basic properties of a {@link AbstractExpressionParameter}
 */
public class JIPipeExpressionEvaluator extends ExpressionEvaluator {
    public static final ExpressionConstant CONSTANT_NULL = new NullConstant();
    public static final ExpressionConstant CONSTANT_NEWLINE = new NewLineConstant();
    public static final ExpressionConstant CONSTANT_TRUE = new BooleanTrueConstant();
    public static final ExpressionConstant CONSTANT_FALSE = new BooleanFalseConstant();
    public static final ExpressionConstant CONSTANT_PI = new NumericPiConstant();
    public static final ExpressionConstant CONSTANT_TAU = new NumericTauConstant();
    public static final ExpressionConstant CONSTANT_E = new NumericEulerConstant();
    public static final ExpressionConstant CONSTANT_POSITIVE_INFINITY = new NumericInfinityConstant();
    public static final ExpressionConstant CONSTANT_NEGATIVE_INFINITY = new NumericNegativeInfinityConstant();
    public static final ExpressionConstant CONSTANT_NAN = new NumericNaNInfinityConstant();
    public static final ExpressionOperator OPERATOR_NEGATE_SYMBOL = new LogicalNotOperator("!", 3);
    public static final ExpressionOperator OPERATOR_NEGATE_TEXT = new LogicalNotOperator("NOT", 3);
    public static final ExpressionOperator OPERATOR_AND_SYMBOL = new LogicalAndOperator("&", 2);
    public static final ExpressionOperator OPERATOR_AND_TEXT = new LogicalAndOperator("AND", 2);
    public static final ExpressionOperator OPERATOR_OR_SYMBOL = new LogicalOrOperator("|", 1);
    public static final ExpressionOperator OPERATOR_OR_TEXT = new LogicalOrOperator("OR", 1);
    public static final ExpressionOperator OPERATOR_XOR_TEXT = new LogicalXOrOperator("XOR", 2);
    public static final ExpressionOperator OPERATOR_NUMERIC_EQUALS = new EqualityPredicateOperator("==", 5);
    public static final ExpressionOperator OPERATOR_NUMERIC_EQUALS_TEXT = new EqualityPredicateOperator("EQUALS", 5);
    public static final ExpressionOperator OPERATOR_NUMERIC_UNEQUALS = new InequalityPredicateOperator("!=", 5);
    public static final ExpressionOperator OPERATOR_NUMERIC_UNEQUALS_TEXT = new InequalityPredicateOperator("UNEQUAL", 5);
    public static final ExpressionOperator OPERATOR_NUMERIC_LESS_THAN = new NumericLessThanPredicateOperator(5);
    public static final ExpressionOperator OPERATOR_NUMERIC_GREATER_THAN = new NumericGreaterThanPredicateOperator(5);
    public static final ExpressionOperator OPERATOR_NUMERIC_LESS_THAN_OR_EQUAL = new NumericLessThanOrEqualPredicateOperator(5);
    public static final ExpressionOperator OPERATOR_NUMERIC_GREATER_THAN_OR_EQUAL = new NumericGreaterThanOrEqualPredicateOperator(5);
    public static final ExpressionOperator OPERATOR_NUMERIC_STRING_PLUS = new AdditionFunctionOperator(6);
    public static final ExpressionOperator OPERATOR_NUMERIC_MINUS = new SubtractionFunctionOperator(6);
    public static final ExpressionOperator OPERATOR_NUMERIC_MULTIPLY = new NumericMultiplyFunctionOperator(7);
    public static final ExpressionOperator OPERATOR_NUMERIC_DIVIDE = new NumericDivideFunctionOperator(7);
    public static final ExpressionOperator OPERATOR_NUMERIC_MODULO = new NumericModuloFunctionOperator(7);
    public static final ExpressionOperator OPERATOR_NUMERIC_EXPONENT = new NumericExponentFunctionOperator();
    public static final Operator OPERATOR_NUMERIC_NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 11);
    public static final Operator OPERATOR_NUMERIC_NEGATE_HIGH = new Operator("-", 1, Operator.Associativity.RIGHT, 12);
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS = new ContainsOperator(6);
    public static final ExpressionOperator OPERATOR_STRING_CONTAINS2 = new ContainsOperator2(6);
    public static final ExpressionOperator OPERATOR_VARIABLE_EXISTS = new VariableExistsOperator(7);
    public static final ExpressionOperator OPERATOR_VARIABLE_RESOLVE = new ResolveVariableOperator(10);
    public static final ExpressionOperator OPERATOR_ELEMENT_ACCESS_TEXT = new ElementAccessOperator("AT", 9);
    public static final ExpressionOperator OPERATOR_ELEMENT_ACCESS_SYMBOL = new ElementAccessOperator("@", 9);
    public static final ExpressionOperator OPERATOR_STATEMENT = new StatementOperator(-99999);

    public static final ExpressionOperator OPERATOR_PAIR = new PairOperator(-1000);

    private final Set<String> knownOperatorTokens = new HashSet<>();
    private final List<String> knownNonAlphanumericOperatorTokens = new ArrayList<>();

    public JIPipeExpressionEvaluator() {
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
        parameters.add(OPERATOR_PAIR);

        parameters.add(CONSTANT_NULL);
        parameters.add(CONSTANT_TRUE);
        parameters.add(CONSTANT_FALSE);
        parameters.add(CONSTANT_NEWLINE);
        parameters.add(CONSTANT_E);
        parameters.add(CONSTANT_PI);
        parameters.add(CONSTANT_TAU);
        parameters.add(CONSTANT_POSITIVE_INFINITY);
        parameters.add(CONSTANT_NEGATIVE_INFINITY);
        parameters.add(CONSTANT_NAN);

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
            variableName = "$\"" + JIPipeExpressionEvaluator.escapeString(variableName) + "\"";
        else {
            List<String> tokens = JIPipeExpressionParameter.getEvaluatorInstance().getKnownNonAlphanumericOperatorTokens();
            boolean processed = false;
            for (String token : tokens) {
                if (variableName.contains(token)) {
                    variableName = "$\"" + JIPipeExpressionEvaluator.escapeString(variableName) + "\"";
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                for (Operator operator : JIPipeExpressionParameter.getEvaluatorInstance().getOperators()) {
                    if (operator.getSymbol().equals(variableName)) {
                        variableName = "$\"" + JIPipeExpressionEvaluator.escapeString(variableName) + "\"";
                        processed = true;
                        break;
                    }
                }
            }
            if (!processed) {
                for (Constant constant : JIPipeExpressionParameter.getEvaluatorInstance().getConstants()) {
                    if (constant.getName().equals(variableName)) {
                        variableName = "$\"" + JIPipeExpressionEvaluator.escapeString(variableName) + "\"";
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

    public static String unescapeString(String escapedString) {
        escapedString = escapedString.trim();
        if (!escapedString.startsWith("\"")) {
            escapedString = "\"" + escapedString + "\"";
        }
        return org.hkijena.jipipe.utils.StringUtils.nullToEmpty(JIPipeExpressionParameter.getEvaluatorInstance().evaluate(escapedString, new ExpressionVariables()));
    }

    public List<String> tokenize(String expression, boolean includeQuotesAsToken, boolean includeQuotesIntoToken) {
        StringBuilder buffer = new StringBuilder();
        boolean isQuoted = false;
        boolean escape = false;
        int expressionEscape = 0;
        int arrayAccessBracketDepth = 0;
        AtomicBoolean resolveVariable = new AtomicBoolean(false);
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            // Expression escape behavior
            if (!isQuoted && !escape && c == '$' && i < expression.length() - 1 && expression.charAt(i + 1) == '{') {
                ++expressionEscape;
                i += 1; // Yes, this is ugly - we need to skip two steps
                if (expressionEscape == 1) {
                    buffer.setLength(0);
                }
                continue;
            }
            if(expressionEscape > 0 && c == '{') {
                ++expressionEscape;
            }
            if (expressionEscape > 0 && c == '}') {
                --expressionEscape;
                if (expressionEscape <= 0) {
                    // Convert to string and flush
                    String escaped = "\"" + escapeString(buffer.toString()) + "\"";
                    tokens.add(escaped);
                    buffer.setLength(0);
                }
                else {
                    buffer.append(c);
                }
                continue;
            }
            if (expressionEscape > 0) {
                buffer.append(c);
                continue;
            }

            // Standard behavior
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
            } else if (!isQuoted && c == '[') {
                flushBufferToToken(buffer, tokens, resolveVariable);
                tokens.add("@"); // Resolve as @ operator
                ++arrayAccessBracketDepth;
            } else if (!isQuoted && c == ']' && arrayAccessBracketDepth > 0) {
                --arrayAccessBracketDepth;
                continue; // Ignore token
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
            throw new JIPipeValidationRuntimeException(e,
                    "Error while evaluating expression " + expression,
                    "The expression could not be evaluated. Available variables are:\n\n" + expressionVariables.entrySet().stream()
                            .map(kv -> "‣ " + kv.getKey() + " \t\t= " + kv.getValue()).collect(Collectors.joining("\n\n")),
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
        int bracketsStack = 0;
        boolean canLiteral = true;
        List<String> tokens = tokenize(expression, true, true);
//        System.out.println(String.join(" | ", tokens));
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
                case "[":
                    ++bracketsStack;
                    canLiteral = true;
                    break;
                case "]":
                    if (bracketsStack <= 0)
                        return new IllegalArgumentException("Unmatched brackets!");
                    --bracketsStack;
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
        if (bracketsStack != 0)
            return new IllegalArgumentException("Unmatched brackets!");
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
                throw new JIPipeValidationRuntimeException(new NullPointerException(), "Unable to find variable '" + literal + "' in expression",
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
