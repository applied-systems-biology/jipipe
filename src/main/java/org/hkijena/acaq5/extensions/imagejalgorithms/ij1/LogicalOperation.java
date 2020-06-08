package org.hkijena.acaq5.extensions.imagejalgorithms.ij1;

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
}
