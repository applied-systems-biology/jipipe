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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase {

    private static JIPipeNodeDatabase INSTANCE;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Node database");
    private final JIPipeProject project;
    private final JIPipeNodeDatabaseUpdater updater;
    private List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();

    public JIPipeNodeDatabase() {
        this(null);
    }

    public JIPipeNodeDatabase(JIPipeProject project) {
        this.project = project;
        this.updater = new JIPipeNodeDatabaseUpdater(this);
        rebuildImmediately();
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
            String text = text_.toLowerCase().replace('\n', ' ');
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

    public static synchronized JIPipeNodeDatabase getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeNodeDatabase();
        }
        return INSTANCE;
    }

    public void rebuildImmediately() {
        queue.cancelAll();
        queue.enqueue(new JIPipeNodeDatabaseBuilderRun(this));
    }

    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return entries;
    }

    public synchronized void setEntries(List<JIPipeNodeDatabaseEntry> entries) {
        this.entries = entries;
    }

    public JIPipeRunnableQueue getQueue() {
        return queue;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeNodeDatabaseUpdater getUpdater() {
        return updater;
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

            for (int j = 0; j < tokens.size(); j++) {
                String token = tokens.getToken(j);
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
            rank -= textTokenWeight * bestDistanceTokenWeight;
        }

        // rank exact name matching
        if (entry.getName().toLowerCase().startsWith(text.toLowerCase())) {
            rank *= 1.2 + text.length() / 10.0;
        }

        return rank;
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew) {
        List<String> textTokens = buildTokens(Collections.singletonList(text));
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        TObjectDoubleMap<JIPipeNodeDatabaseEntry> rankMap = new TObjectDoubleHashMap<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (entry.getRole() != role)
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
        result.sort(Comparator.comparing(rankMap::get));
        return result;
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType) {
        List<String> textTokens = buildTokens(Collections.singletonList(text));
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        TObjectDoubleMap<JIPipeNodeDatabaseEntry> rankMap = new TObjectDoubleHashMap<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (entry.getRole() != role)
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
        result.sort(Comparator.comparing(rankMap::get));
        return result;
    }
}
