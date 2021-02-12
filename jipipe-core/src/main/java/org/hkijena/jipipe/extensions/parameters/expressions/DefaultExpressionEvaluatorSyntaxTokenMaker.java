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

import com.fathzer.soft.javaluator.Constant;
import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import org.apache.commons.lang3.math.NumberUtils;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates tokens for an {@link ExpressionEvaluator} instance.
 */
public class DefaultExpressionEvaluatorSyntaxTokenMaker extends AbstractTokenMaker {

    private final List<String> knownNonAlphanumericOperatorTokens;
    private Set<String> operators;

    public DefaultExpressionEvaluatorSyntaxTokenMaker() {
        knownNonAlphanumericOperatorTokens = DefaultExpressionParameter.getEvaluatorInstance().getKnownNonAlphanumericOperatorTokens();
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();
        for (Constant constant : DefaultExpressionParameter.getEvaluatorInstance().getConstants()) {
            tokenMap.put(constant.getName(), Token.RESERVED_WORD);
        }
        for (Function function : DefaultExpressionParameter.getEvaluatorInstance().getFunctions()) {
            tokenMap.put(function.getName(), Token.FUNCTION);
        }
        operators = new HashSet<>();
        for (Operator operator : DefaultExpressionParameter.getEvaluatorInstance().getOperators()) {
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
        int count = text.count;
        int end = offset + count;
        char[] array = text.array;

        StringBuilder buffer = new StringBuilder();
        boolean isQuoted = false;
        boolean isEscape = false;
        int lastEscapeIndex = -1;

        int currentTokenStart = offset;

        for (int index = offset; index < end; index++) {
            char c = array[index];
            if (isEscape && index == lastEscapeIndex + 2) {
                isEscape = false;
            }
            if (c == '\\' && !isEscape) {
                isEscape = true;
                lastEscapeIndex = index;
            }
            if (c == '"') {
                if (!isEscape) {
                    isQuoted = !isQuoted;
                    if (!isQuoted) {
                        // Flush the builder
                        buffer.append(c);
                        addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
                        currentTokenStart = index + 1;
                        buffer.setLength(0);
                        continue;
                    }
                }
            }
            if (!isQuoted && (c == ' ' || c == '\t' || c == '\r' || c == '\n')) {
                addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
                buffer.setLength(0);
                currentTokenStart = index;
                addToken(text, index, index, Token.WHITESPACE, newStartOffset + currentTokenStart);
                currentTokenStart = index + 1;
                continue;
            }
            if (!isQuoted && (c == '(' || c == ')' || c == ',')) {
                addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
                buffer.setLength(0);
                currentTokenStart = index;
                addToken(text, index, index, Token.SEPARATOR, newStartOffset + currentTokenStart);
                currentTokenStart = index + 1;
                continue;
            }
            buffer.append(c);
//            if(!isQuote && operators.contains(builder.toString())) {
//                // Builder is operator
//                addTokenFromBuilder(text, builder.toString(), builderStart, offset, newStartOffset);
//                builder.setLength(0);
//                builderStart = index + 1;
//            }
            if (!isQuoted && buffer.length() > 0) {
                String s1 = buffer.toString();
                if (index != end - 1) {
                    // Workaround <= >=
                    if (s1.endsWith("<") || s1.endsWith(">")) {
                        char next = array[index + 1];
                        if (next == '=')
                            continue;
                    }
                    // Workaround !=
                    if (s1.endsWith("!")) {
                        char next = array[index + 1];
                        if (next == '=')
                            continue;
                    }
                }
                for (String s : knownNonAlphanumericOperatorTokens) {
                    int i1 = s1.indexOf(s);
                    if (i1 != -1) {
                        if (i1 > 0) {
                            addToken(text, s1.substring(0, i1), currentTokenStart, newStartOffset + currentTokenStart);
                        }
                        addToken(text, s, currentTokenStart + i1, newStartOffset + currentTokenStart);
                        buffer.setLength(0);
                        currentTokenStart = index + 1;
                        break;
                    }
                }
            }
        }
        if (buffer.length() > 0) {
            addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
        }
        if (firstToken == null) {
            addNullToken();
        }
        return firstToken;
    }

    private void addToken(Segment segment, String text, int start, int startOffset) {
        if (text.isEmpty())
            return;
        int end = start + text.length() - 1;
        int tokenType = getWordsToHighlight().get(segment, start, end);
        if (text.startsWith("\""))
            tokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
        else if (NumberUtils.isCreatable(text))
            tokenType = Token.LITERAL_NUMBER_FLOAT;
        if (tokenType == -1)
            tokenType = Token.VARIABLE;
        int shift = 0;
        for (int i = start; i <= end; i++) {
            addToken(segment, i, i, tokenType, startOffset + shift++);
        }
    }

//    @Override
//    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
//        System.out.println("ato " + start + "-" + end + " @ " + startOffset);
//        super.addToken(segment, start, end, tokenType, startOffset);
//    }
}
