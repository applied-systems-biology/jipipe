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

package org.hkijena.jipipe.extensions.parameters.library.util;

import java.util.Collection;

/**
 * Available operations
 */
public enum LogicalOperation {
    LogicalOr,
    LogicalAnd,
    LogicalXor;

    /**
     * Apply the logical operation for the items
     *
     * @param items the items
     * @return result
     */
    public boolean apply(Collection<Boolean> items) {
        switch (this) {
            case LogicalOr:
                return items.contains(true);
            case LogicalAnd:
                return items.stream().allMatch(x -> x);
            case LogicalXor:
                return items.contains(true) && items.stream().distinct().count() > 1;
            default:
                throw new UnsupportedOperationException("Unsupported: " + this);
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case LogicalOr:
                return "OR";
            case LogicalAnd:
                return "AND";
            case LogicalXor:
                return "XOR";
            default:
                throw new UnsupportedOperationException();
        }
    }
}
