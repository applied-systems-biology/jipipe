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

package org.hkijena.jipipe.extensions.omero.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROImageUploader;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Upload to OMERO", description = "Uploads an image to OMERO.")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Dataset", autoCreate = true)
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "ID", autoCreate = true)
public class UploadOMEROImageAlgorithm extends JIPipeMergingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private OMEExporterSettings exporterSettings = new OMEExporterSettings();
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private boolean uploadAnnotations = false;
    private AnnotationQueryExpression uploadedAnnotationsFilter = new AnnotationQueryExpression("");

    private OMEROGateway currentGateway;
    private Map<Long, OMEROImageUploader> currentUploaders = new HashMap<>();

    public UploadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
        registerSubParameter(exporterSettings);
        registerSubParameter(exporter);
    }

    public UploadOMEROImageAlgorithm(UploadOMEROImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.uploadAnnotations = other.uploadAnnotations;
        this.uploadedAnnotationsFilter = new AnnotationQueryExpression(other.uploadedAnnotationsFilter);
        registerSubParameter(credentials);
        registerSubParameter(exporterSettings);
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        try {
            currentGateway = new OMEROGateway(credentials.getCredentials(), progressInfo);
            for (OMERODatasetReferenceData dataset : getInputSlot("Dataset").getAllData(OMERODatasetReferenceData.class, progressInfo)) {
                if(!currentUploaders.containsKey(dataset.getDatasetId())) {
                    int numThreads = getThreadPool() != null ? getThreadPool().getMaxThreads() : 1;
                    currentUploaders.put(dataset.getDatasetId(), new OMEROImageUploader(credentials.getCredentials(), dataset.getDatasetId(), numThreads, numThreads, progressInfo));
                }
            }
            super.runParameterSet(progressInfo, parameterAnnotations);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if(currentGateway != null) {
                try {
                    currentGateway.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (OMEROImageUploader uploader : currentUploaders.values()) {
                try {
                    uploader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<OMEImageData> images = dataBatch.getInputData("Image", OMEImageData.class, progressInfo);
        ArrayList<JIPipeAnnotation> annotations = new ArrayList<>(dataBatch.getGlobalAnnotations().values());
        for (OMEImageData image : images) {
            Path targetPath = getNewScratch();
            exportImages(image, annotations, targetPath, progressInfo);
            for (OMERODatasetReferenceData dataset : dataBatch.getInputData("Dataset", OMERODatasetReferenceData.class, progressInfo)) {
                uploadImages(targetPath, annotations, dataset.getDatasetId(), progressInfo);
            }
            try {
                PathUtils.deleteDirectoryRecursively(targetPath, progressInfo.resolve("Cleanup"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImages(Path targetPath, List<JIPipeAnnotation> annotations, long datasetId, JIPipeProgressInfo progressInfo) {
        List<Path> filePaths = PathUtils.findFilesByExtensionIn(targetPath, ".ome.tif");
        progressInfo.log("Uploading " + filePaths.size() + " files");

        List<JIPipeAnnotation> filteredAnnotations = uploadedAnnotationsFilter.queryAll(annotations);
        if(!uploadAnnotations) {
            filteredAnnotations = null;
        }

        OMEROImageUploader uploader = currentUploaders.get(datasetId);
        for (Path filePath : filePaths) {
            for (long imageId : uploader.upload(filePath, filteredAnnotations, currentGateway)) {
                getFirstOutputSlot().addData(new OMEROImageReferenceData(imageId), annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
            }
        }
    }

    private void exportImages(OMEImageData image, List<JIPipeAnnotation> annotations, Path targetPath, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dummy = new JIPipeDataSlot(new JIPipeDataSlotInfo(OMEImageData.class, JIPipeSlotType.Input), this);
        dummy.addData(image, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        image.setExporterSettings(exporterSettings);

        // Export to BioFormats
        progressInfo.log("Image files will be written into " + targetPath);
        exporter.writeToFolder(dummy, targetPath, progressInfo);
    }

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }

    @JIPipeDocumentation(name = "Exporter settings", description = "To upload the image to OMERO, it must be exported via Bio Formats. Use following settings to change the generated output.")
    @JIPipeParameter("exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }

    @JIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }

    @JIPipeDocumentation(name = "Upload annotations", description = "Uploads annotations as Key-Value pairs. Use the 'Uploaded annotations' setting to control which annotations to upload.")
    @JIPipeParameter("upload-annotations")
    public boolean isUploadAnnotations() {
        return uploadAnnotations;
    }

    @JIPipeParameter("upload-annotations")
    public void setUploadAnnotations(boolean uploadAnnotations) {
        this.uploadAnnotations = uploadAnnotations;
    }

    @JIPipeDocumentation(name = "Uploaded annotations", description = "Determines which annotations should be uploaded. " + AnnotationQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("uploaded-annotations")
    public AnnotationQueryExpression getUploadedAnnotationsFilter() {
        return uploadedAnnotationsFilter;
    }

    @JIPipeParameter("uploaded-annotations")
    public void setUploadedAnnotationsFilter(AnnotationQueryExpression uploadedAnnotationsFilter) {
        this.uploadedAnnotationsFilter = uploadedAnnotationsFilter;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }
}
