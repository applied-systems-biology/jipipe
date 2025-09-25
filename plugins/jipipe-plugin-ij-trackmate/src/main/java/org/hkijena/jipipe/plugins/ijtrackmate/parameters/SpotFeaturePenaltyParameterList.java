package org.hkijena.jipipe.plugins.ijtrackmate.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class SpotFeaturePenaltyParameterList extends JIPipeListParameter<SpotFeaturePenaltyParameter> {
    public SpotFeaturePenaltyParameterList() {
        super(SpotFeaturePenaltyParameter.class);
    }

    public SpotFeaturePenaltyParameterList(SpotFeaturePenaltyParameterList other) {
        super(SpotFeaturePenaltyParameter.class);
        for (SpotFeaturePenaltyParameter parameter : other) {
            add(new SpotFeaturePenaltyParameter(parameter));
        }
    }

    @Override
    public SpotFeaturePenaltyParameter addNewInstance() {
        SpotFeaturePenaltyParameter quality = new SpotFeaturePenaltyParameter(new SpotFeature("QUALITY"), 1.0);
        add(quality);
        return quality;
    }
}
