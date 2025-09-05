package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.utils.PointRange;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphCanvasResources {
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
            result = JIPipeDesktopGraphCanvasUIConstants.COMMENT_EDGE_COLOR;
        } else if (multicolor) {
            result = Color.getHSBColor(1.0f * multiColorIndex / multiColorMax, 0.45f, 0.65f);
        } else {
            if (JIPipeDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = JIPipeDesktopGraphCanvasUIConstants.COLOR_EDGE_DEFAULT;
            else if (JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = JIPipeDesktopGraphCanvasUIConstants.COLOR_EDGE_CONVERT;
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
