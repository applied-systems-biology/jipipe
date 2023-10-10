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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;

@JIPipeDocumentation(name = "Convert spots to RGB", description = "Visualizes spots")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nVisualize")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Spots", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusColorRGBData.class, slotName = "Image", autoCreate = true, optional = true, description = "")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
public class SpotsToRGBNode extends JIPipeIteratingAlgorithm {
    private SpotDrawer spotDrawer = new SpotDrawer();

    private double magnification = 1.0;

    public SpotsToRGBNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SpotsToRGBNode(SpotsToRGBNode other) {
        super(other);
        this.spotDrawer = new SpotDrawer(other.spotDrawer);
        this.magnification = other.magnification;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = dataBatch.getInputData("Spots", SpotsCollectionData.class, progressInfo);
        ImagePlus reference;
        {
            ImagePlusData data = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
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
            spotDrawer.drawOnGraphics(spotsCollectionData, graphics2D, new Rectangle(0, 0, scaledSourceIp.getWidth(), scaledSourceIp.getHeight()), index, Collections.emptySet());
            graphics2D.dispose();
            ColorProcessor render = new ColorProcessor(bufferedImage);
            targetStack.setProcessor(render, index.zeroSliceIndexToOneStackIndex(reference));
        }, progressInfo);

        // Generate final output
        ImagePlus result = new ImagePlus("Spots visualization", targetStack);
        ImageJUtils.copyHyperstackDimensions(reference, result);
        result.copyScale(reference);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Spot visualization", description = "The following settings control how spots are visualized")
    @JIPipeParameter("spot-visualization")
    public SpotDrawer getSpotDrawer() {
        return spotDrawer;
    }

    public void setSpotDrawer(SpotDrawer spotDrawer) {
        this.spotDrawer = spotDrawer;
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
