/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils;

/**
 * Models a search ranking function
 * @param <T> the items
 */
public interface RankingFunction<T> {
    /**
     * Ranks the value in conjunction with the filter strings
     * @param value the value
     * @param filterStrings the filter strings (can be null or empty)
     * @return a rank for each category. each item represents the ranking score (lower values are higher ranks) for a column. if null, the item does not match
     */
    int[] rank(T value, String[] filterStrings);
}
