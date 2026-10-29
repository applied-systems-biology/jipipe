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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation;
import org.hkijena.jipipe.api.nodes.taxonomy.families.TrackMateFamily;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.TrackMateFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingAnalysisFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

@JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, label = "Convert tracks to RGB", after = {MeasureTracksNode.class}, purpose = "Render the measured tracks as RGB images")
@SetJIPipeDocumentation(name = "Convert tracks to RGB", description = "Renders measured tracks as a color RGB image, optionally overlaid on a supplied RGB reference image. It takes a track collection and an optional RGB image, draws the tracks on every time and z slice using the track visualization settings and magnification, and produces a new RGB image with the same dimensions and scale.")
@ConfigureJIPipeNode(function = TrackingAnalysisFunction.class, framework = TrackMateFramework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = TrackCollectionData.class, name = "Tracks", create = true)
@AddJIPipeInputSlot(value = ImagePlusColorRGBData.class, name = "Image", create = true, optional = true, description = "")
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
        ImageJIterationUtils.forEachIndexedZCTSlice(reference, (sourceIp, index) -> {
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

    @SetJIPipeDocumentation(name = "Track visualization", description = "The following settings control how tracks are visualized")
    @JIPipeParameter("track-visualization")
    public TrackDrawer getTrackDrawer() {
        return trackDrawer;
    }

    public void setTrackDrawer(TrackDrawer trackDrawer) {
        this.trackDrawer = trackDrawer;
    }

    @SetJIPipeDocumentation(name = "Magnification", description = "Magnification applied during the rendering")
    @JIPipeParameter("magnification")
    public double getMagnification() {
        return magnification;
    }

    @JIPipeParameter("magnification")
    public void setMagnification(double magnification) {
        this.magnification = magnification;
    }
}
