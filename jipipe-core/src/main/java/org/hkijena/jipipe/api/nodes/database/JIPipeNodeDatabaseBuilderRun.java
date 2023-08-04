package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;

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

        if(CACHED_GLOBAL_ENTRIES == null) {
            CACHED_GLOBAL_ENTRIES = new ArrayList<>();
            // Add creation of nodes by info
            for (Map.Entry<String, JIPipeNodeInfo> entry : nodeRegistry.getRegisteredNodeInfos().entrySet()) {
                {
                    CreateNewNodeByInfoDatabaseEntry newEntry = new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + entry.getKey(), entry.getValue());
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
            }
        }
        else {
            newEntries.addAll(CACHED_GLOBAL_ENTRIES);
        }

        if(database.getProject() != null) {
            // Add existing nodes
            for (JIPipeGraphNode graphNode : database.getProject().getGraph().getGraphNodes()) {
                newEntries.add(new ExistingPipelineNodeDatabaseEntry("existing-pipeline-node:" + graphNode.getUUIDInParentGraph(), graphNode));
            }

            // Add existing compartments
            for (Map.Entry<UUID, JIPipeProjectCompartment> entry : database.getProject().getCompartments().entrySet()) {
                newEntries.add(new ExistingCompartmentDatabaseEntry("existing-compartment:" + entry.getKey(), entry.getValue()));
            }
        }

        database.setEntries(newEntries);
    }
}
