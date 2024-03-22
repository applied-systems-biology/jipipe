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

package org.hkijena.jipipe.extensions.omero.nodes.upload;

import omero.ServerError;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
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
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.parameters.AnnotationsToOMEROKeyValuePairExporter;
import org.hkijena.jipipe.extensions.omero.parameters.AnnotationsToOMEROTagExporter;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROImageUploader;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Upload image to OMERO", description = "Uploads an image to OMERO.")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeInputSlot(value = OMEImageData.class, slotName = "Images", create = true, description = "The image(s) to upload")
@AddJIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Target dataset", create = true, description = "The data set where the image(s) will be stored")
@AddJIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", create = true, description = "Reference to the uploaded image(s)")
public class UploadOMEROImageAlgorithm extends JIPipeIteratingAlgorithm {
    private final AnnotationsToOMEROKeyValuePairExporter keyValuePairExporter;
    private final AnnotationsToOMEROTagExporter tagExporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private DataExportExpressionParameter fileNameGenerator = new DataExportExpressionParameter("auto_file_name");

    public UploadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairExporter = new AnnotationsToOMEROKeyValuePairExporter();
        registerSubParameter(keyValuePairExporter);
        this.tagExporter = new AnnotationsToOMEROTagExporter();
        registerSubParameter(tagExporter);
    }

    public UploadOMEROImageAlgorithm(UploadOMEROImageAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.fileNameGenerator = new DataExportExpressionParameter(other.fileNameGenerator);
        this.keyValuePairExporter = new AnnotationsToOMEROKeyValuePairExporter(other.keyValuePairExporter);
        registerSubParameter(keyValuePairExporter);
        this.tagExporter = new AnnotationsToOMEROTagExporter(other.tagExporter);
        registerSubParameter(tagExporter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData imageData = iterationStep.getInputData("Images", OMEImageData.class, progressInfo);
        long datasetId = iterationStep.getInputData("Target dataset", OMERODatasetReferenceData.class, progressInfo).getDatasetId();

        // Export image
        Path tmpDirectory = getNewScratch();
        String fileName;
        if (StringUtils.isNullOrEmpty(fileNameGenerator.getExpression())) {
            fileName = "unnamed";
        } else {
            Path outputPath = fileNameGenerator.generatePath(tmpDirectory,
                    getProjectDirectory(),
                    getProjectDataDirs(),
                    imageData.toString(),
                    iterationStep.getInputRow("Images"),
                    new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));
            fileName = outputPath.getFileName().toString();
        }
        Path imagePath = PathUtils.ensureExtension(tmpDirectory.resolve(fileName), ".ome.tif", ".ome.tiff");
        progressInfo.log("Exporting image to " + imagePath);
        imageData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tmpDirectory), imagePath.getFileName().toString(), true, progressInfo);

        // Determine tags/kv-pairs
        Set<String> tags = new HashSet<>();
        Map<String, String> kvPairs = new HashMap<>();

        keyValuePairExporter.createKeyValuePairs(kvPairs, iterationStep.getMergedTextAnnotations().values());
        tagExporter.createTags(tags, iterationStep.getMergedTextAnnotations().values());

        // Upload to OMERO
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();

        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            DatasetData datasetData = gateway.getDataset(datasetId, -1);
            SecurityContext context = new SecurityContext(datasetData.getGroupId());
            try (OMEROImageUploader uploader = new OMEROImageUploader(credentials, environment.getEmail(), datasetId, context, progressInfo)) {
                List<Long> imageIds = uploader.upload(imagePath, tags, kvPairs, gateway, progressInfo);
                for (Long imageId : imageIds) {
                    ImageData uploadedImageData = gateway.getImage(imageId, -1);
                    iterationStep.addOutputData(getFirstOutputSlot(), new OMEROImageReferenceData(uploadedImageData, environment), progressInfo);
                }
            } catch (DSOutOfServiceException | DSAccessException | ServerError e) {
                throw new RuntimeException(e);
            }
        }

        try {
            PathUtils.deleteDirectoryRecursively(tmpDirectory, progressInfo.resolve("Cleanup"));
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @SetJIPipeDocumentation(name = "File name", description = "Expression that generates the file name for the OME TIFF to be uploaded. Please note that the directory will be ignored and 'unnamed' will be assumed if " +
            "no name is provided.")
    @JIPipeParameter("file-name-generator")
    public DataExportExpressionParameter getFileNameGenerator() {
        return fileNameGenerator;
    }

    @JIPipeParameter("file-name-generator")
    public void setFileNameGenerator(DataExportExpressionParameter fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @SetJIPipeDocumentation(name = "Export annotations as key-value pairs", description = "The following settings allow you to export annotations as key-value pairs")
    @JIPipeParameter("key-value-pair-exporter")
    public AnnotationsToOMEROKeyValuePairExporter getKeyValuePairExporter() {
        return keyValuePairExporter;
    }

    @SetJIPipeDocumentation(name = "Export list annotation as tag", description = "The following settings allow you to export a single list-like annotation as tag list.")
    @JIPipeParameter("tag-exporter")
    public AnnotationsToOMEROTagExporter getTagExporter() {
        return tagExporter;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(reportContext, this), environment);
    }
}
