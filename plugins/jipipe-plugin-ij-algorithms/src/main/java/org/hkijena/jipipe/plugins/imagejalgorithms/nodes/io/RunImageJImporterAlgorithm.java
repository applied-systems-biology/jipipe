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
import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataImporterRef;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;

@SetJIPipeDocumentation(name = "Import from ImageJ", description = "Runs an ImageJ importer. You can select the utilized importer type in the parameters.")
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nImport")
public class RunImageJImporterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ImageJDataImporterRef importerType = new ImageJDataImporterRef();
    private ImageJImportParameters importParameters = new ImageJImportParameters();

    public RunImageJImporterAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(importParameters);
    }

    public RunImageJImporterAlgorithm(RunImageJImporterAlgorithm other) {
        super(other);
        this.importParameters = new ImageJImportParameters(other.importParameters);
        setImporterType(other.getImporterType());
        registerSubParameter(importParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = importerType.getInstance().importData(null, importParameters, progressInfo);
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            iterationStep.addOutputData(getFirstOutputSlot(),
                    dataTable.getDataItemStore(i),
                    dataTable.getTextAnnotations(i),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    dataTable.getDataAnnotations(i),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (importerType.getInstance() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Importer type", "importer-type"),
                    "No importer type selected!", "No importer type was selected", "Please select an importer"));
        }
        super.reportValidity(reportContext, report);
    }

    @SetJIPipeDocumentation(name = "Set import parameters", description = "Sets the import parameters via its default UI")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/configure.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/configure.png")
    public void setImporterParametersFromUI(JIPipeWorkbench parent) {
        if (importerType.getInstance() == null) {
            JOptionPane.showMessageDialog(((JIPipeDesktopWorkbench) parent).getWindow(),
                    "Please select an importer type, first!",
                    "Set import parameters",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ImageJDataImportOperation operation = new ImageJDataImportOperation(importerType.getInstance());
        this.importParameters.copyTo(operation);
        ImageJDataImporterUI ui = JIPipe.getImageJAdapters().createUIForImportOperation(parent, operation);
        if (JOptionPane.showConfirmDialog(((JIPipeDesktopWorkbench) parent).getWindow(), ui, "Set import parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            operation.copyTo(this.importParameters);
            emitParameterUIChangedEvent();
        }
    }

    @SetJIPipeDocumentation(name = "Importer type", description = "Please select the importer type")
    @JIPipeParameter(value = "importer-type", important = true)
    public ImageJDataImporterRef getImporterType() {
        return importerType;
    }

    @JIPipeParameter("importer-type")
    public void setImporterType(ImageJDataImporterRef importerType) {
        this.importerType = importerType;
    }

    @SetJIPipeDocumentation(name = "Import parameters", description = "Please setup the following parameters to indicate which data is imported")
    @JIPipeParameter("import-parameters")
    public ImageJImportParameters getImportParameters() {
        return importParameters;
    }
}
