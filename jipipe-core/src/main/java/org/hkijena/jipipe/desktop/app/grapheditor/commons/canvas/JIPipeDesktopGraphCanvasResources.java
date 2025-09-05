package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDropShadowRenderer;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphCanvasResources {
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
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    private final Color improvedStrokeBackgroundColor = UIManager.getColor("Panel.background");
    private final Color smartEdgeSlotBackground = UIManager.getColor("EditorPane.background");
    private final Color smartEdgeSlotForeground = UIManager.getColor("Label.foreground");
    private final ImageIcon lockIcon = JIPipe.RESOURCES.getIcon16Inverted("actions/lock.png");
    private final ImageIcon cursorImage = JIPipe.RESOURCES.getIcon16("actions/target.png");

    private Font smartEdgeTooltipSlotFont;
    private Font smartEdgeTooltipNodeFont;

    public JIPipeDesktopGraphCanvasResources(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public Color getImprovedStrokeBackgroundColor() {
        return improvedStrokeBackgroundColor;
    }

    public Color getSmartEdgeSlotBackground() {
        return smartEdgeSlotBackground;
    }

    public Color getSmartEdgeSlotForeground() {
        return smartEdgeSlotForeground;
    }

    public ImageIcon getLockIcon() {
        return lockIcon;
    }

    public Font getSmartEdgeTooltipSlotFont() {
        return smartEdgeTooltipSlotFont;
    }

    public Font getSmartEdgeTooltipNodeFont() {
        return smartEdgeTooltipNodeFont;
    }

    public Paint getEdgeBackgroundPaint(JIPipeDataSlot source, JIPipeDataSlot target, PointRange sourcePoint, PointRange targetPoint, Color defaultPaint) {
        Paint strokePaint = defaultPaint;

        if (canvasUI.getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            if (source.getNode() instanceof JIPipeAlgorithm sourceAlgorithm && target.getNode() instanceof JIPipeAlgorithm targetAlgorithm) {
                JIPipeRuntimePartitionConfiguration runtimePartitions = canvasUI.getDesktopWorkbench().getProject().getRuntimePartitions();
                JIPipeRuntimePartition sourcePartition = runtimePartitions.get(sourceAlgorithm.getRuntimePartition().getIndex());
                JIPipeRuntimePartition targetPartition = runtimePartitions.get(targetAlgorithm.getRuntimePartition().getIndex());
                if (sourcePartition != targetPartition) {
                    try {
                        strokePaint = new LinearGradientPaint(sourcePoint.center.x, sourcePoint.center.y,
                                targetPoint.center.x, targetPoint.center.y,
                                new float[]{0f, 1f},
                                new Color[]{sourcePartition.getColor().getContentOrDefault(defaultPaint), targetPartition.getColor().getContentOrDefault(defaultPaint)});
                    } catch (Throwable throwable) {
                        strokePaint = sourcePartition.getColor().getContentOrDefault(defaultPaint);
                    }
                } else {
                    strokePaint = sourcePartition.getColor().getContentOrDefault(defaultPaint);
                }
            }
        }
        return strokePaint;
    }

    public int getArrowHeadShift() {
        if (canvasUI.getSettings().isDrawArrowHeads()) {
            int sz = 1;
            return -2 * sz - 6;
        } else {
            return 0;
        }
    }

    public Stroke getStrokeHighlight() {
        int width = (int) Math.max(1, canvasUI.getZoom() * 8);
        return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
    }

    public Stroke getStrokeDefault() {
        int width = (int) Math.max(1, canvasUI.getZoom() * 4);
        return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
    }

    public Stroke getStrokeDefaultBorder() {
        int width = (int) Math.max(1, canvasUI.getZoom() * 4) + 2;
        return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
    }

    public Color getEdgeColor(JIPipeDataSlot source, JIPipeDataSlot target, boolean multicolor, int multiColorIndex, int multiColorMax) {
        Color result;
        if (source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode) {
            result = COMMENT_EDGE_COLOR;
        } else if (multicolor) {
            result = Color.getHSBColor(1.0f * multiColorIndex / multiColorMax, 0.45f, 0.65f);
        } else {
            if (JIPipeDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = COLOR_EDGE_DEFAULT;
            else if (JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = COLOR_EDGE_CONVERT;
            else
                result = Color.RED;
        }
        return result;
    }

    public void updateAssets() {
        smartEdgeTooltipSlotFont = new Font(Font.DIALOG, Font.BOLD, Math.max(1, (int) Math.round(14 * canvasUI.getZoom())));
        smartEdgeTooltipNodeFont = new Font(Font.DIALOG, Font.PLAIN, Math.max(1, (int) Math.round(9 * canvasUI.getZoom())));
    }

    public ImageIcon getCursorImage() {
        return cursorImage;
    }
}
