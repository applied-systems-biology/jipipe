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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.parameters.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A project compartment.
 * Its node functionality is structural.
 */
@JIPipeDocumentation(name = "Graph compartment", description = "A compartment in the analysis graph")
@JIPipeInputSlot(value = JIPipeCompartmentOutputData.class, slotName = "Input", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = JIPipeCompartmentOutputData.class, slotName = "Output", autoCreate = true)
public class JIPipeProjectCompartment extends JIPipeGraphNode implements JIPipeCustomParameterCollection {

    private JIPipeProject project;
    private JIPipeCompartmentOutput outputNode;

    private GraphNodeParameterReferenceGroupCollection exportedParameters = new GraphNodeParameterReferenceGroupCollection();

    private boolean showLimitedParameters = false;

    /**
     * Creates new instance
     *
     * @param info Algorithm info
     */
    public JIPipeProjectCompartment(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exportedParameters);
    }

    /**
     * Copies the compartment
     *
     * @param other Original compartment
     */
    public JIPipeProjectCompartment(JIPipeProjectCompartment other) {
        super(other);
        this.exportedParameters = new GraphNodeParameterReferenceGroupCollection(other.exportedParameters);
        this.showLimitedParameters = other.showLimitedParameters;

        registerSubParameter(exportedParameters);
    }

    /**
     * @return The compartment ID
     */
    public UUID getProjectCompartmentUUID() {
        return getUUIDInParentGraph();
    }

    /**
     * @return If the compartment is initialized
     */
    public boolean isInitialized() {
        return project != null && outputNode != null;
    }

    @Override
    @JIPipeParameter(value = "jipipe:node:name", functional = false)
    public void setCustomName(String customName) {
        super.setCustomName(customName);
        if (outputNode != null) {
            outputNode.setCustomName(getName() + " output");
        }
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {

    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
//        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
//                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
//        HashMap<String, JIPipeParameterAccess> map = new HashMap<>(standardParameters.getParameters());
//        map.values().removeIf(access -> access.getSource() != this);
//        return map;
        return Collections.emptyMap();
    }

    @Override
    public boolean getIncludeReflectionParameters() {
        return true;
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        updateExportedParameters();
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
        if (project != null) {
            result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, project.getGraph().getParameterTree(false, null), false));
        }
        return result;
    }

    /**
     * @return The project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * Sets the project. Internal use only.
     *
     * @param project The project
     */
    public void setProject(JIPipeProject project) {
        this.project = project;
        updateExportedParameters();
    }

    /**
     * @return The compartment output
     */
    public JIPipeCompartmentOutput getOutputNode() {
        return outputNode;
    }

    /**
     * Sets the compartment output. Internal use only.
     *
     * @param outputNode the compartment output
     */
    public void setOutputNode(JIPipeCompartmentOutput outputNode) {
        this.outputNode = outputNode;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {

    }

    @JIPipeDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the group into the group node")
    @JIPipeParameter(value = "exported-parameters", functional = false)
    public GraphNodeParameterReferenceGroupCollection getExportedParameters() {
        return exportedParameters;
    }

    @JIPipeParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameterReferenceGroupCollection exportedParameters) {
        this.exportedParameters = exportedParameters;
        updateExportedParameters();
        registerSubParameter(exportedParameters);
        emitParameterStructureChangedEvent();
    }

    private void updateExportedParameters() {
        if (project != null) {
            exportedParameters.setGraph(project.getGraph());
            exportedParameters.setUiRestrictToCompartments(Collections.singleton(getProjectCompartmentUUID()));
        }
    }

    @JIPipeDocumentation(name = "Show limited parameter set", description = "If enabled, only the exported parameters, name, and description are shown as parameters. " +
            "The data batch generation will also be hidden. This can be useful for educational pipelines.")
    @JIPipeParameter(value = "show-limited-parameters", functional = false)
    public boolean isShowLimitedParameters() {
        return showLimitedParameters;
    }

    @JIPipeParameter("show-limited-parameters")
    public void setShowLimitedParameters(boolean showLimitedParameters) {
        this.showLimitedParameters = showLimitedParameters;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (showLimitedParameters) {
            if (access.getSource() == this) {
                String key = access.getKey();
                if ("show-limited-parameters".equals(key) || "jipipe:node:name".equals(key) || "jipipe:node:description".equals(key)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return super.isParameterUIVisible(tree, access);
    }
}
