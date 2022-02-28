package org.hkijena.jipipe.extensions.pipelinerender;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.utils.ui.ScreenImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderPipelineRun implements JIPipeRunnable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final JIPipeProject project;

    public RenderPipelineRun(JIPipeProject project) {
        this.project = project;
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

        // Render the compartments and measure their size
        Map<UUID, BufferedImage> compartmentRenders = new HashMap<>();

        int maxInsideWidth = 0;
        int maxInsideHeight = 0;

        for (int i = 0; i < compartmentUUIDs.size(); i++) {
            progressInfo.setProgress(i + 1);
            progressInfo.log("Rendering compartment " + compartmentUUIDs.get(i));
            UUID compartmentUUID = compartmentUUIDs.get(i);
            BufferedImage image = renderCompartment(copyGraph, compartmentUUID);
            compartmentRenders.put(compartmentUUID, image);

            maxInsideWidth = Math.max(maxInsideWidth, image.getWidth());
            maxInsideHeight = Math.max(maxInsideHeight, image.getHeight());
        }

        progressInfo.log("Rendering full image");
        progressInfo.setProgress(compartmentUUIDs.size() + 1);

        // Measure out the size of the compartment graph in grids
        int compartmentGraphGridsX = 0;
        int compartmentGraphGridsY = 0;

        Map<UUID, Point> compartmentGridLocations = new HashMap<>();
        JIPipeGraphViewMode compartmentGraphViewMode = project.getCompartmentGraph().getAdditionalMetadata(JIPipeGraphViewMode.class, "jipipe:graph:view-mode");
        if(compartmentGraphViewMode == null) {
            compartmentGraphViewMode = JIPipeGraphViewMode.VerticalCompact;
        }

        int maxCompartmentGraphRealSizeY = 0; // Grid size in real coordinates
        switch (compartmentGraphViewMode) {
            case Vertical:
            case VerticalCompact: {
                // Verticals are always 3 tall
                maxCompartmentGraphRealSizeY = compartmentGraphViewMode.getGridHeight() * 3;
            }
        }

        for (JIPipeGraphNode graphNode : project.getCompartmentGraph().getGraphNodes()) {
            Point location = graphNode.getLocationWithin("", compartmentGraphViewMode.name());
            compartmentGraphGridsX = Math.max(compartmentGraphGridsX, location.x);
            compartmentGraphGridsY = Math.max(compartmentGraphGridsY, location.y);
            compartmentGridLocations.put(graphNode.getUUIDInGraph(), location);

            if(compartmentGraphViewMode == JIPipeGraphViewMode.Horizontal) {
                maxCompartmentGraphRealSizeY = Math.max(maxCompartmentGraphRealSizeY, compartmentGraphViewMode.getGridHeight() * (graphNode.getOutputSlots().size() + 1));
            }
        }

        // Convert from grid into real coordinates by using the known constrains of the view modes
        int compartmentGridWidth;
        int compartmentGridHeight;
        {
            double factor = 1.0 * maxInsideHeight / maxCompartmentGraphRealSizeY;
            compartmentGridHeight = (int)(compartmentGraphViewMode.getGridHeight() * factor);
            compartmentGridWidth = (int)(compartmentGraphViewMode.getGridWidth() * factor);
        }

    }

    private BufferedImage renderCompartment(JIPipeGraph copyGraph, UUID compartment) {
        BufferedImage[] result = new BufferedImage[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                JIPipeGraphCanvasUI canvasUI = new JIPipeGraphCanvasUI(new JIPipeDummyWorkbench(), null, copyGraph, compartment, new JIPipeDummyGraphHistoryJournal());
                canvasUI.revalidate();
                canvasUI.crop();
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
