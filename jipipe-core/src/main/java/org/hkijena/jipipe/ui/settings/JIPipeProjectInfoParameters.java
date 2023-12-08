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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEventListener;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.parameters.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional metadata that provides parameter references for {@link JIPipeProjectOverviewUI}
 */
public class JIPipeProjectInfoParameters extends AbstractJIPipeParameterCollection implements JIPipeCustomParameterCollection, ParameterReferencesChangedEventListener {

    public static final String METADATA_KEY = "org.hkijena.jipipe:pipeline-parameters";
    private JIPipeProject project;
    private GraphNodeParameterReferenceGroupCollection exportedParameters = new GraphNodeParameterReferenceGroupCollection();

    public JIPipeProjectInfoParameters() {
        this.exportedParameters.getParameterReferencesChangedEventEmitter().subscribe(this);
    }

    @JIPipeParameter(value = "exported-parameters", hidden = true)
    public GraphNodeParameterReferenceGroupCollection getExportedParameters() {
        return exportedParameters;
    }

    @JIPipeParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameterReferenceGroupCollection parameters) {
        this.exportedParameters.getParameterReferencesChangedEventEmitter().unsubscribe(this);
        this.exportedParameters = parameters;
        this.exportedParameters.getParameterReferencesChangedEventEmitter().subscribe(this);
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
        if (project != null) {
            this.exportedParameters.setGraph(project.getGraph());
            result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getProject().getGraph().getParameterTree(false, null), false));
        }
        return result;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Override
    public void onParameterReferencesChanged(ParameterReferencesChangedEvent event) {
        getParameterStructureChangedEventEmitter().emit(new ParameterStructureChangedEvent(this));
    }

    public JIPipeProject getProject() {
        return project;
    }

    public void setProject(JIPipeProject project) {
        this.project = project;
        exportedParameters.setGraph(project.getGraph());
    }
}
