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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class OrientationJStructureTensorParameters extends AbstractJIPipeParameterCollection {
    private int localWindowSigma = 2;
    private OrientationJGradientOperator gradient = OrientationJGradientOperator.CubicSpline;

    public OrientationJStructureTensorParameters() {
    }

    public OrientationJStructureTensorParameters(OrientationJStructureTensorParameters other) {
        this.localWindowSigma = other.localWindowSigma;
        this.gradient = other.gradient;
    }

    @SetJIPipeDocumentation(name = "Local window σ", description = "The structure tensor computes the orientation and isotropy properties in a local window. Here, the local window is characterized by a 2D Gaussian function of standard deviation σ. " +
            "The parameter σ (expressed in pixel unit) is a critical parameter that determines the scale of the analysis. It should have a value roughly close to the structure of interest (e.g. thickness of the filament)")
    @JIPipeParameter("local-window-sigma")
    public int getLocalWindowSigma() {
        return localWindowSigma;
    }

    @JIPipeParameter("local-window-sigma")
    public void setLocalWindowSigma(int localWindowSigma) {
        this.localWindowSigma = localWindowSigma;
    }

    @SetJIPipeDocumentation(name = "Gradient", description = "OrientationJ has different methods to compute the gradient.\n" +
            "<ul>" +
            "<li>The cubic spline is always the best choice, fast, accurate, quasi-isotropic and less boundary artefact</li>\n" +
            "<li>The finite difference gradient is very fast but it has poor isotropy properties</li>\n" +
            "<li>The Fourier gradient is exact but it has periodic boundary conditions.</li>" +
            "</ul>")
    @JIPipeParameter("gradient")
    public OrientationJGradientOperator getGradient() {
        return gradient;
    }

    @JIPipeParameter("gradient")
    public void setGradient(OrientationJGradientOperator gradient) {
        this.gradient = gradient;
    }


}
