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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDropShadowRenderer;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;

/**
 * Constants used by JIPipeDesktopGraphCanvasUI
 */
public final class JIPipeDesktopGraphCanvasUIConstants {

    public static final JIPipeDesktopDropShadowRenderer DROP_SHADOW_BORDER = new JIPipeDesktopDropShadowRenderer(Color.BLACK,
            5,
            0.3f,
            12,
            true,
            true,
            true,
            true);
    public static final JIPipeDesktopDropShadowRenderer BOOKMARK_SHADOW_BORDER = new JIPipeDesktopDropShadowRenderer(new Color(0x33cc33),
            12,
            0.3f,
            12,
            true,
            true,
            true,
            true);

    public static final Font GRAPH_TOOL_CURSOR_FONT = new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal());
    public static final Color COLOR_HIGHLIGHT_GREEN = new Color(0, 128, 0);
    public static final Stroke STROKE_UNIT = new BasicStroke(1);
    public static final Stroke STROKE_THICK = new BasicStroke(3);
    public static final Stroke STROKE_UNIT_COMMENT = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    public static final Stroke STROKE_SELECTION = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0);
    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT_HIGHLIGHT = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{8}, 0);
    public static final Stroke STROKE_SMART_EDGE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Color COLOR_RESIZE_HANDLE_FILL = new Color(0x22A02D);
    public static final Color COLOR_RESIZE_HANDLE_BORDER = new Color(0x22A02D).darker();
    public static final int RESIZE_HANDLE_DISTANCE = 12;
    public static final int RESIZE_HANDLE_SIZE = 10;
    public static final Color COMMENT_EDGE_COLOR = new Color(194, 141, 0);
    public static final Color COLOR_EDGE_DEFAULT = ThemeUtils.isUsingDarkTheme() ? new Color(0x3E3E3E) : new Color(0x737880);
    public static final Color COLOR_EDGE_CONVERT = new Color(0x2957C2);

    private JIPipeDesktopGraphCanvasUIConstants() {
        // Prevent instantiation
    }
}