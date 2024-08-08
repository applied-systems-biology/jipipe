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
//    private final JIPipeLuceneNodeDatabaseSearch luceneSearch;
    private final JIPipeLegacyNodeDatabaseSearch legacySearch;


    public JIPipeNodeDatabase() {
        this(null);
    }

    public JIPipeNodeDatabase(JIPipeProject project) {
        this.project = project;
        this.updater = new JIPipeNodeDatabaseUpdater(this);
//        this.luceneSearch = new JIPipeLuceneNodeDatabaseSearch(this);
        this.legacySearch = new JIPipeLegacyNodeDatabaseSearch(this);
        rebuildImmediately();
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
//
//
//    public JIPipeLuceneNodeDatabaseSearch getLuceneSearch() {
//        return luceneSearch;
//    }

    public JIPipeLegacyNodeDatabaseSearch getLegacySearch() {
        return legacySearch;
    }
}
