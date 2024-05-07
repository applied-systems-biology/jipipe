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

package org.hkijena.jipipe.plugins.utils.algorithms.meta;

import ij.IJ;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.multiparameters.nodes.DefineParametersTableAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.table.ParameterTable;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@SetJIPipeDocumentation(name = "Define JIPipe project parameters", description = "Defines parameters that will be put into JIPipe projects. The parameter key has two modes: " +
        "If it matches with a pipeline parameter (that can be set up via a pipeline's settings), this parameter is changed. It can also match with an absolute path to a node's parameter with following format: " +
        "[node-id]/[node parameter key]")
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class JIPipeProjectParameterDefinition extends DefineParametersTableAlgorithm {
    public JIPipeProjectParameterDefinition(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeProjectParameterDefinition(DefineParametersTableAlgorithm other) {
        super(other);
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/folder-open.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/folder-open.png")
    @SetJIPipeDocumentation(name = "Load parameters from project", description = "Loads parameters from a project file")
    public void importParametersFromProject(JIPipeWorkbench workbench) {
        Path projectFile = JIPipeFileChooserApplicationSettings.openFile(((JIPipeDesktopWorkbench) workbench).getWindow(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Import JIPipe project", UIUtils.EXTENSION_FILTER_JIP);
        if (projectFile != null) {
            try {
                JIPipeProject project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
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
                emitParameterChangedEvent("parameter-table");
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }
}
