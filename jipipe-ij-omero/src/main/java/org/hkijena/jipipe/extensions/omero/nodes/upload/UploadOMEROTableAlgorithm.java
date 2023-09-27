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

package org.hkijena.jipipe.extensions.omero.nodes.upload;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROAnnotationReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@JIPipeDocumentation(name = "Attach table to OMERO image", description = "Attaches a table to an OMERO image")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Tables", autoCreate = true, description = "The table to attach")
@JIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Target", autoCreate = true, description = "The target OMERO image")
@JIPipeOutputSlot(value = OMEROAnnotationReferenceData.class, slotName = "Tables", autoCreate = true, description = "Reference to the generated table annotation")
public class UploadOMEROTableAlgorithm extends JIPipeIteratingAlgorithm {

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        long imageId = dataBatch.getInputData("Target", OMEROImageReferenceData.class, progressInfo).getImageId();
        ResultsTableData resultsTableData = dataBatch.getInputData("Tables", ResultsTableData.class, progressInfo);
        OMEROCredentialsEnvironment credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());

        // Determine file name
        String fileName;
        if(StringUtils.isNullOrEmpty(fileNameGenerator.getExpression())) {
            fileName = "unnamed";
        }
        else {
            Path tmpDirectory = getNewScratch();
            Path outputPath = fileNameGenerator.generatePath(tmpDirectory,
                    getProjectDirectory(),
                    getProjectDataDirs(),
                    resultsTableData.toString(),
                    dataBatch.getInputRow("Tables"),
                    new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));
            fileName = outputPath.getFileName().toString();
        }

        try (OMEROGateway gateway = new OMEROGateway(credentials.toLoginCredentials(), progressInfo)) {
            TablesFacility tablesFacility = gateway.getGateway().getFacility(TablesFacility.class);
            progressInfo.log("Attaching tables to Image ID=" + imageId);
            ImageData imageData = gateway.getImage(imageId, -1);
            SecurityContext context = new SecurityContext(imageData.getGroupId());

            TableData tableData = OMEROUtils.tableToOMERO(resultsTableData);
            tableData = tablesFacility.addTable(context, imageData, fileName, tableData);

            dataBatch.addOutputData(getFirstOutputSlot(), new OMEROAnnotationReferenceData(tableData.getOriginalFileId()), progressInfo);

        } catch (ExecutionException | DSAccessException | DSOutOfServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Table name", description = "Expression that generates the name of the uploaded table. Please note that the directory will be ignored and 'unnamed' will be assumed if " +
            "no name is provided.")
    @JIPipeParameter("file-name-generator")
    public DataExportExpressionParameter getFileNameGenerator() {
        return fileNameGenerator;
    }

    @JIPipeParameter("file-name-generator")
    public void setFileNameGenerator(DataExportExpressionParameter fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @JIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(context, this), environment);
    }
}
