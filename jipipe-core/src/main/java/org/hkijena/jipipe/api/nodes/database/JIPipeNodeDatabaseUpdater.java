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

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import javax.swing.*;

public class JIPipeNodeDatabaseUpdater implements JIPipeGraph.NodeAddedEventListener, JIPipeGraph.NodeRemovedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {
    private final JIPipeNodeDatabase database;
    private final Timer timer;

    public JIPipeNodeDatabaseUpdater(JIPipeNodeDatabase database) {
        this.timer = new Timer(1000, e -> database.rebuildImmediately());
        this.database = database;
        initialize();
    }

    private void initialize() {
        if (database.getProject() != null) {
            database.getProject().getGraph().getNodeAddedEventEmitter().subscribe(this);
            database.getProject().getGraph().getNodeRemovedEventEmitter().subscribe(this);
            database.getProject().getCompartmentGraph().getNodeAddedEventEmitter().subscribe(this);
            database.getProject().getCompartmentGraph().getNodeRemovedEventEmitter().subscribe(this);
            database.getProject().getMetadata().getParameterChangedEventEmitter().subscribe(this);
        }
    }

    public void rebuildLater() {
        timer.restart();
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
        if (database.getProject() != null && event.getSource() == database.getProject().getMetadata() && "node-templates".equals(event.getKey())) {
            rebuildLater();
        }
    }
}
