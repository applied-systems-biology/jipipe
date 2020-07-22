package org.hkijena.jipipe.api.nodes;

/**
 * Strategies that determine how to detect the columns that should be used for matching
 */
public enum JIPipeColumnGrouping {
    Union,
    Intersection,
    Custom;


    @Override
    public String toString() {
        switch (this) {
            case Union:
                return "Automated: Use column set union";
            case Intersection:
                return "Automated: Use column set intersection";
            default:
                return this.name();
        }
    }
}
