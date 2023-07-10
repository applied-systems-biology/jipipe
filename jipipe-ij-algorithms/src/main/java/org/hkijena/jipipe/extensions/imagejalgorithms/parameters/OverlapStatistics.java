package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

public enum OverlapStatistics {
    TotalOverlap,
    JaccardIndex,
    DiceCoefficient,
    VolumeSimilarity,
    FalseNegativeError,
    FalsePositiveError;


    @Override
    public String toString() {
        switch (this) {
            case TotalOverlap:
                return "Total Overlap";
            case JaccardIndex:
                return "Jaccard Index";
            case DiceCoefficient:
                return "Dice Coefficient";
            case VolumeSimilarity:
                return "Volume Similarity";
            case FalseNegativeError:
                return "False Negative Error";
            case FalsePositiveError:
                return "False Positive Error";
            default:
                return super.toString();
        }
    }
}
