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

package org.hkijena.jipipe.plugins.ij3d.utils;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings;

import java.awt.*;

public class Roi3DDrawer extends AbstractJIPipeParameterCollection {
    private boolean drawOver = true;

    private double opacity = 1.0;

    private OptionalColorParameter overrideFillColor = new OptionalColorParameter(Color.RED, false);

    public Roi3DDrawer() {
    }

    public Roi3DDrawer(Roi3DDrawer other) {
        this.drawOver = other.drawOver;
        this.opacity = other.opacity;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
    }

    @SetJIPipeDocumentation(name = "Opacity", description = "Opacity of the added ROI and labels. If zero, they are not visible. If set to one, they are fully visible.")
    @JIPipeParameter("opacity")
    @NumberParameterSettings(step = 0.1)
    public double getOpacity() {
        return opacity;
    }

    @JIPipeParameter("opacity")
    public boolean setOpacity(double opacity) {
        if (opacity < 0 || opacity > 1)
            return false;
        this.opacity = opacity;
        return true;
    }

    @SetJIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @JIPipeParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @JIPipeParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }

    @SetJIPipeDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @JIPipeParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @JIPipeParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    public ImagePlus draw(ROI3DListData roi3DListData, ImagePlus referenceImage, JIPipeProgressInfo progressInfo) {

        if (referenceImage == null) {
            referenceImage = roi3DListData.createBlankCanvas("RGB", 24);
        }
        if (drawOver && referenceImage.getType() != ImagePlus.COLOR_RGB) {
            referenceImage = ImageJUtils.renderToRGBWithLUTIfNeeded(referenceImage, progressInfo.resolve("Convert to RGB"));
        }

        ImagePlus labels = roi3DListData.toLabels(referenceImage, progressInfo.resolve("Render to labels"));
        byte[][] lut = new byte[roi3DListData.size()][];
        for (int i = 0; i < roi3DListData.size(); i++) {
            ROI3D roi3D = roi3DListData.get(i);
            lut[i] = new byte[]{
                    (byte) roi3D.getFillColor().getRed(),
                    (byte) roi3D.getFillColor().getGreen(),
                    (byte) roi3D.getFillColor().getBlue()
            };
        }
        progressInfo.log("Labels to RGB ...");
        ImagePlus rgbLabels = LabelImages.labelToRgb(labels, lut, Color.BLACK);
        ImageJUtils.copyHyperstackDimensions(referenceImage, rgbLabels);

        if (drawOver) {
            ImagePlus rgbImage = ImageJUtils.duplicate(referenceImage);
            ImageJUtils.forEachIndexedZCTSlice(rgbImage, (outputIp, index) -> {
                ImageProcessor colorIp = ImageJUtils.getSliceZero(rgbLabels, index);
                int[] outputPixels = (int[]) outputIp.getPixels();
                int[] colorPixels = (int[]) colorIp.getPixels();
                for (int i = 0; i < outputPixels.length; i++) {
                    if (colorPixels[i] > 0) {
                        outputPixels[i] = ImageJUtils.rgbPixelLerp(outputPixels[i], colorPixels[i], opacity);
                    }
                }
            }, progressInfo.resolve("Blending"));
            return rgbImage;
        } else {
            return rgbLabels;
        }
    }
}
