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

package org.hkijena.jipipe.desktop.commons.components.layouts;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopColumnLayoutTest {

    private JPanel createContainer(int width, int gutter) {
        JPanel container = new JPanel();
        container.setLayout(new JIPipeDesktopColumnLayout(gutter));
        container.setSize(width, 600);
        container.setPreferredSize(new Dimension(width, 600));
        container.doLayout();
        return container;
    }

    private Component addLabel(JPanel container, JIPipeDesktopColumnConstraints constraints) {
        JLabel label = new JLabel("X");
        label.setPreferredSize(new Dimension(50, 30));
        label.setMinimumSize(new Dimension(50, 30));
        container.add(label, constraints);
        return label;
    }

    @Test
    void singleFullWidthItem() {
        JPanel container = createContainer(1000, 16);
        Component c = addLabel(container, new JIPipeDesktopColumnConstraints(12));
        container.doLayout();
        assertEquals(1000 - 16, c.getWidth());
    }

    @Test
    void twoHalfWidthItems() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(6));
        Component c2 = addLabel(container, new JIPipeDesktopColumnConstraints(6));
        container.doLayout();
        assertTrue(c1.getX() < c2.getX());
        assertEquals(c1.getWidth(), c2.getWidth(), 1);
    }

    @Test
    void wrappingWhenSpanExceeds12() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(8));
        Component c2 = addLabel(container, new JIPipeDesktopColumnConstraints(8));
        container.doLayout();
        assertTrue(c2.getY() > c1.getY(), "Second item should wrap to next row");
    }

    @Test
    void offsetPushesItemRight() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(6).withOffset(3));
        container.doLayout();
        assertTrue(c1.getX() > 100, "Offset item should start further right");
    }

    @Test
    void breakpointChangesSpan() {
        JPanel container = createContainer(600, 16);
        Component c = addLabel(container, new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.SM, 12));
        container.doLayout();
        assertEquals(600 - 16, c.getWidth(), "At 600px width (SM breakpoint), span should be 12");
    }

    @Test
    void defaultConstraintsWhenNoneProvided() {
        JPanel container = createContainer(1000, 16);
        JLabel label = new JLabel("X");
        label.setPreferredSize(new Dimension(50, 30));
        container.add(label);
        container.doLayout();
        assertEquals(1000 - 16, label.getWidth(), "Default should be full width (12 columns)");
    }

    @Test
    void preferredLayoutSizeComputesHeight() {
        JPanel container = new JPanel();
        container.setLayout(new JIPipeDesktopColumnLayout(16));
        container.setSize(1000, 600);
        addLabel(container, new JIPipeDesktopColumnConstraints(6));
        addLabel(container, new JIPipeDesktopColumnConstraints(6));
        addLabel(container, new JIPipeDesktopColumnConstraints(12));
        Dimension pref = container.getLayout().preferredLayoutSize(container);
        assertTrue(pref.height > 0, "Preferred height should be positive");
        assertTrue(pref.width > 0, "Preferred width should be positive");
    }
}
