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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JIPipeNodeDatabaseBuilderRun extends AbstractJIPipeRunnable {

    private static List<JIPipeNodeDatabaseEntry> CACHED_GLOBAL_ENTRIES;
    private final JIPipeNodeDatabase database;

    public JIPipeNodeDatabaseBuilderRun(JIPipeNodeDatabase database) {
        this.database = database;
    }

    @Override
    public String getTaskLabel() {
        return "Rebuild node database";
    }

    @Override
    public void run() {
        JIPipeNodeRegistry nodeRegistry = JIPipe.getNodes();
        List<JIPipeNodeDatabaseEntry> newEntries = new ArrayList<>();

        if (CACHED_GLOBAL_ENTRIES == null) {
            CACHED_GLOBAL_ENTRIES = new ArrayList<>();
            // Add creation of nodes by info
            for (Map.Entry<String, JIPipeNodeInfo> entry : nodeRegistry.getRegisteredNodeInfos().entrySet()) {
                JIPipeNodeInfo nodeInfo = entry.getValue();
                if (nodeInfo.isHidden() || nodeInfo.getCategory() instanceof InternalNodeTypeCategory) {
                    continue;
                }
                {
                    CreateNewNodeByInfoDatabaseEntry newEntry = new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + entry.getKey(), nodeInfo);
                    newEntries.add(newEntry);
                    CACHED_GLOBAL_ENTRIES.add(newEntry);
                }
                // Add node example creation
                ArrayList<JIPipeNodeExample> examples = new ArrayList<>(nodeRegistry.getNodeExamples(entry.getKey()));
                for (int i = 0; i < examples.size(); i++) {
                    JIPipeNodeExample example = examples.get(i);
                    CreateNewNodeByExampleDatabaseEntry newEntry = new CreateNewNodeByExampleDatabaseEntry("create-node-by-example:" + entry.getKey() + ":[" + i + "]", example);
                    newEntries.add(newEntry);
                    CACHED_GLOBAL_ENTRIES.add(newEntry);
                }
                // Add node alias creation
                List<JIPipeNodeMenuLocation> aliases = nodeInfo.getAliases();
                for (int i = 0; i < aliases.size(); i++) {
                    JIPipeNodeMenuLocation alias = aliases.get(i);
                    if(!StringUtils.isNullOrEmpty(alias.getAlternativeName())) {
                        // Add as alias if name is different
                        CreateNewNodeByInfoAliasDatabaseEntry newEntry = new CreateNewNodeByInfoAliasDatabaseEntry("create-node-by-info:" + entry.getKey() + ":alias-" + i, nodeInfo, alias);
                        newEntries.add(newEntry);
                        CACHED_GLOBAL_ENTRIES.add(newEntry);
                    }
                }
            }

            // Create compartments
            {
                CreateNewCompartmentNodeDatabaseEntry entry = new CreateNewCompartmentNodeDatabaseEntry();
                CACHED_GLOBAL_ENTRIES.add(entry);
                newEntries.add(entry);
            }

        } else {
            newEntries.addAll(CACHED_GLOBAL_ENTRIES);
        }

        if (database.getProject() != null) {
            // Add existing nodes
            for (JIPipeGraphNode graphNode : database.getProject().getGraph().getGraphNodes()) {
                newEntries.add(new ExistingPipelineNodeDatabaseEntry("existing-pipeline-node:" + graphNode.getUUIDInParentGraph(), graphNode));
            }

            // Add existing compartments
            for (Map.Entry<UUID, JIPipeProjectCompartment> entry : database.getProject().getCompartments().entrySet()) {
                newEntries.add(new ExistingCompartmentDatabaseEntry("existing-compartment:" + entry.getKey(), entry.getValue()));
            }
        }

        // Create from templates
        for (JIPipeNodeTemplate template : JIPipe.getNodeTemplates().getAllTemplates(database.getProject())) {
            try {
                CreateNewNodesByTemplateDatabaseEntry newEntry = new CreateNewNodesByTemplateDatabaseEntry(template);
                newEntries.add(newEntry);
            } catch (Throwable ignored) {
            }
        }

        database.setEntries(newEntries);
//        try {
//            database.getLuceneSearch().rebuildDirectory();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }
}
