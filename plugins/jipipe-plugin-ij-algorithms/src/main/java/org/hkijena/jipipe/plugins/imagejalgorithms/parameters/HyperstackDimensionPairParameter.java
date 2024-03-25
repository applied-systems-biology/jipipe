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

package org.hkijena.jipipe.plugins.imagejalgorithms.parameters;

import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;

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
