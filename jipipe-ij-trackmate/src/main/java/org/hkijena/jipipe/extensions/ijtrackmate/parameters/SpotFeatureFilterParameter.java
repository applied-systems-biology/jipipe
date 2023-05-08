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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

public class SpotFeatureFilterParameter extends AbstractJIPipeParameterCollection {
    private SpotFeature feature;
    private double value;

    private boolean above = true;

    public SpotFeatureFilterParameter() {
    }

    public SpotFeatureFilterParameter(SpotFeature feature, double value, boolean above) {
        this.feature = feature;
        this.value = value;
        this.above = above;
    }

    public SpotFeatureFilterParameter(SpotFeatureFilterParameter other) {
        this.feature = other.feature;
        this.value = other.value;
        this.above = other.above;
    }

    @JIPipeDocumentation(name = "Feature")
    @JsonGetter("feature")
    @JIPipeParameter("feature")
    public SpotFeature getFeature() {
        return feature;
    }

    @JIPipeParameter("feature")
    @JsonSetter("feature")
    public void setFeature(SpotFeature feature) {
        this.feature = feature;
    }

    @JIPipeDocumentation(name = "Value")
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

    @JIPipeDocumentation(name = "Mode")
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

    public static class List extends ListParameter<SpotFeatureFilterParameter> {
        public List() {
            super(SpotFeatureFilterParameter.class);
        }

        public List(List other) {
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
}
