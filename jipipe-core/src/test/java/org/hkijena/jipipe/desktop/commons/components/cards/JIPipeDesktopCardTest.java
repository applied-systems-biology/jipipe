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

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopCardTest {

    @Test
    void createWithTitleAndIcon() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test Card", UIManager.getIcon("Tree.leafIcon"));
        assertEquals("Test Card", card.getTitle());
        assertNotNull(card.getIcon());
    }

    @Test
    void setBody() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JLabel body = new JLabel("Body content");
        card.setBody(body);
        assertSame(body, card.getBody());
    }

    @Test
    void setVariant() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        card.setVariant(JIPipeDesktopCardVariant.Primary);
        assertEquals(JIPipeDesktopCardVariant.Primary, card.getVariant());
    }

    @Test
    void addHeaderAction() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JButton button = new JButton("Action");
        card.addHeaderAction(button);
        assertEquals(1, card.getHeaderActionCount());
    }

    @Test
    void addFooterAction() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JButton button = new JButton("Action");
        card.addFooterAction(button);
        assertEquals(1, card.getFooterActionCount());
    }

    @Test
    void builderPattern() {
        JButton footerBtn = new JButton("OK");
        JIPipeDesktopCard card = JIPipeDesktopCard.builder("Built Card")
                .icon(UIManager.getIcon("Tree.leafIcon"))
                .variant(JIPipeDesktopCardVariant.Success)
                .body(new JLabel("Body"))
                .footerButton(footerBtn)
                .build();
        assertEquals("Built Card", card.getTitle());
        assertEquals(JIPipeDesktopCardVariant.Success, card.getVariant());
        assertNotNull(card.getBody());
        assertEquals(1, card.getFooterActionCount());
    }

    @Test
    void noHeaderWhenTitleAndIconNull() {
        JIPipeDesktopCard card = new JIPipeDesktopCard();
        card.setBody(new JLabel("Body"));
        assertFalse(card.hasHeader());
    }

    @Test
    void hasHeaderWhenTitleSet() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Title");
        assertTrue(card.hasHeader());
    }
}
