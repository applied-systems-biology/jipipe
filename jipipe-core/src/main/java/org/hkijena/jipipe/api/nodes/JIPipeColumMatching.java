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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

/**
 * Strategies that determine how to detect the columns that should be used for matching
 */
@EnumParameterSettings(itemInfo = JIPipeColumnMatchingEnumInfo.class)
public enum JIPipeColumMatching {
    Union,
    Intersection,
    PrefixHashUnion,
    PrefixHashIntersection,
    MergeAll,
    SplitAll,
    None,
    Custom;


    @Override
    public String toString() {
        switch (this) {
            case Union:
                return "Use column set union";
            case Intersection:
                return "Use column set intersection";
            case PrefixHashUnion:
                return "Use columns prefixed with '#' (union, default)";
            case PrefixHashIntersection:
                return "Use columns prefixed with '#' (intersection)";
            case MergeAll:
                return "All into one batch";
            case SplitAll:
                return "Each into its own batch";
            case None:
                return "No column (multiply inputs)";
            case Custom:
                return "Custom (see 'Custom grouping columns')";
            default:
                return this.name();
        }
    }
}
