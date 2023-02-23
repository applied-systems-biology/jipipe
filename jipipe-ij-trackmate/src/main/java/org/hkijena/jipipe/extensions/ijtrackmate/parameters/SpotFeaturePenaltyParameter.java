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
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

public class SpotFeaturePenaltyParameter implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private SpotFeature feature;
    private double penalty;

    public SpotFeaturePenaltyParameter() {
    }

    public SpotFeaturePenaltyParameter(SpotFeature feature, double penalty) {
        this.feature = feature;
        this.penalty = penalty;
    }

    public SpotFeaturePenaltyParameter(SpotFeaturePenaltyParameter other) {
        this.feature = other.feature;
        this.penalty = other.penalty;
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

    @JIPipeDocumentation(name = "Penalty")
    @JsonGetter("penalty")
    @JIPipeParameter("penalty")
    public double getPenalty() {
        return penalty;
    }

    @JIPipeParameter("penalty")
    @JsonSetter("penalty")
    public void setPenalty(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static class List extends ListParameter<SpotFeaturePenaltyParameter> {
        public List() {
            super(SpotFeaturePenaltyParameter.class);
        }

        public List(List other) {
            super(SpotFeaturePenaltyParameter.class);
            for (SpotFeaturePenaltyParameter parameter : other) {
                add(new SpotFeaturePenaltyParameter(parameter));
            }
        }

        @Override
        public SpotFeaturePenaltyParameter addNewInstance() {
            return new SpotFeaturePenaltyParameter(new SpotFeature("QUALITY"), 1.0);
        }
    }
}
