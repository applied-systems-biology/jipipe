package org.hkijena.jipipe.extensions.ijweka;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.StringUtils;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStack3D;
import trainableSegmentation.WekaSegmentation;

import java.util.*;
import java.util.stream.Collectors;

public class WekaUtils {

    /**
     * Reliably sets the 3D features
     * @param wekaSegmentation the segmentation
     * @param values the features
     */
    public static void set3DFeatures(WekaSegmentation wekaSegmentation, Set<WekaFeature3D> values) {
        Set<String> names = values.stream().map(WekaFeature3D::name).collect(Collectors.toSet());
        boolean[] enabledFeatures = new boolean[FeatureStack3D.availableFeatures.length];
        for (int i = 0; i < enabledFeatures.length; i++) {
            if(names.contains(FeatureStack3D.availableFeatures[i]))
                enabledFeatures[i] = true;
        }
        wekaSegmentation.setEnabledFeatures(enabledFeatures);
    }
}
