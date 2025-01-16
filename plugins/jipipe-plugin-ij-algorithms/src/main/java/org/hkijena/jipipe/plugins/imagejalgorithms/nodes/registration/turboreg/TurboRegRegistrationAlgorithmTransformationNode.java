package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegTransformationInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

class TurboRegRegistrationAlgorithmTransformationNode {
    private final ImageSliceIndex sourceIndex;
    private final TurboRegRegistrationAlgorithmRule rule;
    private TurboRegTransformationInfo transformationInfo;

    TurboRegRegistrationAlgorithmTransformationNode(ImageSliceIndex sourceIndex, TurboRegRegistrationAlgorithmRule rule) {
        this.sourceIndex = sourceIndex;
        this.rule = rule;
    }

    public TurboRegRegistrationAlgorithmRule getRule() {
        return rule;
    }

    public ImageSliceIndex getSourceIndex() {
        return sourceIndex;
    }

    public TurboRegTransformationInfo getTransformationInfo() {
        return transformationInfo;
    }

    public void setTransformationInfo(TurboRegTransformationInfo transformationInfo) {
        this.transformationInfo = transformationInfo;
    }
}
