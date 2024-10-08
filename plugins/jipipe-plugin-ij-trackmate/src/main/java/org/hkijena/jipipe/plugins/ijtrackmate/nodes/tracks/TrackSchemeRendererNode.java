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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks;

import com.mxgraph.util.mxCellRenderer;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeGraphComponent;
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

@SetJIPipeDocumentation(name = "Render track scheme", description = "Renders the track scheme as image")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nVisualize")
@AddJIPipeInputSlot(value = TrackCollectionData.class, name = "Tracks", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Track scheme", create = true)
public class TrackSchemeRendererNode extends JIPipeSimpleIteratingAlgorithm {

    public TrackSchemeRendererNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TrackSchemeRendererNode(TrackSchemeRendererNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        try {
            TrackScheme[] buffer = new TrackScheme[1];
            SwingUtilities.invokeAndWait(() -> {
                TrackScheme trackScheme = new TrackScheme(trackCollectionData.getModel(), new SelectionModel(trackCollectionData.getModel()), new DisplaySettings());
                trackScheme.render();
                trackScheme.getGUI().setVisible(false);
                buffer[0] = trackScheme;
            });
            Thread.sleep(500);
            SwingUtilities.invokeAndWait(() -> {
                TrackScheme trackScheme = buffer[0];
                TrackSchemeGraphComponent graphComponent = (TrackSchemeGraphComponent) ReflectionUtils.getDeclaredFieldValue("graphComponent", trackScheme.getGUI());
//                graphComponent.zoom(5);
//                graphComponent.setSize(graphComponent.getHorizontalScrollBar().getMaximum(), graphComponent.getVerticalScrollBar().getMaximum());
                BufferedImage image = mxCellRenderer.createBufferedImage(graphComponent.getGraph(), null, 1, Color.WHITE, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());
//                BufferedImage image1 = ScreenImage.createImage(graphComponent);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(new ImagePlus("Track Scheme", image)), progressInfo);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
