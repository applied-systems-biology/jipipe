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

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

public class JIPipeLegacyNodeDatabaseSearch {
    private final JIPipeNodeDatabase nodeDatabase;

    public JIPipeLegacyNodeDatabaseSearch(JIPipeNodeDatabase nodeDatabase) {
        this.nodeDatabase = nodeDatabase;
    }

    /**
     * Converts a list of texts into word tokens
     *
     * @param texts the texts
     * @return the tokens
     */
    public static List<String> buildTokens(List<String> texts) {
        List<String> tokens = new ArrayList<>();
        for (String text_ : texts) {
            if (StringUtils.isNullOrEmpty(text_))
                continue;
            String text = text_.toLowerCase(Locale.ROOT).replace('\n', ' ');
            for (String s : text.split(" ")) {
                if (!StringUtils.isNullOrEmpty(s)) {
                    tokens.add(s);
                }
            }
        }
        return tokens;
    }

    public static double weight(double x, double xMax) {
        return Math.exp(-Math.pow(x / xMax, 2));
    }

    public static int dataTypeDistance(Class<? extends JIPipeData> from, Class<? extends JIPipeData> to) {
        if (from == to) {
            return 0;
        } else if (to.isAssignableFrom(from)) {
            return ReflectionUtils.getClassDistance(to, from);
        } else {
            return JIPipe.getDataTypes().getConversionDistance(from, to) * 5; // Weight these higher
        }
    }

    public double queryTextRanking(JIPipeNodeDatabaseEntry entry, String text, List<String> textTokens) {
        if (textTokens.isEmpty())
            return Double.POSITIVE_INFINITY;
        double rank = 0;
        WeightedTokens tokens = entry.getTokens();


        for (int i = 0; i < textTokens.size(); i++) {
            String textToken = textTokens.get(i);
            double textTokenWeight = weight(i, textTokens.size());
            double bestDistanceTokenWeight = 0;
            boolean foundToken = false;

            for (int j = 0; j < tokens.size(); j++) {
                String token = tokens.getToken(j);

                if (token.toLowerCase(Locale.ROOT).contains(textToken.toLowerCase(Locale.ROOT))) {
                    foundToken = true;
                }

                int distance = LevenshteinDistance.getDefaultInstance().apply(textToken, token);
                double tokenWeight = tokens.getWeight(j) - j / 100.0;
                if (distance >= 0) {
                    double distanceWeight = 1.0 - (1.0 * distance / Math.max(token.length(), textToken.length()));
                    double distanceTokenWeight = distanceWeight * tokenWeight;
                    if (distanceTokenWeight > bestDistanceTokenWeight) {
                        bestDistanceTokenWeight = distanceTokenWeight;
                    }
                }
            }

            if (!foundToken) {
                rank += 1.0; // Add penalty instead of rejecting
            }

            rank -= textTokenWeight * bestDistanceTokenWeight;
        }

        // rank exact name matching
        if (entry.getName().toLowerCase(Locale.ROOT).startsWith(text.toLowerCase(Locale.ROOT))) {
            rank *= 1.2 + text.length() / 10.0;
        }


        return rank;
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds) {
        List<String> textTokens = buildTokens(Collections.singletonList(text));
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        TObjectDoubleMap<JIPipeNodeDatabaseEntry> rankMap = new TObjectDoubleHashMap<>();

        for (JIPipeNodeDatabaseEntry entry : nodeDatabase.getEntries()) {
            if (!entry.getVisibility().matches(role))
                continue;
            if (entry.exists() && !allowExisting)
                continue;
            if (!entry.exists() && !allowNew)
                continue;
            result.add(entry);


            double ranking = queryTextRanking(entry, text, textTokens);
            if (entry.isDeprecated()) {
                ranking *= 0.8;
            }

            rankMap.put(entry, ranking);
        }

        if (textTokens.isEmpty()) {
            result.sort(Comparator.comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId())).thenComparing(JIPipeNodeDatabaseEntry::getName));
        } else {
            result.removeIf(e -> rankMap.get(e) >= 0);
            result.sort(Comparator.comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId())).thenComparing(rankMap::get));
        }
        return result;
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType) {
        Set<String> pinnedIds = new HashSet<>(); // TODO: implement pinned Ids
        List<String> textTokens = buildTokens(Collections.singletonList(text));
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        TObjectDoubleMap<JIPipeNodeDatabaseEntry> rankMap = new TObjectDoubleHashMap<>();
        for (JIPipeNodeDatabaseEntry entry : nodeDatabase.getEntries()) {
            if (!entry.getVisibility().matches(role))
                continue;
            if (entry.exists() && !allowExisting)
                continue;
            if (!entry.exists() && !allowNew)
                continue;

            int bestConversionDistance = Integer.MAX_VALUE;
            if (targetSlotType == JIPipeSlotType.Input) {
                for (Map.Entry<String, JIPipeDataSlotInfo> slotInfoEntry : entry.getOutputSlots().entrySet()) {
                    int conversionDistance = dataTypeDistance(slotInfoEntry.getValue().getDataClass(), targetDataType);
                    if (conversionDistance >= 0 && conversionDistance < bestConversionDistance) {
                        bestConversionDistance = conversionDistance;
                    }
                }
            } else {
                for (Map.Entry<String, JIPipeDataSlotInfo> slotInfoEntry : entry.getInputSlots().entrySet()) {
                    int conversionDistance = dataTypeDistance(targetDataType, slotInfoEntry.getValue().getDataClass());
                    if (conversionDistance >= 0 && conversionDistance < bestConversionDistance) {
                        bestConversionDistance = conversionDistance;
                    }
                }
            }

            if (bestConversionDistance == Integer.MAX_VALUE) {
                continue;
            }

            double textRanking = queryTextRanking(entry, text, textTokens);
            double dataTypeRanking = -weight(bestConversionDistance, 5);
            double ranking = textTokens.isEmpty() ? dataTypeRanking : textRanking;
            if (entry.isDeprecated()) {
                ranking *= 0.8;
            }

            result.add(entry);
            rankMap.put(entry, ranking);
        }

        if (textTokens.isEmpty()) {
            result.sort(Comparator.comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId())).thenComparing(JIPipeNodeDatabaseEntry::getName));
        } else {
            result.removeIf(e -> rankMap.get(e) >= 0);
            result.sort(Comparator.comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId())).thenComparing(rankMap::get));
        }

        return result;
    }
}
