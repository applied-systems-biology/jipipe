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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tab pill's padding border is order-independent:
 * {@link JIPipeDesktopTabPane#setTabPanelBorder} must apply to tabs created
 * BEFORE the call, not only to tabs created after it. A tab component
 * captures the pane's border at construction; the historical bug was that a
 * caller (the node finder's detail rail) added its tabs before setting the
 * border, so the pills silently rendered with the thin default padding
 * (4,0,4,0 — zero horizontal padding, pills touching) while the AI monitor
 * — the design reference — set the border first and looked correct.
 */
class JIPipeDesktopTabPaneBorderTest {

    private static final Border PILL_BORDER = BorderFactory.createEmptyBorder(12, 4, 12, 4);
    private static JIPipeDesktopTabPane borderBeforePane;
    private static JIPipeDesktopTabPane borderAfterPane;

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
            }
            JIPipe.ensureInstance();
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT/JIPipe not available: " + t);
        }

        // The correct order (the AI monitor): border first, tabs after.
        borderBeforePane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
        borderBeforePane.setTabPanelBorder(PILL_BORDER);
        borderBeforePane.addTab("A", JIPipe.RESOURCES.getIcon16("actions/tag.png"),
            new JPanel(), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // The historical bug order (the finder rail before the fix): tabs
        // first, border after — the pills must STILL end up with the border.
        borderAfterPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
        borderAfterPane.addTab("A", JIPipe.RESOURCES.getIcon16("actions/tag.png"),
            new JPanel(), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        borderAfterPane.addTab("B", JIPipe.RESOURCES.getIcon16("actions/tag.png"),
            new JPanel(), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        borderAfterPane.setTabPanelBorder(PILL_BORDER);
    }

    @Test
    void borderSetBeforeTabCreationAppliesToTheTabs() {
        for (int i = 0; i < borderBeforePane.getTabCount(); i++) {
            assertThat(pillAt(borderBeforePane, i).getBorder())
                .as("tab %d created after setTabPanelBorder", i)
                .isSameAs(PILL_BORDER);
        }
    }

    @Test
    void borderSetAfterTabCreationIsPropagatedToExistingTabs() {
        for (int i = 0; i < borderAfterPane.getTabCount(); i++) {
            assertThat(pillAt(borderAfterPane, i).getBorder())
                .as("tab %d created BEFORE setTabPanelBorder", i)
                .isSameAs(PILL_BORDER);
        }
    }

    @Test
    void aLaterBorderChangeReplacesThePreviousOne() {
        Border other = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        borderAfterPane.setTabPanelBorder(other);
        for (int i = 0; i < borderAfterPane.getTabCount(); i++) {
            assertThat(pillAt(borderAfterPane, i).getBorder()).isSameAs(other);
        }
        borderAfterPane.setTabPanelBorder(PILL_BORDER);
    }

    private static JComponent pillAt(JIPipeDesktopTabPane pane, int index) {
        Component component = pane.getTabbedPane().getTabComponentAt(index);
        assertThat(component).isNotNull();
        return (JComponent) component;
    }
}
