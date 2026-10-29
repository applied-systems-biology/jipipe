/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components.cards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The card's header visibility rule: a header is present only when the card
 * has a title/icon or a header action (the computed {@code headerVisible},
 * {@code hasTitleOrIcon || hasActions}). The simple set/getter behavior of
 * the title, variant, body and action slots is not tested (the "do not test
 * trivial things" policy) - the user verifies the rendered cards in the app.
 */
class JIPipeDesktopCardTest {

    @Test
    void noHeaderWhenTitleAndIconNull() {
        JIPipeDesktopCard card = new JIPipeDesktopCard();
        card.setBody(new javax.swing.JLabel("Body"));
        assertFalse(card.hasHeader());
    }

    @Test
    void hasHeaderWhenTitleSet() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Title");
        assertTrue(card.hasHeader());
    }
}
