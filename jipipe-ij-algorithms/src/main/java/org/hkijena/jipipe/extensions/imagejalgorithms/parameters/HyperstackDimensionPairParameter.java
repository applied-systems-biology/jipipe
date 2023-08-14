package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;

public class HyperstackDimensionPairParameter extends PairParameter<HyperstackDimension, HyperstackDimension> {

    public HyperstackDimensionPairParameter() {
        super(HyperstackDimension.class, HyperstackDimension.class);
        setKey(HyperstackDimension.Depth);
        setValue(HyperstackDimension.Depth);
    }

    public HyperstackDimensionPairParameter(HyperstackDimension first, HyperstackDimension second) {
        super(HyperstackDimension.class, HyperstackDimension.class);
        setKey(first);
        setValue(second);
    }

    public HyperstackDimensionPairParameter(PairParameter<HyperstackDimension, HyperstackDimension> other) {
        super(other);
    }

    public static class List extends ListParameter<HyperstackDimensionPairParameter> {
        public List() {
            super(HyperstackDimensionPairParameter.class);
        }

        public List(List other) {
            super(HyperstackDimensionPairParameter.class);
            for (HyperstackDimensionPairParameter pair : other) {
                add(new HyperstackDimensionPairParameter(pair));
            }
        }
    }
}