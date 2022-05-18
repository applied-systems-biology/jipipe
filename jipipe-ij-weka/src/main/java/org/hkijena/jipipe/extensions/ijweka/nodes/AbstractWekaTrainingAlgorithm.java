package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierSettings;

public abstract class AbstractWekaTrainingAlgorithm extends JIPipeMergingAlgorithm {

    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();

    public AbstractWekaTrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AbstractWekaTrainingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    public AbstractWekaTrainingAlgorithm(AbstractWekaTrainingAlgorithm other) {
        super(other);
        this.classifierSettings = new WekaClassifierSettings(other.classifierSettings);
    }

    @JIPipeDocumentation(name = "Classifier", description = "Settings for the classifier")
    @JIPipeParameter("classifier-settings")
    public WekaClassifierSettings getClassifierSettings() {
        return classifierSettings;
    }
}
