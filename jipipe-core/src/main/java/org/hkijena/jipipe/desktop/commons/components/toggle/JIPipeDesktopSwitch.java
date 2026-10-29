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

package org.hkijena.jipipe.desktop.commons.components.toggle;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A compact on/off switch (iOS-style pill) for the desktop UI.
 * <p>
 * The #1339 parameter UX overhaul replaces the boolean parameter's
 * name-as-checkbox-text control with this: the parameter name comes from the
 * form panel's label column, and the control itself is a nameless toggle.
 * <p>
 * Plain {@link JToggleButton} semantics (space toggles, arrow keys, focus
 * border) are kept; only the painting is custom - a rounded track whose color
 * follows the selection state (off = theme border color, on = theme success
 * color) and a circular knob that slides left/right. State changes are
 * immediate (no animation).
 */
public class JIPipeDesktopSwitch extends JToggleButton {

    private static final int TRACK_WIDTH = 34;
    private static final int TRACK_HEIGHT = 19;

    public JIPipeDesktopSwitch() {
        initialize();
    }

    public JIPipeDesktopSwitch(String text) {
        super(text);
        initialize();
    }

    private void initialize() {
        setPreferredSize(new Dimension(TRACK_WIDTH, TRACK_HEIGHT));
        // A fixed-size control: must not shrink below the track. Without this,
        // a form-panel cell (GridBagLayout, fill=HORIZONTAL) may collapse the
        // component to 0x0 because JButton descendants report a 0x0 minimum.
        setMinimumSize(new Dimension(TRACK_WIDTH, TRACK_HEIGHT));
        setMargin(new Insets(0, 0, 0, 0));
        setFocusPainted(false);
        setRolloverEnabled(false);
        setContentAreaFilled(false);
        setBorder(BorderFactory.createEmptyBorder());
        // Blend into the row: the component background is the panel background;
        // the track is painted on top.
        setOpaque(true);
        addChangeListener(e -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Match the row background (parent's), so the switch blends in.
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        Color background = getParent() != null ? getParent().getBackground() : style.getPanelBackground();
        g2.setColor(background);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // The track is left-aligned so the compact pill sits at the start of
        // the control cell (next to the label column), matching the other
        // inline controls' left edge, and does not float in the middle when
        // the cell is stretched by fill=HORIZONTAL.
        int trackX = 0;
        int trackY = (getHeight() - TRACK_HEIGHT) / 2;
        boolean selected = isSelected();

        // Track
        Color trackColor = selected ? style.getPrimaryColor() : style.getBorderColor();
        g2.setColor(trackColor);
        g2.fillRoundRect(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

        // Focus ring (keyboard navigation)
        if (isFocusOwner()) {
            g2.setColor(style.getPrimaryColor());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(trackX - 2, trackY - 2, TRACK_WIDTH + 4, TRACK_HEIGHT + 4, TRACK_HEIGHT + 4, TRACK_HEIGHT + 4);
        }

        // Knob (slides right when selected)
        int knobSize = TRACK_HEIGHT - 4;
        int knobY = trackY + 2;
        int knobX = selected ? trackX + TRACK_WIDTH - knobSize - 2 : trackX + 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(knobX, knobY, knobSize, knobSize);

        g2.dispose();
    }
}
