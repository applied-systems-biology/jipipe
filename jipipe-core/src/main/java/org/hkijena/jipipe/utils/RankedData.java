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

import org.hkijena.jipipe.ui.components.SearchBox;

public class RankedData<T> implements Comparable<RankedData<T>>{
    private final T data;
    private final int[] rank;

    public RankedData(T data, int[] rank) {
        this.data = data;
        this.rank = rank;
    }

    public int getRankAt(int i) {
        return i < rank.length ? rank[i] : 0;
    }

    @Override
    public int compareTo(RankedData<T> o) {
        if(o.rank.length == 0 && rank.length == 0)
            return 0;
        int num = Math.min(o.rank.length, rank.length);
        for (int i = 0; i < num; i++) {
            int lhs = getRankAt(i);
            int rhs = o.getRankAt(i);
            int compare = Integer.compare(lhs, rhs);
            if(compare != 0)
                return compare;
        }
        return 0;
    }

    public T getData() {
        return data;
    }
}
