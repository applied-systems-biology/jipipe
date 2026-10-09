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

package org.hkijena.jipipe.api.nodes.database;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightedTokens {

    public static final int WEIGHT_NAME = 8;

    /**
     * The weight of the primary function leaf and each secondary
     * function name in the taxonomy function path (the node's main
     * search term alongside the node name).
     */
    public static final int WEIGHT_FUNCTION_LEAF = 6;

    /**
     * The weight of the direct parent of a taxonomy function
     * (depth 1 of the {@code getParent()} chain walked up from a leaf).
     * Each ancestor level walks down by 2, floored at
     * {@link #WEIGHT_FUNCTION_TOP}.
     */
    public static final int WEIGHT_FUNCTION_PARENT = 4;

    /**
     * The floor weight of a taxonomy function ancestor
     * (grandparent and deeper in the {@code getParent()} chain).
     */
    public static final int WEIGHT_FUNCTION_TOP = 2;

    /**
     * The weight of the taxonomy framework's display name.
     */
    public static final int WEIGHT_FRAMEWORK = 6;

    /**
     * The weight of the taxonomy framework's abbreviation.
     */
    public static final int WEIGHT_FRAMEWORK_ABBREV = 5;

    /**
     * The weight of each taxonomy tag.
     */
    public static final int WEIGHT_TAG = 6;

    public static final int WEIGHT_MENU = 2;
    public static final int WEIGHT_DESCRIPTION = 1;

    public final List<String> tokens = new ArrayList<>();
    public final TIntList weights = new TIntArrayList();

    /**
     * Adds a text to tokenize
     *
     * @param text   the text
     * @param weight the weight
     */
    public void add(String text, int weight) {
        if (StringUtils.isNullOrEmpty(text))
            return;
        text = text.toLowerCase(Locale.ROOT).replace('\n', ' ');
        for (String s : text.split(" ")) {
            if (!StringUtils.isNullOrEmpty(s)) {
                tokens.add(s);
                weights.add(weight);
            }
        }
    }

    public String getToken(int index) {
        return tokens.get(index);
    }

    public int getWeight(int index) {
        return weights.get(index);
    }

    /**
     * Checks whether a token with the given (already lowercased) text is present.
     *
     * @param token the token text
     * @return true if the token is present
     */
    public boolean contains(String token) {
        return tokens.contains(token);
    }

    public int size() {
        return tokens.size();
    }
}
