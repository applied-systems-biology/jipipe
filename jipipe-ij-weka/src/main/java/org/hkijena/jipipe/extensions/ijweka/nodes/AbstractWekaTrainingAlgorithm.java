package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;

public abstract class AbstractWekaTrainingAlgorithm extends JIPipeMergingAlgorithm {

    private WekaClassifierParameter classifier = new WekaClassifierParameter();

    public AbstractWekaTrainingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    public AbstractWekaTrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AbstractWekaTrainingAlgorithm(AbstractWekaTrainingAlgorithm other) {
        super(other);
        this.classifier = new WekaClassifierParameter(other.classifier);
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
}
