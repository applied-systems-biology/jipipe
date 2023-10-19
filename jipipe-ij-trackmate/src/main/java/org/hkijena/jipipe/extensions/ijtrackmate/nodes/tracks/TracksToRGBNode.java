/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;

@JIPipeDocumentation(name = "Convert tracks to RGB", description = "Visualizes tracks")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nVisualize")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Tracks", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusColorRGBData.class, slotName = "Image", autoCreate = true, optional = true, description = "")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
public class TracksToRGBNode extends JIPipeIteratingAlgorithm {
    private TrackDrawer trackDrawer = new TrackDrawer();

    private double magnification = 1.0;

    public TracksToRGBNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TracksToRGBNode(TracksToRGBNode other) {
        super(other);
        this.trackDrawer = new TrackDrawer(other.trackDrawer);
        this.magnification = other.magnification;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData spotsCollectionData = iterationStep.getInputData("Tracks", TrackCollectionData.class, progressInfo);
        ImagePlus reference;
        {
            ImagePlusData data = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
            if (data != null) {
                reference = data.getImage();
            } else {
                reference = spotsCollectionData.getImage();
            }
        }
        final int targetWidth = (int) (magnification * reference.getWidth());
        final int targetHeight = (int) (magnification * reference.getHeight());
        ImageStack targetStack = new ImageStack(targetWidth, targetHeight, reference.getStackSize());
        ImageJUtils.forEachIndexedZCTSlice(reference, (sourceIp, index) -> {
            ImageProcessor scaledSourceIp = magnification != 1.0 ? sourceIp.resize((int) (magnification * sourceIp.getWidth()), (int) (magnification * sourceIp.getHeight()), false) : sourceIp;
            BufferedImage bufferedImage = BufferedImageUtils.copyBufferedImageToARGB(scaledSourceIp.getBufferedImage());
            Graphics2D graphics2D = bufferedImage.createGraphics();
            trackDrawer.drawOnGraphics(spotsCollectionData, graphics2D, new Rectangle(0, 0, scaledSourceIp.getWidth(), scaledSourceIp.getHeight()), index, Collections.emptySet());
            graphics2D.dispose();
            ColorProcessor render = new ColorProcessor(bufferedImage);
            targetStack.setProcessor(render, index.zeroSliceIndexToOneStackIndex(reference));
        }, progressInfo);

        // Generate final output
        ImagePlus result = new ImagePlus("Track visualization", targetStack);
        ImageJUtils.copyHyperstackDimensions(reference, result);
        result.copyScale(reference);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Track visualization", description = "The following settings control how tracks are visualized")
    @JIPipeParameter("track-visualization")
    public TrackDrawer getTrackDrawer() {
        return trackDrawer;
    }

    public void setTrackDrawer(TrackDrawer trackDrawer) {
        this.trackDrawer = trackDrawer;
    }

    @JIPipeDocumentation(name = "Magnification", description = "Magnification applied during the rendering")
    @JIPipeParameter("magnification")
    public double getMagnification() {
        return magnification;
    }

    @JIPipeParameter("magnification")
    public void setMagnification(double magnification) {
        this.magnification = magnification;
    }
}
