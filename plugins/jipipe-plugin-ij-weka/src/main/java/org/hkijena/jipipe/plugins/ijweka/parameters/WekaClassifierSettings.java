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

package org.hkijena.jipipe.plugins.ijweka.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class WekaClassifierSettings extends AbstractJIPipeParameterCollection {
    private WekaClassifierParameter classifier = new WekaClassifierParameter();
    private boolean balanceClasses = false;

    public WekaClassifierSettings() {
    }

    public WekaClassifierSettings(WekaClassifierSettings other) {
        this.classifier = new WekaClassifierParameter(other.classifier);
        this.balanceClasses = other.balanceClasses;
    }

    @SetJIPipeDocumentation(name = "Classifier", description = "The selected classifier")
    @JIPipeParameter("classifier")
    public WekaClassifierParameter getClassifier() {
        return classifier;
    }

    @JIPipeParameter("classifier")
    public void setClassifier(WekaClassifierParameter classifier) {
        this.classifier = classifier;
    }

    @SetJIPipeDocumentation(name = "Balance classes", description = "The classifier uses by the default all the user traces to train. " +
            "By selecting this option, we filter first the classes in order to provide a balanced distribution of the samples. " +
            "This implies that the less numerous classes will duplicate some of their samples and the more populated classes will lose some of " +
            "their samples for the sake of even distribution. This option is strongly recommended if we want to give the same importance to all classes. " +
            "An alternative is to use the Weka CostSensitiveClassifier and set a corresponding cost matrix.")
    @JIPipeParameter("balance-classes")
    public boolean isBalanceClasses() {
        return balanceClasses;
    }

    @JIPipeParameter("balance-classes")
    public void setBalanceClasses(boolean balanceClasses) {
        this.balanceClasses = balanceClasses;
    }
}
