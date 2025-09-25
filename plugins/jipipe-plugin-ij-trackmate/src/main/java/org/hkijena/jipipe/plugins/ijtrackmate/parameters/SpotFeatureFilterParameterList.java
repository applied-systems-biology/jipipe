package org.hkijena.jipipe.plugins.ijtrackmate.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class SpotFeatureFilterParameterList extends JIPipeListParameter<SpotFeatureFilterParameter> {
    public SpotFeatureFilterParameterList() {
        super(SpotFeatureFilterParameter.class);
    }

    public SpotFeatureFilterParameterList(SpotFeatureFilterParameterList other) {
        super(SpotFeatureFilterParameter.class);
        for (SpotFeatureFilterParameter parameter : other) {
            add(new SpotFeatureFilterParameter(parameter));
        }
    }

    @Override
    public SpotFeatureFilterParameter addNewInstance() {
        SpotFeatureFilterParameter parameter = new SpotFeatureFilterParameter(new SpotFeature("QUALITY"), 1.0, true);
        add(parameter);
        return parameter;
    }
}
