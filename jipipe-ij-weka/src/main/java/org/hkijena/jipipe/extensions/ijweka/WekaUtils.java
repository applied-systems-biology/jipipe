package org.hkijena.jipipe.extensions.ijweka;

import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature3D;
import trainableSegmentation.FeatureStack3D;
import trainableSegmentation.WekaSegmentation;

import java.util.Set;
import java.util.stream.Collectors;

public class WekaUtils {

    /**
     * Reliably sets the 3D features
     *
     * @param wekaSegmentation the segmentation
     * @param values           the features
     */
    public static void set3DFeatures(WekaSegmentation wekaSegmentation, Set<WekaFeature3D> values) {
        Set<String> names = values.stream().map(WekaFeature3D::name).collect(Collectors.toSet());
        boolean[] enabledFeatures = new boolean[FeatureStack3D.availableFeatures.length];
        for (int i = 0; i < enabledFeatures.length; i++) {
            if (names.contains(FeatureStack3D.availableFeatures[i]))
                enabledFeatures[i] = true;
        }
        wekaSegmentation.setEnabledFeatures(enabledFeatures);
    }
}
