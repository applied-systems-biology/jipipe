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

    private final Map<Thread, OMEROGateway> currentGateways = new HashMap<>();
    private final Map<Thread, Map<Long, OMEROImageUploader>> currentUploaders = new HashMap<>();
    private OMEROCredentials credentials = new OMEROCredentials();
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private boolean uploadAnnotations = false;
    private AnnotationQueryExpression uploadedAnnotationsFilter = new AnnotationQueryExpression("");
    private JIPipeProgressInfo parameterProgressInfo;

    public UploadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
        registerSubParameter(exporter);
    }

    public UploadOMEROImageAlgorithm(UploadOMEROImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.uploadAnnotations = other.uploadAnnotations;
        this.uploadedAnnotationsFilter = new AnnotationQueryExpression(other.uploadedAnnotationsFilter);
        registerSubParameter(credentials);
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        this.parameterProgressInfo = progressInfo;
        super.runParameterSet(progressInfo, parameterAnnotations);
        for (OMEROGateway gateway : currentGateways.values()) {
            try {
                gateway.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Map<Long, OMEROImageUploader> threadUploaders : currentUploaders.values()) {
            for (OMEROImageUploader uploader : threadUploaders.values()) {
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
        // Create OMERO connection if needed
        OMEROImageUploader uploader;
        OMEROGateway gateway;
        synchronized (this) {
            gateway = currentGateways.getOrDefault(Thread.currentThread(), null);
            if (gateway == null) {
                parameterProgressInfo.log("Creating OMERO gateway for thread " + Thread.currentThread());
                gateway = new OMEROGateway(credentials.getCredentials(), parameterProgressInfo);
                currentGateways.put(Thread.currentThread(), gateway);
            }
            Map<Long, OMEROImageUploader> uploaderMap = currentUploaders.getOrDefault(Thread.currentThread(), null);
            if (uploaderMap == null) {
                uploaderMap = new HashMap<>();
                currentUploaders.put(Thread.currentThread(), uploaderMap);
            }
            uploader = uploaderMap.getOrDefault(datasetId, null);
            if (uploader == null) {
                parameterProgressInfo.log("Creating OMERO uploader for dataset=" + datasetId + " in thread " + Thread.currentThread());
                uploader = new OMEROImageUploader(credentials.getCredentials(), datasetId, 1, 1, parameterProgressInfo.resolve("Dataset " + datasetId));
                uploaderMap.put(datasetId, uploader);
            }
        }

        List<Path> filePaths = PathUtils.findFilesByExtensionIn(targetPath, ".ome.tif");
        progressInfo.log("Uploading " + filePaths.size() + " files");

        List<JIPipeAnnotation> filteredAnnotations = uploadedAnnotationsFilter.queryAll(annotations);
        if (!uploadAnnotations) {
            filteredAnnotations = null;
        }
        for (Path filePath : filePaths) {
            for (long imageId : uploader.upload(filePath, filteredAnnotations, gateway)) {
                getFirstOutputSlot().addData(new OMEROImageReferenceData(imageId), annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
            }
        }
    }

    private void exportImages(OMEImageData image, List<JIPipeAnnotation> annotations, Path targetPath, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dummy = new JIPipeDataSlot(new JIPipeDataSlotInfo(OMEImageData.class, JIPipeSlotType.Input), this);
        dummy.addData(image, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);

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
        return true;
    }
}
