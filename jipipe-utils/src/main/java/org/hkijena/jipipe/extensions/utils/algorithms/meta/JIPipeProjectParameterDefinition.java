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

package org.hkijena.jipipe.extensions.utils.algorithms.meta;

import ij.IJ;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.multiparameters.datasources.ParametersDataTableDefinition;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.library.table.ParameterTable;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.settings.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@JIPipeDocumentation(name = "Define JIPipe project parameters", description = "Defines parameters that will be put into JIPipe projects. The parameter key has two modes: " +
        "If it matches with a pipeline parameter (that can be set up via a pipeline's settings), this parameter is changed. It can also match with an absolute path to a node's parameter with following format: " +
        "[node-id]/[node parameter key]")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class JIPipeProjectParameterDefinition extends ParametersDataTableDefinition {
    public JIPipeProjectParameterDefinition(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeProjectParameterDefinition(ParametersDataTableDefinition other) {
        super(other);
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/folder-open.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/folder-open.png")
    @JIPipeDocumentation(name = "Load parameters from project", description = "Loads parameters from a project file")
    public void importParametersFromProject(JIPipeWorkbench workbench) {
        Path projectFile = FileChooserSettings.openFile(workbench.getWindow(), FileChooserSettings.LastDirectoryKey.Projects, "Import JIPipe project", UIUtils.EXTENSION_FILTER_JIP);
        if (projectFile != null) {
            try {
                JIPipeProject project = JIPipeProject.loadProject(projectFile, new JIPipeIssueReport());
                JIPipeProjectInfoParameters infoParameters = project.getPipelineParameters();
                getParameterTable().clear();
                JIPipeParameterTree tree = new JIPipeParameterTree(infoParameters);
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    if (entry.getKey().equals("exported-parameters"))
                        continue;
                    getParameterTable().addColumn(new ParameterTable.ParameterColumn(entry.getValue().getName(), entry.getKey(), entry.getValue().getFieldClass()), null);
                }
                getParameterTable().addRow();
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    if (entry.getKey().equals("exported-parameters"))
                        continue;
                    getParameterTable().setValueAt(entry.getValue().get(Object.class), 0, getParameterTable().getColumnIndex(entry.getKey()));
                }
                getEventBus().post(new ParameterChangedEvent(this, "parameter-table"));
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }
}
