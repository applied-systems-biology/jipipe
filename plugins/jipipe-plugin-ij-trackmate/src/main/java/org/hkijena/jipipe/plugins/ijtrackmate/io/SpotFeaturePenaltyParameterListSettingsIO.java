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

package org.hkijena.jipipe.plugins.ijtrackmate.io;

import org.hkijena.jipipe.plugins.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.SpotFeaturePenaltyParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a Map(String, Double) to a list of {@link org.hkijena.jipipe.plugins.ijtrackmate.parameters.SpotFeaturePenaltyParameter}
 */
public class SpotFeaturePenaltyParameterListSettingsIO implements SettingsIO {
    @Override
    public Object settingToParameter(Object obj) {
        Map<String, Double> map = (Map<String, Double>) obj;
        SpotFeaturePenaltyParameter.List result = new SpotFeaturePenaltyParameter.List();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            result.add(new SpotFeaturePenaltyParameter(new SpotFeature(entry.getKey()), entry.getValue()));
        }
        return result;
    }

    @Override
    public Object parameterToSetting(Object obj) {
        Map<String, Double> result = new HashMap<>();
        SpotFeaturePenaltyParameter.List list = (SpotFeaturePenaltyParameter.List) obj;
        for (SpotFeaturePenaltyParameter parameter : list) {
            result.put(parameter.getFeature().getValue(), parameter.getPenalty());
        }
        return result;
    }

    @Override
    public Class<?> getSettingClass() {
        return Map.class;
    }

    @Override
    public Class<?> getParameterClass() {
        return SpotFeaturePenaltyParameter.List.class;
    }
}
