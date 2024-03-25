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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CanvasEqualizer extends AbstractJIPipeParameterCollection {
    private JIPipeExpressionParameter xAxis = new JIPipeExpressionParameter("");
    private JIPipeExpressionParameter yAxis = new JIPipeExpressionParameter("");
    private Color backgroundColor = Color.BLACK;
    private Anchor anchor = Anchor.CenterCenter;

    public CanvasEqualizer() {
    }

    public CanvasEqualizer(CanvasEqualizer other) {
        this.xAxis = new JIPipeExpressionParameter(other.xAxis);
        this.yAxis = new JIPipeExpressionParameter(other.yAxis);
        this.backgroundColor = other.backgroundColor;
        this.anchor = other.anchor;
    }

    public List<ImagePlus> equalize(List<ImagePlus> input, JIPipeExpressionVariablesMap variables) {

        variables = new JIPipeExpressionVariablesMap(variables);

        int wNew = 0;
        int hNew = 0;
        for (ImagePlus image : input) {
            wNew = Math.max(image.getWidth(), wNew);
            hNew = Math.max(image.getHeight(), hNew);
        }

        variables.set("width", wNew);
        variables.set("height", hNew);
        if (!xAxis.isEmpty())
            wNew = (int) xAxis.evaluateToNumber(variables);
        if (!yAxis.isEmpty())
            hNew = (int) yAxis.evaluateToNumber(variables);

        List<ImagePlus> result = new ArrayList<>();
        for (ImagePlus imp : input) {
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

            if (imp.hasImageStack()) {
                ImagePlus resultImage = new ImagePlus(imp.getTitle() + "_Expanded", expandStack(imp.getStack(), wNew, hNew, xOff, yOff));
                resultImage.setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
                resultImage.copyScale(imp);
                result.add(resultImage);
            } else {
                ImagePlus resultImage = new ImagePlus(imp.getTitle() + "_Expanded", expandImage(imp.getProcessor(), wNew, hNew, xOff, yOff));
                resultImage.setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
                resultImage.copyScale(imp);
                result.add(resultImage);
            }
        }
        return result;
    }

    public ImageStack expandStack(ImageStack stackOld, int wNew, int hNew, int xOff, int yOff) {
        int nFrames = stackOld.getSize();
        ImageProcessor ipOld = stackOld.getProcessor(1);

        ImageStack stackNew = new ImageStack(wNew, hNew, stackOld.getColorModel());
        ImageProcessor ipNew;

        for (int i = 1; i <= nFrames; i++) {
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

    @SetJIPipeDocumentation(name = "X axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("x-axis-expression")
    @JIPipeExpressionParameterVariable(key = "width", name = "Width", description = "Calculated width of the output image")
    @JIPipeExpressionParameterVariable(key = "height", name = "Height", description = "Calculated height of the output image")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis-expression")
    public void setxAxis(JIPipeExpressionParameter xAxis) {
        this.xAxis = xAxis;
    }

    @SetJIPipeDocumentation(name = "Y axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("y-axis-expression")
    @JIPipeExpressionParameterVariable(key = "width", name = "Width", description = "Calculated width of the output image")
    @JIPipeExpressionParameterVariable(key = "height", name = "Height", description = "Calculated height of the output image")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis-expression")
    public void setyAxis(JIPipeExpressionParameter yAxis) {
        this.yAxis = yAxis;
    }

    @SetJIPipeDocumentation(name = "Background color", description = "The color of the outside canvas")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Anchor", description = "From which point to expand the canvas")
    @JIPipeParameter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }
}
