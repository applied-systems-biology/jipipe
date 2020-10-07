package org.hkijena.jipipe.api.nodes;

/**
 * Strategies that determine how to detect the columns that should be used for matching
 */
public enum JIPipeColumnGrouping {
    Union,
    Intersection,
    PrefixHashUnion,
    PrefixHashIntersection,
    Custom;


    @Override
    public String toString() {
        switch (this) {
            case Union:
                return "Automated: Use column set union";
            case Intersection:
                return "Automated: Use column set intersection";
            case PrefixHashUnion:
                return "Automated: Use columns prefixed with '#' (union)";
            case PrefixHashIntersection:
                return "Automated: Use columns prefixed with '#' (intersection)";
            default:
                return this.name();
        }
    }
}
