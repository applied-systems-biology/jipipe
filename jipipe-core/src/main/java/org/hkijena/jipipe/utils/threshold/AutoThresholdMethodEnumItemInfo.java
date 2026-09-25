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

package org.hkijena.jipipe.utils.threshold;

import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;

import javax.swing.*;

/**
 * Renders per-method documentation for {@link AutoThresholdMethod}.
 */
public class AutoThresholdMethodEnumItemInfo implements JIPipeEnumParameterItemInfo {

    private static String description(AutoThresholdMethod method) {
        switch (method) {
            case Default:
                return "<b>Default</b><br>The IsoData method used by ImageJ's Threshold widget, "
                        + "with a correction for dominant peaks. Robust general-purpose default.";
            case Huang:
                return "<b>Huang</b><br>Huang's fuzzy thresholding using Shannon entropy. "
                        + "Slower on dense histograms (iterative per-bin entropy).";
            case Intermodes:
                return "<b>Intermodes</b><br>Assumes a bimodal histogram; iteratively smooths it "
                        + "until two maxima remain. The threshold is the mean of the two modes.";
            case IsoData:
                return "<b>IsoData</b><br>Iterative inter-means thresholding (Ridler &amp; Calvard).";
            case IJ_IsoData:
                return "<b>IJ IsoData</b><br>The original ImageJ IsoData implementation (kept for "
                        + "backward compatibility).";
            case Li:
                return "<b>Li</b><br>Li's minimum cross-entropy thresholding (iterative).";
            case MaxEntropy:
                return "<b>MaxEntropy</b><br>Kapur-Sahoo-Wong maximum entropy thresholding.";
            case Mean:
                return "<b>Mean</b><br>The threshold is the mean of the histogram values.";
            case MinError:
                return "<b>MinError</b><br>Kittler-Illingworth minimum error thresholding (iterative).";
            case Minimum:
                return "<b>Minimum</b><br>Assumes a bimodal histogram; iteratively smooths it until "
                        + "two maxima remain. The threshold is the minimum between the modes.";
            case Moments:
                return "<b>Moments</b><br>Tsai's moment-preserving thresholding.";
            case Otsu:
                return "<b>Otsu</b><br>Otsu's between-class variance maximization.";
            case Percentile:
                return "<b>Percentile</b><br>Doyle's percentile thresholding (50% foreground).";
            case RenyiEntropy:
                return "<b>RenyiEntropy</b><br>Renyi-entropy based thresholding (uses alpha = 0.5, 1, 2).";
            case Shanbhag:
                return "<b>Shanbhag</b><br>Shanbhag's information-measure thresholding.";
            case Triangle:
                return "<b>Triangle</b><br>Zack-Rogers-Latt triangle algorithm; skew-corrected. "
                        + "Suited for images with a dominant peak at one end.";
            case Yen:
                return "<b>Yen</b><br>Yen's maximum entropy criterion thresholding.";
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return null;
    }

    @Override
    public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return value.toString();
    }

    @Override
    public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        if (value instanceof AutoThresholdMethod)
            return "<html>" + description((AutoThresholdMethod) value) + "</html>";
        return null;
    }
}
