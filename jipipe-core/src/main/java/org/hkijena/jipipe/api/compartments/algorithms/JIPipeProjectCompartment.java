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

package org.hkijena.jipipe.api.compartments.algorithms;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

import java.util.*;

/**
 * A project compartment.
 * Its node functionality is structural.
 */
@SetJIPipeDocumentation(name = "Graph compartment", description = "A compartment in the analysis graph")
@AddJIPipeInputSlot(value = JIPipeCompartmentOutputData.class, name = "Input", create = true, optional = true)
@AddJIPipeOutputSlot(value = JIPipeCompartmentOutputData.class)
public class JIPipeProjectCompartment extends JIPipeGraphNode implements JIPipeCustomParameterCollection {

    private JIPipeProject project;
    private BiMap<String, JIPipeProjectCompartmentOutput> outputNodes = HashBiMap.create();

    private GraphNodeParameterReferenceGroupCollection exportedParameters = new GraphNodeParameterReferenceGroupCollection();

    private boolean showLimitedParameters = false;

    /**
     * Creates new instance
     *
     * @param info Algorithm info
     */
    public JIPipeProjectCompartment(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "Incoming data from other compartments", JIPipeCompartmentOutputData.class, true)
                .restrictOutputTo(JIPipeCompartmentOutputData.class)
                .sealInput()
                .build());
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
        return project != null && outputNodes != null;
    }


    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void onSlotConfigurationChanged(JIPipeSlotConfiguration.SlotConfigurationChangedEvent event) {
        super.onSlotConfigurationChanged(event);

        if(project != null) {
            project.updateCompartmentOutputs(this);
        }
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
    public JIPipeProject getRuntimeProject() {
        return project;
    }

    /**
     * Sets the project. Internal use only.
     *
     * @param runtimeProject The project
     */
    public void setRuntimeProject(JIPipeProject runtimeProject) {
        this.project = runtimeProject;
        updateExportedParameters();
    }

    public JIPipeProjectCompartmentOutput getOutputNode(String outputName) {
        return outputNodes.get(outputName);
    }

    public BiMap<String, JIPipeProjectCompartmentOutput> getOutputNodes() {
        return outputNodes;
    }

    public void setOutputNodes(BiMap<String, JIPipeProjectCompartmentOutput> outputNodes) {
        this.outputNodes = outputNodes;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @SetJIPipeDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the group into the group node")
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

    @SetJIPipeDocumentation(name = "Show limited parameter set", description = "If enabled, only the exported parameters, name, and description are shown as parameters. " +
            "The iteration step generation will also be hidden. This can be useful for educational pipelines.")
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

    public List<JIPipeProjectCompartmentOutput> getSortedOutputNodes() {
        List<JIPipeProjectCompartmentOutput> result = new ArrayList<>();
        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            result.add(getOutputNode(outputSlot.getName()));
        }
        return result;
    }
}
