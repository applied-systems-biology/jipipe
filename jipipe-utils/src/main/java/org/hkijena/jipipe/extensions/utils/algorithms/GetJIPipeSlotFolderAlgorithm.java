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

package org.hkijena.jipipe.extensions.utils.algorithms;

import ij.IJ;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.utils.datatypes.JIPipeOutputData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeProjectOutputTreePanel;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Get JIPipe slot folder", description = "Extracts a slot output folder from a JIPipe output. Use the 'Set output slot' button to select the correct parameters.")
@JIPipeInputSlot(value = JIPipeOutputData.class, slotName = "JIPipe output", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Slot output folder", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Meta run")
public class GetJIPipeSlotFolderAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String nodeId;
    private String slotName;
    private String compartmentId;

    public GetJIPipeSlotFolderAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GetJIPipeSlotFolderAlgorithm(GetJIPipeSlotFolderAlgorithm other) {
        super(other);
        this.nodeId = other.nodeId;
        this.slotName = other.slotName;
        this.compartmentId = other.compartmentId;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeOutputData outputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeOutputData.class, progressInfo);
        Path slotPath = outputData.toPath().resolve(compartmentId).resolve(nodeId).resolve(slotName);
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(slotPath), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        report.resolve("Node ID").checkNonEmpty(nodeId, this);
        report.resolve("Slot name").checkNonEmpty(slotName, this);
        report.resolve("Compartment ID").checkNonEmpty(compartmentId, this);
    }

    @JIPipeDocumentation(name = "Node alias ID", description = "The unique identifier of the node that contains the output. You can either use the 'Set output slot' button to auto-configure this value or look up the node ID in the help of the node. " +
            "Please note that this is not the node type ID.")
    @JIPipeParameter(value = "node-id", uiOrder = -100)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/node.png")
    public String getNodeId() {
        return nodeId;
    }

    @JIPipeParameter("node-id")
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @JIPipeDocumentation(name = "Slot name", description = "The name of the output slot within the targeted node. " +
            "You can use the 'Set output slot' button to auto-configure or just type in the name of the output slot.")
    @JIPipeParameter(value = "slot-name", uiOrder = -99)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/slot.png")
    public String getSlotName() {
        return slotName;
    }

    @JIPipeParameter("slot-name")
    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    @JIPipeDocumentation(name = "Compartment alias ID", description = "The ID of the compartment, where the data is located. " +
            "You can use the 'Set output slot' button to auto-configure or just type in the name of the output slot.")
    @JIPipeParameter("compartment-id")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/graph-compartment.png")
    public String getCompartmentId() {
        return compartmentId;
    }

    @JIPipeParameter("compartment-id")
    public void setCompartmentId(String compartmentId) {
        this.compartmentId = compartmentId;
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/jipipe.png")
    @JIPipeDocumentation(name = "Set output slot", description = "Loads parameters from a project file")
    public void importParametersFromProject(JIPipeWorkbench workbench) {
        Path projectFile = FileChooserSettings.openFile(workbench.getWindow(), FileChooserSettings.KEY_PROJECT, "Import JIPipe project", UIUtils.EXTENSION_FILTER_JIP);
        if (projectFile != null) {
            try {
                JIPipeProject project = JIPipeProject.loadProject(projectFile, new JIPipeIssueReport());
                JIPipeProjectOutputTreePanel panel = new JIPipeProjectOutputTreePanel(project);
                panel.setBorder(BorderFactory.createEtchedBorder());
                int result = JOptionPane.showOptionDialog(
                        workbench.getWindow(),
                        new Object[]{panel},
                        "Select slot",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null, null, null);

                if (result == JOptionPane.OK_OPTION) {
                    Object component = panel.getTree().getLastSelectedPathComponent();
                    if (component instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) component;
                        if (node.getUserObject() instanceof JIPipeDataSlot) {
                            JIPipeDataSlot slot = (JIPipeDataSlot) node.getUserObject();
                            ParameterUtils.setParameter(this, "node-id", slot.getNode().getAliasIdInGraph());
                            ParameterUtils.setParameter(this, "slot-name", slot.getName());
                            ParameterUtils.setParameter(this, "compartment-id",
                                    slot.getNode().getProjectCompartment().getAliasIdInGraph());
                        } else {
                            JOptionPane.showMessageDialog(workbench.getWindow(), "Please select a slot", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(workbench.getWindow(), "Please select a slot", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                IJ.handleException(e);
            }
        }
    }
}
