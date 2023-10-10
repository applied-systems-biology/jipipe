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

import omero.ServerError;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
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

@JIPipeDocumentation(name = "Upload image to OMERO", description = "Uploads an image to OMERO.")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Images", autoCreate = true, description = "The image(s) to upload")
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Target dataset", autoCreate = true, description = "The data set where the image(s) will be stored")
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", autoCreate = true, description = "Reference to the uploaded image(s)")
public class UploadOMEROImageAlgorithm extends JIPipeIteratingAlgorithm {
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private DataExportExpressionParameter fileNameGenerator = new DataExportExpressionParameter("auto_file_name");

    private final AnnotationsToOMEROKeyValuePairExporter keyValuePairExporter;
    private final AnnotationsToOMEROTagExporter tagExporter;

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEImageData imageData = dataBatch.getInputData("Images", OMEImageData.class, progressInfo);
        long datasetId = dataBatch.getInputData("Target dataset", OMERODatasetReferenceData.class, progressInfo).getDatasetId();

        // Export image
        Path tmpDirectory = getNewScratch();
        String fileName;
        if(StringUtils.isNullOrEmpty(fileNameGenerator.getExpression())) {
            fileName = "unnamed";
        }
        else {
            Path outputPath = fileNameGenerator.generatePath(tmpDirectory,
                    getProjectDirectory(),
                    getProjectDataDirs(),
                    imageData.toString(),
                    dataBatch.getInputRow("Images"),
                    new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));
            fileName = outputPath.getFileName().toString();
        }
        Path imagePath = PathUtils.ensureExtension(tmpDirectory.resolve(fileName), ".ome.tif", ".ome.tiff");
        progressInfo.log("Exporting image to " + imagePath);
        imageData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tmpDirectory), imagePath.getFileName().toString(), true, progressInfo);

        // Determine tags/kv-pairs
        Set<String> tags = new HashSet<>();
        Map<String, String> kvPairs = new HashMap<>();

        keyValuePairExporter.createKeyValuePairs(kvPairs, dataBatch.getMergedTextAnnotations().values());
        tagExporter.createTags(tags,  dataBatch.getMergedTextAnnotations().values());

        // Upload to OMERO
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();

        try(OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            DatasetData datasetData = gateway.getDataset(datasetId, -1);
            SecurityContext context = new SecurityContext(datasetData.getGroupId());
            try (OMEROImageUploader uploader = new OMEROImageUploader(credentials, environment.getEmail(), datasetId, context, progressInfo)) {
                List<Long> imageIds = uploader.upload(imagePath, tags, kvPairs, gateway, progressInfo);
                for (Long imageId : imageIds) {
                    ImageData uploadedImageData = gateway.getImage(imageId, -1);
                    dataBatch.addOutputData(getFirstOutputSlot(), new OMEROImageReferenceData(uploadedImageData, environment), progressInfo);
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

    @JIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }

    @JIPipeDocumentation(name = "File name", description = "Expression that generates the file name for the OME TIFF to be uploaded. Please note that the directory will be ignored and 'unnamed' will be assumed if " +
            "no name is provided.")
    @JIPipeParameter("file-name-generator")
    public DataExportExpressionParameter getFileNameGenerator() {
        return fileNameGenerator;
    }

    @JIPipeParameter("file-name-generator")
    public void setFileNameGenerator(DataExportExpressionParameter fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @JIPipeDocumentation(name = "Export annotations as key-value pairs", description = "The following settings allow you to export annotations as key-value pairs")
    @JIPipeParameter("key-value-pair-exporter")
    public AnnotationsToOMEROKeyValuePairExporter getKeyValuePairExporter() {
        return keyValuePairExporter;
    }

    @JIPipeDocumentation(name = "Export list annotation as tag", description = "The following settings allow you to export a single list-like annotation as tag list.")
    @JIPipeParameter("tag-exporter")
    public AnnotationsToOMEROTagExporter getTagExporter() {
        return tagExporter;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(context, this), environment);
    }
}
