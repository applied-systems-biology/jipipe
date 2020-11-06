package org.hkijena.jipipe.api.nodes;

/**
 * Strategies that determine how to detect the columns that should be used for matching
 */
public enum JIPipeColumnGrouping {
    Union,
    Intersection,
    PrefixHashUnion,
    PrefixHashIntersection,
    MergeAll,
    SplitAll,
    Custom;


    @Override
    public String toString() {
        switch (this) {
            case Union:
                return "Use column set union";
            case Intersection:
                return "Use column set intersection";
            case PrefixHashUnion:
                return "Use columns prefixed with '#' (union)";
            case PrefixHashIntersection:
                return "Use columns prefixed with '#' (intersection)";
            case MergeAll:
                return "All into one batch";
            case SplitAll:
                return "Each into its own batch";
            case Custom:
                return "Custom (see 'Custom grouping columns')";
            default:
                return this.name();
        }
    }
}
