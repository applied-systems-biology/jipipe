package org.hkijena.jipipe.plugins.expressions;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

public class JIPipeExpressionCustomASTParser {

    private final Map<String, Integer> operatorPrecedenceMap = new HashMap<>();
    private final Set<String> functions = new HashSet<>();

    public JIPipeExpressionCustomASTParser() {

    }

    public JIPipeExpressionCustomASTParser addOperator(String operator, int precedence) {
        operatorPrecedenceMap.put(operator, precedence);
        return this;
    }

    public JIPipeExpressionCustomASTParser addFunctions(String... functions) {
        Collections.addAll(this.functions, functions);
        return this;
    }

    public ASTNode parse(List<String> tokens) {
        List<Token> tokenList = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            if ("(".equals(token) || ")".equals(token)) {
                tokenList.add(new Token(TokenType.PARENTHESIS, token));
            } else if (token.equals(",")) {
                tokenList.add(new Token(TokenType.COMMA, token));
            } else if (operatorPrecedenceMap.containsKey(token)) {
                tokenList.add(new Token(TokenType.OPERATOR, token));
            } else if (functions.contains(token)) {
                tokenList.add(new Token(TokenType.FUNCTION, token));
            } else if (NumberUtils.isCreatable(token)) {
                tokenList.add(new Token(TokenType.NUMBER, token));
            } else {
                tokenList.add(new Token(TokenType.VARIABLE, token));
            }
        }
        return parse(new TokenIterator(tokenList));
    }

    public ASTNode parse(TokenIterator tokens) {
        return parseExpression(tokens, 0);
    }

    private ASTNode parseExpression(TokenIterator tokens, int minPrecedence) {
        ASTNode left = parsePrimary(tokens);

        while (tokens.hasNext() && isOperator(tokens.current().value) && operatorPrecedenceMap.get(tokens.current().value) >= minPrecedence) {
            String operator = tokens.current().value;
            int precedence = operatorPrecedenceMap.get(operator);
            tokens.next();
            ASTNode right = parseExpression(tokens, precedence + 1);
            left = new OperationNode(left, right, operator);
        }

        return left;
    }

    private ASTNode parsePrimary(TokenIterator tokens) {
        Token token = tokens.current();
        tokens.next();

        switch (token.type) {
            case VARIABLE:
                return new VariableNode(token.value);
            case NUMBER:
                return new NumberNode(StringUtils.parseDouble(token.value));
            case FUNCTION:
                return parseFunction(tokens, token.value);
            case PARENTHESIS:
                if (token.value.equals("(")) {
                    ASTNode node = parseExpression(tokens, 0);
                    tokens.next(); // skip ')'
                    return node;
                }
            default:
                throw new IllegalArgumentException("Unexpected token: " + token.value);
        }
    }

    private ASTNode parseFunction(TokenIterator tokens, String functionName) {
        List<ASTNode> arguments = new ArrayList<>();
        tokens.next(); // skip '('
        while (!tokens.current().value.equals(")")) {
            arguments.add(parseExpression(tokens, 0));
            if (tokens.current().value.equals(",")) {
                tokens.next(); // skip ','
            }
        }
        tokens.next(); // skip ')'
        return new FunctionNode(functionName, arguments);
    }

    private boolean isOperator(String value) {
        return operatorPrecedenceMap.containsKey(value);
    }

    public enum TokenType {NUMBER, VARIABLE, OPERATOR, FUNCTION, PARENTHESIS, COMMA}

    public interface ASTNode {
    }

    public static class TokenIterator {
        private final List<Token> tokens;
        private int currentIndex;

        public TokenIterator(List<Token> tokens) {
            this.tokens = tokens;
            this.currentIndex = 0;
        }

        public boolean hasNext() {
            return currentIndex < tokens.size();
        }

        public Token current() {
            if (currentIndex < tokens.size()) {
                return tokens.get(currentIndex);
            }
            return null;
        }

        public void next() {
            currentIndex++;
        }

        public Token peekNext() {
            if (currentIndex + 1 < tokens.size()) {
                return tokens.get(currentIndex + 1);
            }
            return null;
        }
    }

    public static class Token {

        public TokenType type;
        public String value;

        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public static class NumberNode implements ASTNode {
        private final double value;

        public NumberNode(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    public static class VariableNode implements ASTNode {
        private final String name;

        public VariableNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class OperationNode implements ASTNode {
        private final ASTNode left;
        private final ASTNode right;
        private final String operator;

        public OperationNode(ASTNode left, ASTNode right, String operator) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        public ASTNode getLeft() {
            return left;
        }

        public ASTNode getRight() {
            return right;
        }

        public String getOperator() {
            return operator;
        }
    }

    public static class FunctionNode implements ASTNode {
        private final String functionName;
        private final List<ASTNode> arguments;

        public FunctionNode(String functionName, List<ASTNode> arguments) {
            this.functionName = functionName;
            this.arguments = arguments;
        }

        public String getFunctionName() {
            return functionName;
        }

        public List<ASTNode> getArguments() {
            return arguments;
        }
    }
}
