package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.references.ImageJDataExporterRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;

@JIPipeDocumentation(name = "Export to ImageJ", description = "Runs an ImageJ exporter. You can select the utilized exporter type in the parameters.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
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
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(exporterType.getInstance().getExportedJIPipeDataType());
        for (Integer row : dataBatch.getInputRows(getFirstInputSlot())) {
            dataTable.addData(getFirstInputSlot().getVirtualData(row),
                    getFirstInputSlot().getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
        exporterType.getInstance().exportData(dataTable, exportParameters, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (exporterType.getInstance() == null) {
            report.reportIsInvalid("No exporter type selected!", "No exporter type was selected", "Please select an exporter", this);
        }
        super.reportValidity(report);
    }

    @JIPipeDocumentation(name = "Set export parameters", description = "Sets the export parameters via its default UI")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/configure.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/configure.png")
    public void setExporterParametersFromUI(JIPipeWorkbench parent) {
        if (exporterType.getInstance() == null) {
            JOptionPane.showMessageDialog(parent.getWindow(),
                    "Please select an exporter type, first!",
                    "Set export parameters",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ImageJDataExportOperation operation = new ImageJDataExportOperation(exporterType.getInstance());
        this.exportParameters.copyTo(operation);
        ImageJDataExporterUI ui = JIPipe.getImageJAdapters().createUIForExportOperation(parent, operation);
        if (JOptionPane.showConfirmDialog(parent.getWindow(), ui, "Set export parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            operation.copyTo(this.exportParameters);
            triggerParameterUIChange();
        }
    }

    @JIPipeDocumentation(name = "Exporter type", description = "Please select the exporter type")
    @JIPipeParameter(value = "exporter-type", important = true)
    public ImageJDataExporterRef getExporterType() {
        return exporterType;
    }

    @JIPipeParameter("exporter-type")
    public void setExporterType(ImageJDataExporterRef exporterType) {
        this.exporterType = exporterType;
    }

    @JIPipeDocumentation(name = "Export parameters", description = "Please setup the following parameters to indicate which data is exported")
    @JIPipeParameter("export-parameters")
    public ImageJExportParameters getExportParameters() {
        return exportParameters;
    }
}