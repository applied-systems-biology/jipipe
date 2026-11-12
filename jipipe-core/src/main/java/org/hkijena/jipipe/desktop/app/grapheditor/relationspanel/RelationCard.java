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

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.commons.nodepatterns.NodePatternRenderer;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * One node card of the Relations panel (MR 3 of the navigator overhaul,
 * #1358): a lightweight node representation that paints like a canvas node -
 * fill, framework signature pattern, border and a header band with icon and
 * name - but without slots.
 * <p>
 * The panel contributes exactly one visual factor over the resolved
 * {@link RelationCardAppearance.Appearance}: the card opacity (the
 * visitation trail ramp), applied to the whole card while painting. The 2px
 * border marks the current node (the trail's current / selected node), which
 * can sit anywhere in the displayed rows. The preferred size is MEASURED from
 * the header name and icon (repo policy: never assume text sizes). A single
 * click navigates to the card's node via the callback given by the panel.
 */
public class RelationCard extends JComponent {

    /**
     * Gap-only paddings around the header content (they do not encode a text
     * size).
     */
    static final int HEADER_HORIZONTAL_PADDING = 6;
    static final int HEADER_VERTICAL_PADDING = 3;
    static final int HEADER_ICON_TEXT_GAP = 4;

    /**
     * Border widths: 1px normally, 2px for the current card.
     */
    static final int BORDER_WIDTH = 1;
    static final int CURRENT_BORDER_WIDTH = 2;

    /**
     * The header band desaturation: the same HSB factors the canvas node uses
     * for its desaturated node colors (see
     * {@code JIPipeDesktopGraphNodeUI}).
     */
    private static final float HEADER_BAND_SATURATION_FACTOR = 0.25f;
    private static final float HEADER_BAND_BRIGHTNESS_FACTOR = 0.8f;

    private static final String ELLIPSIS = "...";

    private final String nodeId;
    private final String displayName;
    private final JIPipeNodeInfo info;
    private final RelationCardAppearance.Appearance appearance;
    private final Consumer<String> navigationCallback;
    private boolean current;
    private float opacity;

    /**
     * Creates a card for one node.
     *
     * @param nodeId             the canvas id of the node
     * @param displayName        the name shown in the header band (the panel
     *                           passes the node's canvas name)
     * @param info               the node type shown by the card
     * @param appearance         the resolved look (fill, border, pattern)
     * @param opacity            the current card opacity (the trail ramp
     *                           factor the panel resolved)
     * @param current            whether the card shows the current node
     *                           (drawn with the thicker border)
     * @param navigationCallback called with the node id when the card is
     *                           clicked
     */
    public RelationCard(String nodeId, String displayName, JIPipeNodeInfo info,
                        RelationCardAppearance.Appearance appearance, float opacity, boolean current,
                        Consumer<String> navigationCallback) {
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.info = info;
        this.appearance = appearance;
        this.opacity = opacity;
        this.current = current;
        this.navigationCallback = navigationCallback;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(displayName);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    navigationCallback.accept(RelationCard.this.nodeId);
                }
            }
        });
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the card opacity and repaints.
     *
     * @param opacity the new opacity (0 to 1)
     */
    public void setOpacity(float opacity) {
        this.opacity = opacity;
        repaint();
    }

    /**
     * Sets whether this card shows the current node (2px border) and
     * repaints.
     *
     * @param current the new current state
     */
    public void setCurrent(boolean current) {
        this.current = current;
        repaint();
    }

    /**
     * The preferred width is derived from the MEASURED header name and icon,
     * clamped to the panel's card minimum; the height is the panel's card
     * height (a layout minimum, grown when the measured header band needs
     * more).
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        Icon icon = resolvedIcon();
        return new Dimension(
                Math.max(JIPipeDesktopRelationsPanel.CARD_MIN_WIDTH,
                        HEADER_HORIZONTAL_PADDING + (icon != null ? icon.getIconWidth() : 0) + HEADER_ICON_TEXT_GAP
                                + metrics.stringWidth(displayName) + HEADER_HORIZONTAL_PADDING),
                Math.max(JIPipeDesktopRelationsPanel.CARD_HEIGHT, measuredHeaderHeight(metrics)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // The single visual factor the panel adds: the trail ramp opacity
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, opacity))));
            int width = getWidth();
            int height = getHeight();
            FontMetrics metrics = g2.getFontMetrics();
            int headerHeight = measuredHeaderHeight(metrics);
            int border = current ? CURRENT_BORDER_WIDTH : BORDER_WIDTH;

            // Fill over the whole card
            g2.setPaint(appearance.fill());
            g2.fillRect(0, 0, width, height);

            // Framework signature pattern over the body (below the header band)
            if (appearance.patternEnabled() && height > headerHeight) {
                NodePatternRenderer.paint(g2, 0, headerHeight, width, height - headerHeight,
                        appearance.patternStyleId(), appearance.patternSeed(),
                        appearance.patternInk(), appearance.patternOpacity());
            }

            // Header band: desaturated like the canvas' desaturated node colors
            g2.setPaint(ColorUtils.multiplyHSB(appearance.fill(),
                    1f, HEADER_BAND_SATURATION_FACTOR, HEADER_BAND_BRIGHTNESS_FACTOR));
            g2.fillRect(0, 0, width, Math.min(headerHeight, height));

            // Header content: icon + name (measured, never assumed)
            int x = HEADER_HORIZONTAL_PADDING;
            Icon icon = resolvedIcon();
            if (icon != null) {
                icon.paintIcon(this, g2, x, Math.max(0, (headerHeight - icon.getIconHeight()) / 2));
                x += icon.getIconWidth() + HEADER_ICON_TEXT_GAP;
            }
            int availableNameWidth = width - x - HEADER_HORIZONTAL_PADDING - border;
            g2.setPaint(nameColor());
            UIUtils.drawStringVerticallyCentered(g2, fittedName(metrics, availableNameWidth), x,
                    headerHeight / 2, metrics);

            // Border: 1px, 2px for the current card (painted inside)
            g2.setPaint(appearance.border());
            g2.fillRect(0, 0, width, border);
            g2.fillRect(0, height - border, width, border);
            g2.fillRect(0, 0, border, height);
            g2.fillRect(width - border, 0, border, height);
        } finally {
            g2.dispose();
        }
    }

    /**
     * The header band height: the measured text height or the icon height,
     * whichever is larger, plus the vertical paddings.
     */
    private int measuredHeaderHeight(FontMetrics metrics) {
        Icon icon = resolvedIcon();
        return Math.max(icon != null ? icon.getIconHeight() : 0, metrics.getHeight())
                + 2 * HEADER_VERTICAL_PADDING;
    }

    /**
     * The node icon with a defensive fallback (the node database resolves an
     * icon for every registered node).
     */
    private Icon resolvedIcon() {
        Icon icon = info.getIcon();
        if (icon != null) {
            return icon;
        }
        return JIPipe.RESOURCES.getIcon16("actions/tag.png");
    }

    /**
     * The header name color: the same colors the canvas node uses for its
     * displayed name.
     */
    private Color nameColor() {
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        if (info.isUnstable()) {
            return style != null ? style.getDangerColor() : Color.RED;
        }
        if (info.isDeprecated()) {
            return style != null ? style.getWarningColor() : Color.RED.darker();
        }
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.BLACK;
    }

    /**
     * The header name fitted into the available width with an ellipsis (a
     * defensive trim: the preferred width already measures the full name).
     */
    private String fittedName(FontMetrics metrics, int availableWidth) {
        if (availableWidth <= 0 || metrics.stringWidth(displayName) <= availableWidth) {
            return displayName;
        }
        for (int end = displayName.length(); end >= 0; --end) {
            String candidate = displayName.substring(0, end) + ELLIPSIS;
            if (metrics.stringWidth(candidate) <= availableWidth) {
                return candidate;
            }
        }
        return "";
    }
}
