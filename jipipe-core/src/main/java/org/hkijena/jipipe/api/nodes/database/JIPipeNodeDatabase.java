package org.hkijena.jipipe.api.nodes.database;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
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

    public double queryTextRanking(JIPipeNodeDatabaseEntry entry, String text) {
        if(StringUtils.isNullOrEmpty(text))
            return Double.POSITIVE_INFINITY;
        text = text.toLowerCase();
        boolean match = false;
        double rank = 0;
        List<String> tokens = entry.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            String token = StringUtils.nullToEmpty(tokens.get(i)).toLowerCase();
            int index = token.indexOf(text);
            double tokenWeight = 1.0 - (1.0 * i / (tokens.size() - 1)); // Weight earlier tokens heigher
            if(index >= 0) {
                match = true;
                double indexWeight = 1.0 - (1.0 * index / token.length());
                rank -= indexWeight * tokenWeight;
            }
        }
        if(match) {
            return rank;
        }
        else {
            return Double.POSITIVE_INFINITY;
        }
    }

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew) {
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
            rankMap.put(entry, queryTextRanking(entry, text));
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
