package org.hkijena.jipipe.extensions.ijweka.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
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

    @JIPipeDocumentation(name = "Classifier", description = "The selected classifier")
    @JIPipeParameter("classifier")
    public WekaClassifierParameter getClassifier() {
        return classifier;
    }

    @JIPipeParameter("classifier")
    public void setClassifier(WekaClassifierParameter classifier) {
        this.classifier = classifier;
    }

    @JIPipeDocumentation(name = "Balance classes", description = "The classifier uses by the default all the user traces to train. " +
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
