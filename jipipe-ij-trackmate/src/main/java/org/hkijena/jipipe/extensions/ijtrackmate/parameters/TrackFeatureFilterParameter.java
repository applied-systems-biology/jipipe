/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import fiji.plugin.trackmate.features.FeatureFilter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

public class TrackFeatureFilterParameter extends AbstractJIPipeParameterCollection {
    private TrackFeature feature;
    private double value;

    private boolean above = true;

    public TrackFeatureFilterParameter() {
    }

    public TrackFeatureFilterParameter(TrackFeature feature, double value, boolean above) {
        this.feature = feature;
        this.value = value;
        this.above = above;
    }

    public TrackFeatureFilterParameter(TrackFeatureFilterParameter other) {
        this.feature = other.feature;
        this.value = other.value;
        this.above = other.above;
    }

    @SetJIPipeDocumentation(name = "Feature")
    @JsonGetter("feature")
    @JIPipeParameter("feature")
    public TrackFeature getFeature() {
        return feature;
    }

    @JIPipeParameter("feature")
    @JsonSetter("feature")
    public void setFeature(TrackFeature feature) {
        this.feature = feature;
    }

    @SetJIPipeDocumentation(name = "Value")
    @JsonGetter("value")
    @JIPipeParameter("value")
    public double getValue() {
        return value;
    }

    @JIPipeParameter("value")
    @JsonSetter("value")
    public void setValue(double value) {
        this.value = value;
    }

    @SetJIPipeDocumentation(name = "Mode")
    @JsonGetter("is-above")
    @JIPipeParameter("is-above")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "[feature] > [value]", falseLabel = "[feature] < [value]")
    public boolean isAbove() {
        return above;
    }

    @JsonSetter("is-above")
    @JIPipeParameter("is-above")
    public void setAbove(boolean above) {
        this.above = above;
    }

    public FeatureFilter toFeatureFilter() {
        return new FeatureFilter(feature.getValue(), value, above);
    }

    public static class List extends ListParameter<TrackFeatureFilterParameter> {
        public List() {
            super(TrackFeatureFilterParameter.class);
        }

        public List(List other) {
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
}
