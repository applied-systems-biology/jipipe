package org.hkijena.jipipe.extensions.imagejalgorithms.ij1;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.Pair;

public class HyperstackDimensionPair extends Pair<HyperstackDimension, HyperstackDimension> {

    public HyperstackDimensionPair() {
        super(HyperstackDimension.class, HyperstackDimension.class);
        setKey(HyperstackDimension.Depth);
        setValue(HyperstackDimension.Depth);
    }

    public HyperstackDimensionPair(HyperstackDimension first, HyperstackDimension second) {
        super(HyperstackDimension.class, HyperstackDimension.class);
        setKey(first);
        setValue(second);
    }

    public HyperstackDimensionPair(Pair<HyperstackDimension, HyperstackDimension> other) {
        super(other);
    }

    public static class List extends ListParameter<HyperstackDimensionPair> {
        public List() {
            super(HyperstackDimensionPair.class);
        }

        public List(List other) {
            super(HyperstackDimensionPair.class);
            for (HyperstackDimensionPair pair : other) {
                add(new HyperstackDimensionPair(pair));
            }
        }
    }
}
