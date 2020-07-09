/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.ui.settings;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.ACAQProject;
import org.hkijena.pipelinej.api.events.ParameterStructureChangedEvent;
import org.hkijena.pipelinej.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.pipelinej.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.pipelinej.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.pipelinej.api.parameters.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional metadata that provides parameter references for {@link org.hkijena.pipelinej.ui.ACAQProjectInfoUI}
 */
public class ACAQProjectInfoParameters implements ACAQParameterCollection, ACAQCustomParameterCollection {

    public static final String METADATA_KEY = "pipeline-parameters";

    private final EventBus eventBus = new EventBus();
    private ACAQProject project;
    private GraphNodeParameters exportedParameters = new GraphNodeParameters();

    public ACAQProjectInfoParameters() {
        this.exportedParameters.getEventBus().register(this);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQParameter(value = "exported-parameters", visibility = ACAQParameterVisibility.Hidden)
    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @ACAQParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameters parameters) {
        this.exportedParameters = parameters;
        this.exportedParameters.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQParameterCollection> getChildParameterCollections() {
        Map<String, ACAQParameterCollection> result = new HashMap<>();
        if (project != null) {
            this.exportedParameters.setGraph(project.getGraph());
            result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getProject().getGraph().getParameterTree(), false));
        }
        return result;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        ACAQParameterTree standardParameters = new ACAQParameterTree(this,
                ACAQParameterTree.IGNORE_CUSTOM | ACAQParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Subscribe
    public void onParameterReferencesChanged(ParameterReferencesChangedEvent event) {
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    public ACAQProject getProject() {
        return project;
    }

    public void setProject(ACAQProject project) {
        this.project = project;
        exportedParameters.setGraph(project.getGraph());
    }
}
