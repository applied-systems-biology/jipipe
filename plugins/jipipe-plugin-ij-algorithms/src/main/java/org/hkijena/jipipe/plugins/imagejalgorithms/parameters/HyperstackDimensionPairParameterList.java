package org.hkijena.jipipe.plugins.imagejalgorithms.parameters;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class HyperstackDimensionPairParameterList extends JIPipeListParameter<HyperstackDimensionPairParameter> {
    public HyperstackDimensionPairParameterList() {
        super(HyperstackDimensionPairParameter.class);
    }

    public HyperstackDimensionPairParameterList(HyperstackDimensionPairParameterList other) {
        super(HyperstackDimensionPairParameter.class);
        for (HyperstackDimensionPairParameter pair : other) {
            add(new HyperstackDimensionPairParameter(pair));
        }
    }
}
