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

import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Generates tokens for an {@link ExpressionEvaluator} instance.
 */
public class DefaultExpressionEvaluatorSyntaxTokenMaker extends AbstractTokenMaker {

    private Set<ExpressionParameterVariable> dynamicVariables;
    private Set<String> operators;
    private final List<String> knownNonAlphanumericOperatorTokens;

    public DefaultExpressionEvaluatorSyntaxTokenMaker() {
        knownNonAlphanumericOperatorTokens = DefaultExpressionParameter.EVALUATOR.getKnownNonAlphanumericOperatorTokens();
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();
        tokenMap.put("TRUE", Token.LITERAL_BOOLEAN);
        tokenMap.put("FALSE", Token.LITERAL_BOOLEAN);
        for (Function function : DefaultExpressionParameter.EVALUATOR.getFunctions()) {
            tokenMap.put(function.getName(), Token.FUNCTION);
        }
        operators = new HashSet<>();
        for (Operator operator : DefaultExpressionParameter.EVALUATOR.getOperators()) {
            tokenMap.put(operator.getSymbol(), Token.OPERATOR);
            operators.add(operator.getSymbol());
        }
        return tokenMap;
    }

    @Override
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
        resetTokenList();
        int offset = text.offset;
        int newStartOffset = startOffset - offset;

        String expression = text.toString();

        StringBuilder buffer = new StringBuilder();
        int bufferStart = 0;
        boolean isQuoted = false;
        boolean isEscape = false;
        for (int index = 0; index < expression.length(); index++) {
            char c = expression.charAt(index);
            if(c == '\\') {
                isEscape = !isEscape;
            }
            if(c == '"') {
                if(!isEscape) {
                    isQuoted = !isQuoted;
                    if(!isQuoted) {
                        // Flush the builder
                        buffer.append(c);
                        addToken(text, buffer.toString(), bufferStart, offset, newStartOffset + offset);
                        bufferStart = index + 1;
                        buffer.setLength(0);
                        continue;
                    }
                }
            }
            if(!isQuoted && (c == ' ' || c == '\t' || c == '\r' || c == '\n')) {
                addToken(text, buffer.toString(), bufferStart, offset, newStartOffset + offset);
                buffer.setLength(0);
                bufferStart = index + 1;
                addToken(text, index + offset, index + offset, Token.WHITESPACE, newStartOffset + index + offset);
                continue;
            }
            if(!isQuoted && (c == '(' || c == ')')) {
                addToken(text, buffer.toString(), bufferStart, offset, newStartOffset);
                buffer.setLength(0);
                bufferStart = index + 1;
                addToken(text, index + offset, index + offset, Token.SEPARATOR, newStartOffset + index + offset);
                continue;
            }
            buffer.append(c);
//            if(!isQuote && operators.contains(builder.toString())) {
//                // Builder is operator
//                addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset);
//                builder.setLength(0);
//                builderStart = index + 1;
//            }
            if(!isQuoted && buffer.length() > 0) {
                String s1 = buffer.toString();
                if(index != expression.length() - 1) {
                    // Workaround <= >=
                    if(s1.endsWith("<") || s1.endsWith(">")) {
                        char next = expression.charAt(index + 1);
                        if(next == '=')
                            continue;
                    }
                    // Workaround !=
                    if(s1.endsWith("!")) {
                        char next = expression.charAt(index + 1);
                        if(next == '=')
                            continue;
                    }
                }
                for (String s : knownNonAlphanumericOperatorTokens) {
                    int i1 = s1.indexOf(s);
                    if(i1 != -1) {
                        if(i1 > 0) {
                            addToken(text, s1.substring(0, i1), bufferStart, offset, newStartOffset);
                        }
                        addToken(text, s, bufferStart + i1, offset, newStartOffset);
                        buffer.setLength(0);
                        bufferStart = index + 1;
                        break;
                    }
                }
            }
        }
        if(buffer.length() > 0) {
            addToken(text, buffer.toString(), bufferStart, offset, newStartOffset);
        }
        if(firstToken == null) {
            addToken(text, 0, text.count, Token.NULL, newStartOffset);
        }
        return firstToken;
    }

    private void addToken(Segment segment, String text, int index, int offset, int startOffset) {
//        System.out.println("Add " + text + " @ " + index + ":" + (index + text.length()));
        if(text.isEmpty())
            return;
        int tokenType = getWordsToHighlight().get(segment, index + offset, index + offset + text.length() - 1);
        if(text.startsWith("\""))
            tokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
        if(dynamicVariables != null && dynamicVariables.stream().anyMatch(v -> Objects.equals(v.getKey(), text)))
            tokenType = Token.VARIABLE;
        if(tokenType == -1)
            tokenType = Token.IDENTIFIER;
        for (int i = 0; i < text.length(); i++) {
            addToken(segment, index + offset + i, index + offset + i, tokenType, startOffset + index + offset + i);
        }
    }

    public void setDynamicVariables(Set<ExpressionParameterVariable> variables) {
        this.dynamicVariables = variables;
    }

    public Set<ExpressionParameterVariable> getDynamicVariables() {
        return dynamicVariables;
    }
}
