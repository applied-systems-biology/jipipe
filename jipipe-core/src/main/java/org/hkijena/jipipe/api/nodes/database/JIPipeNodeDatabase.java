package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase implements JIPipeGraph.NodeAddedEventListener, JIPipeGraph.NodeRemovedEventListener, JIPipeParameterCollection.ParameterChangedEventListener, JIPipeRunnable {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Node database");
    private final JIPipeProject project;
    private final Timer timer;
    private List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();

    public JIPipeNodeDatabase() {
        this(null);
    }

    public JIPipeNodeDatabase(JIPipeProject project) {
        this.project = project;
        this.timer = new Timer(1000, e -> rebuildImmediately());
        if(project != null) {
            project.getGraph().getNodeAddedEventEmitter().subscribe(this);
            project.getGraph().getNodeRemovedEventEmitter().subscribe(this);
            project.getCompartmentGraph().getNodeAddedEventEmitter().subscribe(this);
            project.getCompartmentGraph().getNodeRemovedEventEmitter().subscribe(this);
            project.getMetadata().getParameterChangedEventEmitter().subscribe(this);
        }
        rebuildImmediately();
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Rebuild database";
    }

    public void rebuildImmediately() {
        queue.cancelAll();
        queue.enqueue(this);
    }

    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return entries;
    }

    public void rebuildLater() {
        timer.restart();
    }

    public JIPipeProject getProject() {
        return project;
    }

    @Override
    public void onNodeAdded(JIPipeGraph.NodeAddedEvent event) {
        rebuildLater();
    }

    @Override
    public void onNodeRemoved(JIPipeGraph.NodeRemovedEvent event) {
        rebuildLater();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if(project != null && event.getSource() == project.getMetadata() && "node-templates".equals(event.getKey())) {
            rebuildLater();
        }
    }

    @Override
    public void run() {
        JIPipeNodeRegistry nodeRegistry = JIPipe.getNodes();
        List<JIPipeNodeDatabaseEntry> newEntries = new ArrayList<>();

        // Add creation of nodes by info
        for (Map.Entry<String, JIPipeNodeInfo> entry : nodeRegistry.getRegisteredNodeInfos().entrySet()) {
            newEntries.add(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + entry.getKey(), entry.getValue()));

            // Add node example creation
            ArrayList<JIPipeNodeExample> examples = new ArrayList<>(nodeRegistry.getNodeExamples(entry.getKey()));
            for (int i = 0; i < examples.size(); i++) {
                JIPipeNodeExample example = examples.get(i);
                newEntries.add(new CreateNewNodeByExampleDatabaseEntry("create-node-by-example:" + entry.getKey() + ":[" + i + "]", example));
            }
        }

        if(project != null) {
            // Add existing nodes
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                newEntries.add(new ExistingPipelineNodeDatabaseEntry("existing-pipeline-node:" + graphNode.getUUIDInParentGraph(), graphNode));
            }

            // Add existing compartments
            for (Map.Entry<UUID, JIPipeProjectCompartment> entry : project.getCompartments().entrySet()) {
                newEntries.add(new ExistingCompartmentDatabaseEntry("existing-compartment:" + entry.getKey(), entry.getValue()));
            }
        }

        synchronized (this) {
            this.entries = newEntries;
        }
    }
}
