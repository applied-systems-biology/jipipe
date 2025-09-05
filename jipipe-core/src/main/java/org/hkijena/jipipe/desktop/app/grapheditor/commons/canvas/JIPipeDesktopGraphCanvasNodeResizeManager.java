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

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.awt.event.MouseEvent;

public class JIPipeDesktopGraphCanvasNodeResizeManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private JIPipeDesktopAnnotationGraphNodeUI currentResizeTarget;
    private Rectangle currentResizeOperationStartProperties;
    private Anchor currentResizeOperationAnchor;

    public JIPipeDesktopGraphCanvasNodeResizeManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void setCurrentResizeTarget(JIPipeDesktopAnnotationGraphNodeUI currentResizeTarget) {
        this.currentResizeTarget = currentResizeTarget;
    }

    public JIPipeDesktopAnnotationGraphNodeUI getCurrentResizeTarget() {
        return currentResizeTarget;
    }

    private Rectangle getCurrentResizeTargetAnchorArea(Anchor anchor) {
        if (currentResizeTarget != null) {
            return switch (anchor) {
                case TopLeft ->
                        new Rectangle(currentResizeTarget.getX() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case TopCenter ->
                        new Rectangle(currentResizeTarget.getX() + currentResizeTarget.getWidth() / 2 - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case TopRight ->
                        new Rectangle(currentResizeTarget.getRightX() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case CenterLeft ->
                        new Rectangle(currentResizeTarget.getX() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() + currentResizeTarget.getHeight() / 2 - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case CenterRight ->
                        new Rectangle(currentResizeTarget.getRightX() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() + currentResizeTarget.getHeight() / 2 - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case BottomLeft ->
                        new Rectangle(currentResizeTarget.getX() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case BottomCenter ->
                        new Rectangle(currentResizeTarget.getX() + currentResizeTarget.getWidth() / 2 - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                case BottomRight ->
                        new Rectangle(currentResizeTarget.getRightX() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE / 2, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE, JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_SIZE);
                default -> null;
            };
        } else {
            return null;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (currentResizeTarget != null && currentResizeOperationAnchor != null && currentResizeOperationStartProperties != null) {
            Point mouseInGrid = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(canvasUI.getLastMousePosition(), canvasUI.getZoom());
            int startGridX = currentResizeOperationStartProperties.x;
            int startGridY = currentResizeOperationStartProperties.y;
            int endGridX = currentResizeOperationStartProperties.x + currentResizeOperationStartProperties.width;
            int endGridY = currentResizeOperationStartProperties.y + currentResizeOperationStartProperties.height;
            switch (currentResizeOperationAnchor) {
                case TopLeft: {
                    int dY = mouseInGrid.y - startGridY;
                    int dGridHeight = -dY;
                    int newY = startGridY + dY;
                    int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                    int dX = mouseInGrid.x - startGridX;
                    int dGridWidth = -dX;
                    int newX = startGridX + dX;
                    int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                    if ((dX != 0 || dY != 0) && newWidth > 0 && newHeight > 0) {
                        currentResizeTarget.moveToGridLocation(new Point(newX, newY), true, true);
                        currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                    }
                }
                break;
                case TopCenter: {
                    int dY = mouseInGrid.y - startGridY;
                    int dGridHeight = -dY;
                    int newY = startGridY + dY;
                    int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                    if (dY != 0 && newHeight > 0) {
                        currentResizeTarget.moveToGridLocation(new Point(startGridX, newY), true, true);
                        currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width, newHeight);
                    }
                }
                break;
                case TopRight: {
                    int dY = mouseInGrid.y - startGridY;
                    int dGridHeight = -dY;
                    int dGridWidth = mouseInGrid.x - endGridX;
                    int newY = startGridY + dY;
                    int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                    int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                    if (newWidth > 0 && newHeight > 0) {
                        currentResizeTarget.moveToGridLocation(new Point(startGridX, newY), true, true);
                        currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                    }
                }
                break;
                case CenterLeft: {
                    int dX = mouseInGrid.x - startGridX;
                    int dGridWidth = -dX;
                    int newX = startGridX + dX;
                    int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                    if (dX != 0 && newWidth > 0) {
                        currentResizeTarget.moveToGridLocation(new Point(newX, startGridY), true, true);
                        currentResizeTarget.setNodeGridSize(newWidth, currentResizeOperationStartProperties.height);
                    }
                }
                break;
                case CenterRight: {
                    int dGridWidth = mouseInGrid.x - endGridX;
                    currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width + dGridWidth, currentResizeOperationStartProperties.height);
                }
                break;
                case BottomLeft: {
                    int dX = mouseInGrid.x - startGridX;
                    int dGridWidth = -dX;
                    int dGridHeight = mouseInGrid.y - endGridY;
                    int newX = startGridX + dX;
                    int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                    int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                    if (newWidth > 0 && newHeight > 0) {
                        currentResizeTarget.moveToGridLocation(new Point(newX, startGridY), true, true);
                        currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                    }
                }
                break;
                case BottomCenter: {
                    int dGridHeight = mouseInGrid.y - endGridY;
                    currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width, currentResizeOperationStartProperties.height + dGridHeight);
                }
                break;
                case BottomRight: {
                    int dGridWidth = mouseInGrid.x - endGridX;
                    int dGridHeight = mouseInGrid.y - endGridY;
                    currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width + dGridWidth, currentResizeOperationStartProperties.height + dGridHeight);
                }
                break;
            }
        }
    }

    public boolean mouseMoved(MouseEvent mouseEvent) {
        if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
            for (Anchor anchor : Anchor.values()) {
                Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                if (rectangle != null) {
                    if (rectangle.contains(mouseEvent.getPoint())) {
                        switch (anchor) {
                            case TopLeft:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                                break;
                            case TopCenter:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                                break;
                            case TopRight:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                                break;
                            case CenterLeft:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                                break;
                            case CenterRight:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                                break;
                            case BottomLeft:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                                break;
                            case BottomCenter:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                                break;
                            case BottomRight:
                                canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                                break;
                            default:
                                canvasUI.resetCursor();
                                break;
                        }
                        return true;
                    }
                } else {
                    canvasUI.resetCursor();
                }
            }
        } else {
            canvasUI.resetCursor();
        }
        return false;
    }

    public void stopAllResizing() {
        currentResizeOperationStartProperties = null;
    }

    public void paint(Graphics2D g) {
        if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
            g.setColor(Color.GRAY);
            g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_MARQUEE);
            g.drawRect(currentResizeTarget.getX() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE, currentResizeTarget.getY() - JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE, currentResizeTarget.getWidth() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE * 2, currentResizeTarget.getHeight() + JIPipeDesktopGraphCanvasResources.RESIZE_HANDLE_DISTANCE * 2);
            g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);

            for (Anchor anchor : Anchor.values()) {
                Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                if (rectangle != null) {
                    g.setColor(JIPipeDesktopGraphCanvasResources.COLOR_RESIZE_HANDLE_FILL);
                    g.fillOval(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                    g.setColor(JIPipeDesktopGraphCanvasResources.COLOR_RESIZE_HANDLE_BORDER);
                    g.drawOval(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                }
            }
        }
    }

    public boolean isResizing() {
        return currentResizeOperationStartProperties != null;
    }

    public boolean mousePressed(MouseEvent mouseEvent) {
        if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
            for (Anchor anchor : Anchor.values()) {
                Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                if (rectangle != null) {
                    if (rectangle.contains(mouseEvent.getPoint())) {
                        JIPipeAnnotationGraphNode node = (JIPipeAnnotationGraphNode) currentResizeTarget.getNode();
                        Point gridLocation = node.getNodeUILocationWithin(StringUtils.nullToEmpty(canvasUI.getCompartmentUUID()));
                        currentResizeOperationStartProperties = new Rectangle(gridLocation.x, gridLocation.y, node.getGridWidth(), node.getGridHeight());
                        currentResizeOperationAnchor = anchor;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
