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

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;

/**
 * Generates tokens for an {@link ExpressionEvaluator} instance.
 */
public class ExpressionEvaluatorSyntaxTokenMaker extends AbstractTokenMaker {
    private final ExpressionEvaluator evaluator;

    public ExpressionEvaluatorSyntaxTokenMaker(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public TokenMap getWordsToHighlight() {
        return null;
    }

    @Override
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
        resetTokenList();
        return null;
    }
}
