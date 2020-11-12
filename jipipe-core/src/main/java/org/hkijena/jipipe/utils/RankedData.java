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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RankedData<T> implements Comparable<RankedData<T>> {
    private final T data;
    private final String dataString;
    private final int[] rank;

    public RankedData(T data, String dataString, int[] rank) {
        this.data = data;
        this.dataString = dataString;
        this.rank = rank;
    }

    public int getRankAt(int i) {
        return i < rank.length ? rank[i] : 0;
    }

    @Override
    public int compareTo(RankedData<T> o) {
        if (o.rank.length == 0 && rank.length == 0)
            return 0;
        int num = Math.min(o.rank.length, rank.length);
        for (int i = 0; i < num; i++) {
            int lhs = getRankAt(i);
            int rhs = o.getRankAt(i);
            int compare = Integer.compare(lhs, rhs);
            if (compare != 0)
                return compare;
        }
        return Integer.compare(dataString.length(), o.dataString.length());
    }

    public T getData() {
        return data;
    }

    /**
     * Creates a list of sorted ranked data
     *
     * @param <T>             the data type
     * @param data            the data
     * @param dataToString    converts the data to string
     * @param rankingFunction the ranking function
     * @param searchStrings   the search terms. If null or empty, all data gets an empty rank
     * @return ranked data, sorted according to the rank
     */
    public static <T> List<T> getSortedAndFilteredData(Collection<T> data, Function<T, String> dataToString, RankingFunction<T> rankingFunction, String[] searchStrings) {
        int maxRankLength = 0;

        if (searchStrings == null || searchStrings.length == 0) {
            return new ArrayList<>(data);
        } else {
            List<RankedData<T>> rankedData = new ArrayList<>();
            for (T item : data) {
                int[] rank = rankingFunction.rank(item, searchStrings);
                if (rank != null) {
                    rankedData.add(new RankedData<>(item, dataToString.apply(item), rank));
                    maxRankLength = Math.max(maxRankLength, rank.length);
                }
            }
            // Sort according to rank
            if (maxRankLength > 0) {
                rankedData.sort(Comparator.naturalOrder());
            }
            return rankedData.stream().map(RankedData::getData).collect(Collectors.toList());
        }
    }
}
