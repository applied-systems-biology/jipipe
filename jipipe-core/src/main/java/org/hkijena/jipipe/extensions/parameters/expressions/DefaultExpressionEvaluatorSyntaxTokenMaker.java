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

/**
 * Generates tokens for an {@link ExpressionEvaluator} instance.
 */
public class DefaultExpressionEvaluatorSyntaxTokenMaker extends AbstractTokenMaker {

    public DefaultExpressionEvaluatorSyntaxTokenMaker() {
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();
        for (Function function : DefaultExpressionParameter.EVALUATOR.getFunctions()) {
            tokenMap.put(function.getName(), Token.FUNCTION);
        }
        for (Operator operator : DefaultExpressionParameter.EVALUATOR.getOperators()) {
            tokenMap.put(operator.getSymbol(), Token.OPERATOR);
        }
        return tokenMap;
    }

    @Override
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
        resetTokenList();
        int offset = text.offset;
        int newStartOffset = startOffset - offset;

        String expression = text.toString();

        StringBuilder builder = new StringBuilder();
        int builderStart = 0;
        boolean isQuote = false;
        boolean isEscape = false;
        for (int index = 0; index < expression.length(); index++) {
            char c = expression.charAt(index);
            if(c == '\\') {
                isEscape = !isEscape;
            }
            if(c == '"') {
                if(!isEscape) {
                    isQuote = !isQuote;
                    if(!isQuote) {
                        // Flush the builder
                        builder.append(c);
                        addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset + offset);
                        builderStart = index + 1;
                        builder.setLength(0);
                        continue;
                    }
                }
            }
            if(c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset + offset);
                builder.setLength(0);
                builderStart = index + 1;
                addToken(text, index + offset, index + offset, Token.WHITESPACE, newStartOffset + index + offset);
                continue;
            }
            if(c == '(' || c == ')') {
                addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset);
                builder.setLength(0);
                builderStart = index + 1;
                addToken(text, index + offset, index + offset, Token.SEPARATOR, newStartOffset + index + offset);
                continue;
            }
            builder.append(c);
        }
        if(builder.length() > 0) {
            addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset);
        }
        if(firstToken == null) {
            addToken(text, 0, text.count, Token.NULL, newStartOffset);
        }
        return firstToken;
    }

    private void addTokenFromBuilder(Segment segment, String text, int index, int offset, int startOffset) {
        if(text.isEmpty())
            return;
        int tokenType = getWordsToHighlight().get(segment, index + offset, index + offset + text.length() - 1);
        if(text.startsWith("\""))
            tokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
        if(tokenType == -1)
            tokenType = Token.IDENTIFIER;
        for (int i = 0; i < text.length(); i++) {
            addToken(segment, index + offset + i, index + offset + i, tokenType, startOffset + index + offset + i);
        }
    }
}
