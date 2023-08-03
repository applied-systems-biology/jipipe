package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase {
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

    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew) {
        // TODO
        return entries;
    }
}
