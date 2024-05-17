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

package org.hkijena.jipipe.plugins.omero.nodes.upload;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROAnnotationReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@SetJIPipeDocumentation(name = "Attach table to OMERO image", description = "Attaches a table to an OMERO image")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Tables", create = true, description = "The table to attach")
@AddJIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Target", create = true, description = "The target OMERO image")
@AddJIPipeOutputSlot(value = OMEROAnnotationReferenceData.class, slotName = "Tables", create = true, description = "Reference to the generated table annotation")
public class UploadOMEROTableAlgorithm extends JIPipeIteratingAlgorithm implements OMEROCredentialAccessNode {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private DataExportExpressionParameter fileNameGenerator = new DataExportExpressionParameter("auto_file_name");

    public UploadOMEROTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UploadOMEROTableAlgorithm(UploadOMEROTableAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.fileNameGenerator = new DataExportExpressionParameter(other.fileNameGenerator);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        long imageId = iterationStep.getInputData("Target", OMEROImageReferenceData.class, progressInfo).getImageId();
        ResultsTableData resultsTableData = iterationStep.getInputData("Tables", ResultsTableData.class, progressInfo);
        OMEROCredentialsEnvironment credentials = getConfiguredOMEROCredentialsEnvironment();

        // Determine file name
        String fileName;
        if (StringUtils.isNullOrEmpty(fileNameGenerator.getExpression())) {
            fileName = "unnamed";
        } else {
            Path tmpDirectory = getNewScratch();
            Path outputPath = fileNameGenerator.generatePath(tmpDirectory,
                    getProjectDirectory(),
                    getProjectDataDirs(),
                    resultsTableData.toString(),
                    iterationStep.getInputRow("Tables"),
                    new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));
            fileName = outputPath.getFileName().toString();
        }

        try (OMEROGateway gateway = new OMEROGateway(credentials.toLoginCredentials(), progressInfo)) {
            TablesFacility tablesFacility = gateway.getGateway().getFacility(TablesFacility.class);
            progressInfo.log("Attaching tables to Image ID=" + imageId);
            ImageData imageData = gateway.getImage(imageId, -1);
            SecurityContext context = new SecurityContext(imageData.getGroupId());

            TableData tableData = OMEROUtils.tableToOMERO(resultsTableData);
            tableData = tablesFacility.addTable(context, imageData, fileName, tableData);

            iterationStep.addOutputData(getFirstOutputSlot(), new OMEROAnnotationReferenceData(tableData.getOriginalFileId()), progressInfo);

        } catch (ExecutionException | DSAccessException | DSOutOfServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Table name", description = "Expression that generates the name of the uploaded table. Please note that the directory will be ignored and 'unnamed' will be assumed if " +
            "no name is provided.")
    @JIPipeParameter("file-name-generator")
    public DataExportExpressionParameter getFileNameGenerator() {
        return fileNameGenerator;
    }

    @JIPipeParameter("file-name-generator")
    public void setFileNameGenerator(DataExportExpressionParameter fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @SetJIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        report.report(new GraphNodeValidationReportContext(reportContext, this), environment);
    }
}
