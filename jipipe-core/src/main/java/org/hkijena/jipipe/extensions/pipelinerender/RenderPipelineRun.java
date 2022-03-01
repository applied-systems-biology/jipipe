package org.hkijena.jipipe.extensions.pipelinerender;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.renderers.DropShadowRenderer;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.ui.ScreenImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderPipelineRun implements JIPipeRunnable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final JIPipeProject project;
    private final Path outputPath;
    private final RenderPipelineRunSettings settings;

    public RenderPipelineRun(JIPipeProject project, Path outputPath, RenderPipelineRunSettings settings) {
        this.project = project;
        this.outputPath = outputPath;
        this.settings = settings;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Render whole pipeline";
    }

    @Override
    public void run() {
        JIPipeGraph copyGraph = new JIPipeGraph(project.getGraph());
        ImmutableList<UUID> compartmentUUIDs = ImmutableList.copyOf(project.getCompartments().keySet());
        getProgressInfo().setMaxProgress(compartmentUUIDs.size() + 1);

        // Pre-calculate the expected label height
        int labelBoxHeight = 0;
        if(settings.isRenderLabel()) {
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
        int factor = 1;
        for (UUID uuid : compartmentUUIDs) {
            BufferedImage bufferedImage = compartmentInsides.get(uuid);
            Rectangle bounds = compartmentBounds.get(uuid);

            int expectedWidth = bufferedImage.getWidth() + 16;
            int expectedHeight = bufferedImage.getHeight() + 16;
            expectedHeight += labelBoxHeight * 2;

            factor = (int)Math.round(Math.max(factor, Math.max(expectedWidth / bounds.getWidth(), expectedHeight / bounds.getHeight())));
        }

        // Calculate final image dimensions
        JIPipeGraphViewMode compartmentGraphViewMode = project.getCompartmentGraph().getAdditionalMetadata(JIPipeGraphViewMode.class, "jipipe:graph:view-mode");
        if(compartmentGraphViewMode == null) {
            compartmentGraphViewMode = JIPipeGraphViewMode.VerticalCompact;
        }
        int outputWidth = 0;
        int outputHeight = 0;
        for (Rectangle bounds : compartmentBounds.values()) {
            outputWidth = Math.max(outputWidth, bounds.x + bounds.width);
            outputHeight = Math.max(outputHeight, bounds.y + bounds.height);
        }
        outputWidth = outputWidth * factor + compartmentGraphViewMode.getGridWidth() * factor;
        outputHeight = outputHeight * factor + compartmentGraphViewMode.getGridHeight() * factor;
        progressInfo.log("The generated image will have a size of " + outputWidth + "x" + outputHeight + " pixels");

        // Create image
        BufferedImage finalImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = finalImage.getGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill general background
        graphics2D.setPaint(UIManager.getColor("EditorPane.background"));
        graphics2D.fillRect(0,0,outputWidth, outputHeight);

        // TODO: Draw edges

        // Generate compartment images
        int shadowSize = 5 * factor;
        int shadowShiftLeft = 3 * factor;
        int shadowShiftRight = 8 * factor;
        DropShadowRenderer dropShadowRenderer = new DropShadowRenderer(Color.BLACK,
                (int) (5 * factor),
                0.3f,
                shadowSize,
                true,
                true,
                true,
                true);
        Font labelFont = null;
        if(settings.isRenderLabel()) {
            labelFont = new Font(Font.DIALOG, Font.PLAIN, settings.getLabelFontSize());
        }
        for (UUID compartmentUUID : compartmentUUIDs) {
            Rectangle bounds = compartmentBounds.get(compartmentUUID);
            final int bx = bounds.x * factor;
            final int by = bounds.y * factor;
            final int bWidth = bounds.width * factor;
            final int bHeight = bounds.height * factor;

            // Draw shadow
            dropShadowRenderer.paint(graphics2D, bx -shadowShiftLeft, by - shadowShiftLeft, bWidth + shadowShiftRight, bHeight + shadowShiftRight);

            // Fill background
            graphics2D.setPaint(UIManager.getColor("EditorPane.background"));
            graphics2D.fillRect(bx, by, bWidth, bHeight);

            // Draw label box
            if(settings.isRenderLabel()) {
                graphics2D.setPaint(UIManager.getColor("Panel.background"));
                graphics2D.fillRect(bx, by, bWidth, labelBoxHeight);
                graphics2D.setPaint(Color.LIGHT_GRAY);
                graphics2D.setStroke(new BasicStroke(Math.min(5, factor)));
                graphics2D.drawLine(bx, by + labelBoxHeight, bx + bWidth - 1, by + labelBoxHeight);
            }

            // Draw border
            graphics2D.setPaint(Color.LIGHT_GRAY);
            graphics2D.setStroke(new BasicStroke(Math.min(5, factor)));
            graphics2D.drawRect(bx, by, bWidth, bHeight);

            // Draw label text
            if(settings.isRenderLabel()) {
                graphics2D.setColor(UIManager.getColor("Label.foreground"));
                graphics2D.setFont(labelFont);
                FontMetrics fontMetrics = graphics2D.getFontMetrics(labelFont);
                int labelTextHeight = fontMetrics.getHeight();
                graphics2D.drawString(project.getCompartments().get(compartmentUUID).getName(), bx + 16, by + labelTextHeight + labelBoxHeight / 2 - labelTextHeight / 2);
            }
        }

        // Draw renders
        for (UUID compartmentUUID : compartmentUUIDs) {
            Rectangle bounds = compartmentBounds.get(compartmentUUID);
            final int bx = bounds.x * factor;
            final int by = bounds.y * factor;
            final int bWidth = bounds.width * factor;
            final int bHeight = bounds.height * factor;

            BufferedImage bufferedImage = compartmentInsides.get(compartmentUUID);
            graphics.drawImage(bufferedImage,
                    bx + bWidth / 2 - bufferedImage.getWidth() / 2,
                    by + (bHeight - labelBoxHeight) / 2 - bufferedImage.getHeight() / 2 + labelBoxHeight,
                    null);
        }

        graphics.dispose();

        // Save image
        progressInfo.log("Saving to " + outputPath);
        try {
            ImageIO.write(finalImage, "PNG", outputPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void calculateCompartmentBounds(JIPipeGraph compartmentGraph, Map<UUID, Rectangle> compartmentBounds) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JIPipeGraphCanvasUI canvasUI = new JIPipeGraphCanvasUI(new JIPipeDummyWorkbench(), null, compartmentGraph, null, new JIPipeDummyGraphHistoryJournal());
                canvasUI.setRenderCursor(false);
                canvasUI.revalidate();
                canvasUI.crop(false);
                canvasUI.revalidate();
                for (Map.Entry<JIPipeGraphNode, JIPipeNodeUI> entry : canvasUI.getNodeUIs().entrySet()) {
                    compartmentBounds.put(entry.getKey().getUUIDInGraph(), entry.getValue().getBounds());
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderCompartmentInsides(JIPipeGraph copyGraph, ImmutableList<UUID> compartmentUUIDs, Map<UUID, BufferedImage> compartmentRenders) {
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
                JIPipeGraphCanvasUI canvasUI = new JIPipeGraphCanvasUI(new JIPipeDummyWorkbench(), null, copyGraph, compartment, new JIPipeDummyGraphHistoryJournal());
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
