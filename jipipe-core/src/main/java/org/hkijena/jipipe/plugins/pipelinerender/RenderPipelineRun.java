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

package org.hkijena.jipipe.plugins.pipelinerender;

import com.google.common.collect.ImmutableList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDropShadowRenderer;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ui.ScreenImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderPipelineRun extends AbstractJIPipeRunnable {
    private final JIPipeProject project;
    private final Path outputPath;
    private final RenderPipelineRunSettings settings;
    private BufferedImage outputImage;

    public RenderPipelineRun(JIPipeProject project, Path outputPath, RenderPipelineRunSettings settings) {
        this.project = project;
        this.outputPath = outputPath;
        this.settings = settings;
    }

    @Override
    public String getTaskLabel() {
        return "Render whole pipeline";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        JIPipeGraph copyGraph = new JIPipeGraph(project.getGraph());
        ImmutableList<UUID> compartmentUUIDs = ImmutableList.copyOf(project.getCompartments().keySet());
        getProgressInfo().setMaxProgress(compartmentUUIDs.size() + 1);

        // Pre-calculate the expected label height
        int labelBoxHeight = 0;
        if (settings.isRenderLabel()) {
            int labelTextHeight = (int) (settings.getLabelFontSize() * 1.333);
            labelBoxHeight = labelTextHeight * 2;
        }

        // Render the compartments and measure their size
        Map<UUID, BufferedImage> compartmentInsides = new HashMap<>();
        renderCompartmentInsides(copyGraph, compartmentUUIDs, compartmentInsides);

        progressInfo.log("Rendering full image");
        progressInfo.setProgress(compartmentUUIDs.size() + 1);

        // Layout the compartment graph
        Map<UUID, Rectangle> compartmentBounds = new HashMap<>();
        calculateCompartmentBounds(project.getCompartmentGraph(), compartmentBounds);

        // Calculate the scale factor
        int scaleFactor = 1;
        for (UUID uuid : compartmentUUIDs) {
            BufferedImage bufferedImage = compartmentInsides.get(uuid);
            Rectangle bounds = compartmentBounds.get(uuid);

            int expectedWidth = bufferedImage.getWidth() + 16;
            int expectedHeight = bufferedImage.getHeight() + 16;
            expectedHeight += labelBoxHeight + 16;

            scaleFactor = (int) Math.round(Math.max(scaleFactor, Math.max(expectedWidth / bounds.getWidth(), expectedHeight / bounds.getHeight())));
        }
        progressInfo.log("Scale factor is " + scaleFactor);
        progressInfo.log("If this causes issues, arrange your nodes and compartments in a space-efficient way!");

        // Calculate final image dimensions
        final JIPipeGraphViewMode compartmentGraphViewMode = JIPipeGraphViewMode.VerticalCompact;
        int outputWidth = 0;
        int outputHeight = 0;
        for (Rectangle bounds : compartmentBounds.values()) {
            outputWidth = Math.max(outputWidth, bounds.x + bounds.width);
            outputHeight = Math.max(outputHeight, bounds.y + bounds.height);
        }
        outputWidth = outputWidth * scaleFactor + compartmentGraphViewMode.getGridWidth() * scaleFactor;
        outputHeight = outputHeight * scaleFactor + compartmentGraphViewMode.getGridHeight() * scaleFactor;
        progressInfo.log("The generated image will have a size of " + outputWidth + "x" + outputHeight + " pixels");

        // Create image
        BufferedImage finalImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = finalImage.getGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill general background
        graphics2D.setPaint(UIManager.getColor("EditorPane.background"));
        graphics2D.fillRect(0, 0, outputWidth, outputHeight);

        // Draw edges
        final int strokeBorderWidth;
        final int defaultStrokeWidth;
        if (settings.getMaxEdgeWidth().isEnabled() && (6 * scaleFactor) > settings.getMaxEdgeWidth().getContent()) {
            strokeBorderWidth = settings.getMaxEdgeWidth().getContent();
            defaultStrokeWidth = (int) (settings.getMaxEdgeWidth().getContent() * 4.0 / 6.0);
        } else {
            strokeBorderWidth = 6 * scaleFactor;
            defaultStrokeWidth = 4 * scaleFactor;
        }
        final Stroke defaultStroke = new BasicStroke(defaultStrokeWidth);
        final Stroke strokeBorder = new BasicStroke(strokeBorderWidth);
        final Color improvedStrokeBackgroundColor = UIManager.getColor("Panel.background");
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : project.getCompartmentGraph().getSlotEdges()) {
            JIPipeGraphEdge graphEdge = project.getCompartmentGraph().getGraph().getEdge(edge.getKey(), edge.getValue());
            UUID sourceCompartment = edge.getKey().getNode().getUUIDInParentGraph();
            UUID targetCompartment = edge.getValue().getNode().getUUIDInParentGraph();
            Rectangle sourceBounds = new Rectangle(compartmentBounds.get(sourceCompartment));
            Rectangle targetBounds = new Rectangle(compartmentBounds.get(targetCompartment));
            sourceBounds.x *= scaleFactor;
            sourceBounds.y *= scaleFactor;
            sourceBounds.width *= scaleFactor;
            sourceBounds.height *= scaleFactor;
            targetBounds.x *= scaleFactor;
            targetBounds.y *= scaleFactor;
            targetBounds.width *= scaleFactor;
            targetBounds.height *= scaleFactor;

            int pointRangeMargin = defaultStrokeWidth * 2;

            PointRange sourceRange = new PointRange(new Point(sourceBounds.x + sourceBounds.width / 2, sourceBounds.y + sourceBounds.height),
                    new Point(sourceBounds.x + pointRangeMargin, sourceBounds.y + sourceBounds.height),
                    new Point(sourceBounds.x + sourceBounds.width - pointRangeMargin * 2, sourceBounds.y + sourceBounds.height));
            PointRange targetRange = new PointRange(new Point(targetBounds.x + targetBounds.width / 2, targetBounds.y),
                    new Point(targetBounds.x + pointRangeMargin, targetBounds.y),
                    new Point(targetBounds.x + targetBounds.width - pointRangeMargin * 2, targetBounds.y));
            PointRange.tighten(sourceRange, targetRange);

            // Improved edge render
            graphics2D.setStroke(strokeBorder);
            graphics2D.setColor(Color.LIGHT_GRAY);
            drawEdge(graphics2D,
                    sourceRange.center,
                    sourceBounds,
                    targetRange.center,
                    graphEdge.getUiShape(),
                    compartmentGraphViewMode, scaleFactor);
            graphics2D.setStroke(defaultStroke);
            graphics2D.setColor(improvedStrokeBackgroundColor);
            drawEdge(graphics2D,
                    sourceRange.center,
                    sourceBounds,
                    targetRange.center,
                    graphEdge.getUiShape(),
                    compartmentGraphViewMode, scaleFactor);
        }

        // Generate compartment images
        int shadowSize = 5 * scaleFactor;
        int shadowShiftLeft = 3 * scaleFactor;
        int shadowShiftRight = 8 * scaleFactor;
        JIPipeDesktopDropShadowRenderer dropShadowRenderer = new JIPipeDesktopDropShadowRenderer(Color.BLACK,
                5 * scaleFactor,
                0.3f,
                shadowSize,
                true,
                true,
                true,
                true);
        Font labelFont = null;
        if (settings.isRenderLabel()) {
            labelFont = new Font(Font.DIALOG, Font.PLAIN, settings.getLabelFontSize());
        }
        for (UUID compartmentUUID : compartmentUUIDs) {
            Rectangle bounds = compartmentBounds.get(compartmentUUID);
            final int bx = bounds.x * scaleFactor;
            final int by = bounds.y * scaleFactor;
            final int bWidth = bounds.width * scaleFactor;
            final int bHeight = bounds.height * scaleFactor;

            // Draw shadow
            if (settings.isRenderShadow()) {
                dropShadowRenderer.paint(graphics2D, bx - shadowShiftLeft, by - shadowShiftLeft, bWidth + shadowShiftRight, bHeight + shadowShiftRight);
            }

            // Fill background
            graphics2D.setPaint(UIManager.getColor("EditorPane.background"));
            graphics2D.fillRect(bx, by, bWidth, bHeight);

            // Draw label box
            if (settings.isRenderLabel()) {
                graphics2D.setPaint(UIManager.getColor("Panel.background"));
                graphics2D.fillRect(bx, by, bWidth, labelBoxHeight);
                graphics2D.setPaint(Color.LIGHT_GRAY);
                graphics2D.setStroke(new BasicStroke(Math.min(5, scaleFactor)));
                graphics2D.drawLine(bx, by + labelBoxHeight, bx + bWidth - 1, by + labelBoxHeight);
            }

            // Draw border
            graphics2D.setPaint(Color.LIGHT_GRAY);
            graphics2D.setStroke(new BasicStroke(Math.min(5, scaleFactor)));
            graphics2D.drawRect(bx, by, bWidth, bHeight);

            // Draw label text
            if (settings.isRenderLabel()) {
                graphics2D.setColor(UIManager.getColor("Label.foreground"));
                graphics2D.setFont(labelFont);
                FontMetrics fontMetrics = graphics2D.getFontMetrics(labelFont);
                String text = project.getCompartments().get(compartmentUUID).getName();
                LineMetrics lineMetrics = fontMetrics.getLineMetrics(text, graphics);
                int labelTextHeight = (int) (lineMetrics.getAscent() - lineMetrics.getDescent());
                graphics2D.drawString(project.getCompartments().get(compartmentUUID).getName(), bx + labelBoxHeight / 2, by + labelTextHeight + labelBoxHeight / 2 - labelTextHeight / 2);
            }
        }

        // Draw renders
        for (UUID compartmentUUID : compartmentUUIDs) {
            Rectangle bounds = compartmentBounds.get(compartmentUUID);
            final int bx = bounds.x * scaleFactor;
            final int by = bounds.y * scaleFactor;
            final int bWidth = bounds.width * scaleFactor;
            final int bHeight = bounds.height * scaleFactor;

            BufferedImage bufferedImage = compartmentInsides.get(compartmentUUID);
            graphics.drawImage(bufferedImage,
                    bx + bWidth / 2 - bufferedImage.getWidth() / 2,
                    by + (bHeight - labelBoxHeight) / 2 - bufferedImage.getHeight() / 2 + labelBoxHeight,
                    null);
        }

        graphics.dispose();

        // Save image
        if (outputPath != null) {
            progressInfo.log("Saving to " + outputPath);
            try {
                ImageIO.write(finalImage, "PNG", outputPath.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.outputImage = finalImage;
        }
    }

    /**
     * The generated image. Only set if outputPath is null.
     *
     * @return the generated image or null
     */
    public BufferedImage getOutputImage() {
        return outputImage;
    }

    private void drawEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphEdge.Shape shape, JIPipeGraphViewMode viewMode, int scaleFactor) {
        switch (shape) {
            case Elbow:
                drawElbowEdge(g, sourcePoint, sourceBounds, targetPoint, viewMode, scaleFactor);
                break;
            case Line:
                g.drawLine((int) (1.0 * sourcePoint.x),
                        (int) (1.0 * sourcePoint.y),
                        (int) (1.0 * targetPoint.x),
                        (int) (1.0 * targetPoint.y));
                break;
        }
    }

    private void drawElbowEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphViewMode viewMode, int scaleFactor) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        buffer = (viewMode.getGridHeight() / 2) * scaleFactor;
        sourceA = sourcePoint.y;
        targetA = targetPoint.y;
        sourceB = sourcePoint.x;
        targetB = targetPoint.x;
        componentStartB = sourceBounds.x;
        componentEndB = sourceBounds.x + sourceBounds.width;

        int a0 = sourceA;
        int b0 = sourceB;
        int a1 = sourceA;
        int b1 = sourceB;

        TIntArrayList xCoords = new TIntArrayList(8);
        TIntArrayList yCoords = new TIntArrayList(8);

        addElbowPolygonCoordinate(a0, b0, 1.0, 0, 0, xCoords, yCoords);

        // Target point is above the source. We have to navigate around it
        if (sourceA > targetA) {
            // Add some space in major direction
            a1 += buffer;
            addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);

            // Go left or right
            if (targetB <= b1) {
                b1 = Math.max(0, componentStartB - buffer);
            } else {
                b1 = componentEndB + buffer;
            }
            addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);

            // Go to target height
            a1 = Math.max(0, targetA - buffer);
            addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            a1 = Math.min(sourceA + buffer, sourceA + dA / 2);
            addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);
        }

        // Target point X is shifted
        if (b1 != targetB) {
            b1 = targetB;
            addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);
        }

        // Go to end point
        a1 = targetA;
        addElbowPolygonCoordinate(a1, b1, 1.0, 0, 0, xCoords, yCoords);

        // Draw the polygon
        g.drawPolyline(xCoords.toArray(), yCoords.toArray(), xCoords.size());
    }

    private void addElbowPolygonCoordinate(int a1, int b1, double scale, int viewX, int viewY, TIntList xCoords, TIntList yCoords) {
        int x2, y2;
        x2 = (int) (b1 * scale) + viewX;
        y2 = (int) (a1 * scale) + viewY;
        xCoords.add(x2);
        yCoords.add(y2);
    }

    private void calculateCompartmentBounds(JIPipeGraph compartmentGraph, Map<UUID, Rectangle> compartmentBounds) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JIPipeDesktopGraphCanvasUI canvasUI = new JIPipeDesktopGraphCanvasUI(new JIPipeDesktopDummyWorkbench(), null, compartmentGraph, null, new JIPipeDummyGraphHistoryJournal());
                canvasUI.setRenderCursor(false);
                canvasUI.revalidate();
                canvasUI.crop(false);
                canvasUI.revalidate();
                for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> entry : canvasUI.getNodeUIs().entrySet()) {
                    compartmentBounds.put(entry.getKey().getUUIDInParentGraph(), entry.getValue().getBounds());
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderCompartmentInsides(JIPipeGraph copyGraph, ImmutableList<UUID> compartmentUUIDs, Map<UUID, BufferedImage> compartmentRenders) {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        for (int i = 0; i < compartmentUUIDs.size(); i++) {
            progressInfo.setProgress(i + 1);
            progressInfo.log("Rendering compartment " + compartmentUUIDs.get(i));
            UUID compartmentUUID = compartmentUUIDs.get(i);
            BufferedImage image = renderCompartment(copyGraph, compartmentUUID);
            compartmentRenders.put(compartmentUUID, image);
        }
    }

    private BufferedImage renderCompartment(JIPipeGraph copyGraph, UUID compartment) {
        BufferedImage[] result = new BufferedImage[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                JIPipeDesktopGraphCanvasUI canvasUI = new JIPipeDesktopGraphCanvasUI(new JIPipeDesktopDummyWorkbench(), null, copyGraph, compartment, new JIPipeDummyGraphHistoryJournal());
                canvasUI.setRenderCursor(false);
                canvasUI.setRenderOutsideEdges(false);
                canvasUI.revalidate();
                canvasUI.crop(true);
                canvasUI.revalidate();
                BufferedImage image = ScreenImage.createImage(canvasUI);
                result[0] = image;
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return result[0];
    }

    public JIPipeProject getProject() {
        return project;
    }
}
