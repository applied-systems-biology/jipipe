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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.io;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataExporterRef;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;

@SetJIPipeDocumentation(name = "Export to ImageJ", description = "Runs an ImageJ exporter. You can select the utilized exporter type in the parameters.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nExport")
public class RunImageJExporterAlgorithm extends JIPipeMergingAlgorithm {

    private ImageJDataExporterRef exporterType = new ImageJDataExporterRef();
    private ImageJExportParameters exportParameters = new ImageJExportParameters();

    public RunImageJExporterAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.exportParameters.setActivate(true);
        this.exportParameters.setDuplicate(true);
        registerSubParameter(exportParameters);
    }

    public RunImageJExporterAlgorithm(RunImageJExporterAlgorithm other) {
        super(other);
        this.exportParameters = new ImageJExportParameters(other.exportParameters);
        setExporterType(other.getExporterType());
        registerSubParameter(exportParameters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(exporterType.getInstance().getExportedJIPipeDataType());
        for (Integer row : iterationStep.getInputRows(getFirstInputSlot())) {
            dataTable.addData(getFirstInputSlot().getDataItemStore(row),
                    getFirstInputSlot().getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    iterationStep.createNewContext(),
                    progressInfo);
        }
        exporterType.getInstance().exportData(dataTable, exportParameters, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (exporterType.getInstance() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Exporter type", "exporter-type"),
                    "No exporter type selected!", "No exporter type was selected", "Please select an exporter"));
        }
        super.reportValidity(reportContext, report);
    }

    @SetJIPipeDocumentation(name = "Set export parameters", description = "Sets the export parameters via its default UI")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/configure.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/configure.png")
    public void setExporterParametersFromUI(JIPipeWorkbench parent) {
        if (exporterType.getInstance() == null) {
            JOptionPane.showMessageDialog(((JIPipeDesktopWorkbench) parent).getWindow(),
                    "Please select an exporter type, first!",
                    "Set export parameters",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ImageJDataExportOperation operation = new ImageJDataExportOperation(exporterType.getInstance());
        this.exportParameters.copyTo(operation);
        ImageJDataExporterUI ui = JIPipe.getImageJAdapters().createUIForExportOperation(parent, operation);
        if (JOptionPane.showConfirmDialog(((JIPipeDesktopWorkbench) parent).getWindow(), ui, "Set export parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            operation.copyTo(this.exportParameters);
            emitParameterUIChangedEvent();
        }
    }

    @SetJIPipeDocumentation(name = "Exporter type", description = "Please select the exporter type")
    @JIPipeParameter(value = "exporter-type", important = true)
    public ImageJDataExporterRef getExporterType() {
        return exporterType;
    }

    @JIPipeParameter("exporter-type")
    public void setExporterType(ImageJDataExporterRef exporterType) {
        this.exporterType = exporterType;
    }

    @SetJIPipeDocumentation(name = "Export parameters", description = "Please setup the following parameters to indicate which data is exported")
    @JIPipeParameter("export-parameters")
    public ImageJExportParameters getExportParameters() {
        return exportParameters;
    }
}
