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
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.roi.IntModificationParameter;

import java.awt.Color;

@JIPipeDocumentation(name = "Make images equal 2D", description = "Makes all images within a data batch have the same size by padding with the predefined background color according to the largest image dimensions.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class TransformEqualCanvasSize2DAlgorithm extends JIPipeMergingAlgorithm {

    private IntModificationParameter xAxis = new IntModificationParameter();
    private IntModificationParameter yAxis = new IntModificationParameter();
    private Color backgroundColor = Color.BLACK;
    private Anchor anchor = Anchor.CenterCenter;

    public TransformEqualCanvasSize2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformEqualCanvasSize2DAlgorithm(TransformEqualCanvasSize2DAlgorithm other) {
        super(other);
        this.xAxis = new IntModificationParameter(other.xAxis);
        this.yAxis = new IntModificationParameter(other.yAxis);
        this.backgroundColor = other.backgroundColor;
        this.anchor = other.anchor;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeRunnableInfo progress) {

        int wNew = 0;
        int hNew = 0;
        for (ImagePlusData image : dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class)) {
            wNew = Math.max(image.getImage().getWidth(), wNew);
            hNew = Math.max(image.getImage().getHeight(), hNew);
        }

        for (ImagePlusData image : dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class)) {
            ImagePlus imp = image.getImage();
            int wOld = imp.getWidth();
            int hOld = imp.getHeight();

            int xOff, yOff;
            int xC = (wNew - wOld) / 2;    // offset for centered
            int xR = (wNew - wOld);        // offset for right
            int yC = (hNew - hOld) / 2;    // offset for centered
            int yB = (hNew - hOld);        // offset for bottom

            switch (anchor) {
                case TopLeft:    // TL
                    xOff = 0;
                    yOff = 0;
                    break;
                case TopCenter:    // TC
                    xOff = xC;
                    yOff = 0;
                    break;
                case TopRight:    // TR
                    xOff = xR;
                    yOff = 0;
                    break;
                case CenterLeft: // CL
                    xOff = 0;
                    yOff = yC;
                    break;
                case CenterCenter: // C
                    xOff = xC;
                    yOff = yC;
                    break;
                case CenterRight:    // CR
                    xOff = xR;
                    yOff = yC;
                    break;
                case BottomLeft: // BL
                    xOff = 0;
                    yOff = yB;
                    break;
                case BottomCenter: // BC
                    xOff = xC;
                    yOff = yB;
                    break;
                case BottomRight: // BR
                    xOff = xR;
                    yOff = yB;
                    break;
                default: // center
                    xOff = xC;
                    yOff = yC;
                    break;
            }

            if (imp.isStack()) {
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Expanded", expandStack(imp.getStack(), wNew, hNew, xOff, yOff))));
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Expanded", expandImage(imp.getProcessor(), wNew, hNew, xOff, yOff))));
            }
        }
    }

    public ImageStack expandStack(ImageStack stackOld, int wNew, int hNew, int xOff, int yOff) {
        int nFrames = stackOld.getSize();
        ImageProcessor ipOld = stackOld.getProcessor(1);

        ImageStack stackNew = new ImageStack(wNew, hNew, stackOld.getColorModel());
        ImageProcessor ipNew;

        for (int i = 1; i <= nFrames; i++) {
            IJ.showProgress((double) i / nFrames);
            ipNew = ipOld.createProcessor(wNew, hNew);
            ipNew.setColor(backgroundColor);
            ipNew.fill();
            ipNew.insert(stackOld.getProcessor(i), xOff, yOff);
            stackNew.addSlice(stackOld.getSliceLabel(i), ipNew);
        }
        return stackNew;
    }

    public ImageProcessor expandImage(ImageProcessor ipOld, int wNew, int hNew, int xOff, int yOff) {
        ImageProcessor ipNew = ipOld.createProcessor(wNew, hNew);
        ipNew.setColor(backgroundColor);
        ipNew.fill();
        ipNew.insert(ipOld, xOff, yOff);
        return ipNew;
    }

    @JIPipeDocumentation(name = "X axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("x-axis")
    public IntModificationParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(IntModificationParameter xAxis) {
        this.xAxis = xAxis;
    }

    @JIPipeDocumentation(name = "Y axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("y-axis")
    public IntModificationParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(IntModificationParameter yAxis) {
        this.yAxis = yAxis;
    }

    @JIPipeDocumentation(name = "Background color", description = "The color of the outside canvas")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JIPipeDocumentation(name = "Anchor", description = "From which point to expand the canvas")
    @JIPipeParameter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }
}
