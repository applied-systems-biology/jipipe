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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CanvasEqualizer extends AbstractJIPipeParameterCollection {
    private DefaultExpressionParameter xAxis = new DefaultExpressionParameter("");
    private DefaultExpressionParameter yAxis = new DefaultExpressionParameter("");
    private Color backgroundColor = Color.BLACK;
    private Anchor anchor = Anchor.CenterCenter;

    public CanvasEqualizer() {
    }

    public CanvasEqualizer(CanvasEqualizer other) {
        this.xAxis = new DefaultExpressionParameter(other.xAxis);
        this.yAxis = new DefaultExpressionParameter(other.yAxis);
        this.backgroundColor = other.backgroundColor;
        this.anchor = other.anchor;
    }

    public List<ImagePlus> equalize(List<ImagePlus> input, ExpressionVariables variables) {

        variables = new ExpressionVariables(variables);

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
    @JIPipeParameter("x-axis-expression")
    @ExpressionParameterSettingsVariable(key = "width", name = "Width", description = "Calculated width of the output image")
    @ExpressionParameterSettingsVariable(key = "height", name = "Height", description = "Calculated height of the output image")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis-expression")
    public void setxAxis(DefaultExpressionParameter xAxis) {
        this.xAxis = xAxis;
    }

    @JIPipeDocumentation(name = "Y axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("y-axis-expression")
    @ExpressionParameterSettingsVariable(key = "width", name = "Width", description = "Calculated width of the output image")
    @ExpressionParameterSettingsVariable(key = "height", name = "Height", description = "Calculated height of the output image")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis-expression")
    public void setyAxis(DefaultExpressionParameter yAxis) {
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
