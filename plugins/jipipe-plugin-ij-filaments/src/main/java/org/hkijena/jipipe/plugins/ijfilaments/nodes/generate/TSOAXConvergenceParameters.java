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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.generate;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class TSOAXConvergenceParameters extends AbstractJIPipeParameterCollection {
    private int minimumSnakeLength = 10;
    private int maximumIterations = 10000;
    private int checkPeriod = 100;
    private double changeThreshold = 0.1;

    public TSOAXConvergenceParameters() {
    }

    public TSOAXConvergenceParameters(TSOAXConvergenceParameters other) {
        this.minimumSnakeLength = other.minimumSnakeLength;
        this.maximumIterations = other.maximumIterations;
        this.checkPeriod = other.checkPeriod;
        this.changeThreshold = other.changeThreshold;
    }

    @SetJIPipeDocumentation(name = "Minimum snake length", description = "(in pixels) specifies the minimum length of a resultant " +
            "snake. Increase the value to eliminate hair-like snake structures as well as to avoid the snakes picking " +
            "up actin patches in yeast images")
    @JIPipeParameter(value = "minimum-snake-length", important = true)
    public int getMinimumSnakeLength() {
        return minimumSnakeLength;
    }

    @JIPipeParameter("minimum-snake-length")
    public void setMinimumSnakeLength(int minimumSnakeLength) {
        this.minimumSnakeLength = minimumSnakeLength;
    }

    @SetJIPipeDocumentation(name = "Maximum iterations", description = "Specifies the maximum number of iterations allowed in each snake evolution")
    @JIPipeParameter("maximum-iterations")
    public int getMaximumIterations() {
        return maximumIterations;
    }

    @JIPipeParameter("maximum-iterations")
    public void setMaximumIterations(int maximumIterations) {
        this.maximumIterations = maximumIterations;
    }

    @SetJIPipeDocumentation(name = "Check period", description = "Specifies the cycle of checking for convergence in number of iterations. A value of 100 means " +
            "a snake is checked for convergence every 100 iterations. (see the last sentence of Section 2.1 in https://doi.org/10.1016/j.media.2013.10.015)")
    @JIPipeParameter("check-period")
    public int getCheckPeriod() {
        return checkPeriod;
    }

    @JIPipeParameter("check-period")
    public void setCheckPeriod(int checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    @SetJIPipeDocumentation(name = "Change threshold", description = "(in pixels) specifies the threshold of change for a snake to be converged. A value of " +
            "0.05 means a snake is converged if every snake point drifts less than 0.05 pixels since last check for " +
            "convergence (see the last sentence of Section 2.1 in https://doi.org/10.1016/j.media.2013.10.015)")
    @JIPipeParameter("change-threshold")
    public double getChangeThreshold() {
        return changeThreshold;
    }

    @JIPipeParameter("change-threshold")
    public void setChangeThreshold(double changeThreshold) {
        this.changeThreshold = changeThreshold;
    }
}
