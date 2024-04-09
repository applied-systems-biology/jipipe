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
