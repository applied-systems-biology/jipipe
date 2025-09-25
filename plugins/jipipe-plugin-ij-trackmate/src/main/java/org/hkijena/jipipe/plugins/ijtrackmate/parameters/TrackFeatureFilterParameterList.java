package org.hkijena.jipipe.plugins.ijtrackmate.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class TrackFeatureFilterParameterList extends JIPipeListParameter<TrackFeatureFilterParameter> {
    public TrackFeatureFilterParameterList() {
        super(TrackFeatureFilterParameter.class);
    }

    public TrackFeatureFilterParameterList(TrackFeatureFilterParameterList other) {
        super(TrackFeatureFilterParameter.class);
        for (TrackFeatureFilterParameter parameter : other) {
            add(new TrackFeatureFilterParameter(parameter));
        }
    }

    @Override
    public TrackFeatureFilterParameter addNewInstance() {
        TrackFeatureFilterParameter parameter = new TrackFeatureFilterParameter(new TrackFeature("NUMBER_SPOTS"), 10, true);
        add(parameter);
        return parameter;
    }
}
