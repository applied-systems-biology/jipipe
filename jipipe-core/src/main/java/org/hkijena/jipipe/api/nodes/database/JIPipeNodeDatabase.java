package org.hkijena.jipipe.api.nodes.database;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase {

    private static JIPipeNodeDatabase INSTANCE;
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Node database");
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
     * @param texts the texts
     * @return the tokens
     */
    public static List<String> buildTokens(List<String> texts) {
        List<String> tokens = new ArrayList<>();
        for (String text_ : texts) {
            if(StringUtils.isNullOrEmpty(text_))
                continue;
            String text = text_.toLowerCase().replace('\n', ' ');
            for (String s : text.split(" ")) {
                if(!StringUtils.isNullOrEmpty(s)) {
                    tokens.add(s);
                }
            }
        }
        return tokens;
    }

    public void rebuildImmediately() {
        queue.cancelAll();
        queue.enqueue(new JIPipeNodeDatabaseBuilderRun(this));
    }

    public synchronized void setEntries(List<JIPipeNodeDatabaseEntry> entries) {
        this.entries = entries;
    }

    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return entries;
    }

    public JIPipeRunnerQueue getQueue() {
        return queue;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeNodeDatabaseUpdater getUpdater() {
        return updater;
    }

    public static double weight(double x, double xMax) {
        return Math.exp(-Math.pow(x/xMax, 2));
    }

    public double queryTextRanking(JIPipeNodeDatabaseEntry entry, List<String> textTokens) {
        if(textTokens.isEmpty())
            return Double.POSITIVE_INFINITY;
        double rank = 0;
        for (int i = 0; i < textTokens.size(); i++) {
            String textToken = textTokens.get(i);
            double textTokenWeight = weight(i, textTokens.size());
            WeightedTokens tokens = entry.getTokens();

            double bestDistanceTokenWeight = 0;

            for (int j = 0; j < tokens.size(); j++) {
                String token = tokens.getToken(j);
                int distance = LevenshteinDistance.getDefaultInstance().apply(textToken, token);
                double tokenWeight = tokens.getWeight(j) - j / 100.0;
                if(distance >= 0) {
                    double distanceWeight = 1.0 - (1.0 * distance / Math.max(token.length(), textToken.length()));
                    double distanceTokenWeight = distanceWeight * tokenWeight;
                    if(distanceTokenWeight > bestDistanceTokenWeight) {
                        bestDistanceTokenWeight = distanceTokenWeight;
                    }
                }
            }


            rank -= textTokenWeight * bestDistanceTokenWeight;
        }
        return rank;
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew) {
        List<String> textTokens = buildTokens(Collections.singletonList(text));
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        TObjectDoubleMap<JIPipeNodeDatabaseEntry> rankMap = new TObjectDoubleHashMap<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if(entry.getRole() != role)
                continue;
            if(entry.exists() && !allowExisting)
                continue;
            if(!entry.exists() && !allowNew)
                continue;
            result.add(entry);
            rankMap.put(entry, queryTextRanking(entry, textTokens));
        }
        result.sort(Comparator.comparing(rankMap::get));
        return result;
    }

    public static synchronized JIPipeNodeDatabase getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new JIPipeNodeDatabase();
        }
        return INSTANCE;
    }
}
