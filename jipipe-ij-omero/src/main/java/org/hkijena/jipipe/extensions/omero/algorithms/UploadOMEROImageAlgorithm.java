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

import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.MapAnnotationData;
import omero.model.MapAnnotation;
import omero.model.MapAnnotationI;
import omero.model.NamedValue;
import omero.model.Pixels;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUploadToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Upload to OMERO", description = "Uploads an image to OMERO.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Dataset", autoCreate = true)
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "ID", autoCreate = true)
public class UploadOMEROImageAlgorithm extends JIPipeMergingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private OMEExporterSettings exporterSettings = new OMEExporterSettings();
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private boolean uploadAnnotations = false;
    private AnnotationQueryExpression uploadedAnnotationsFilter = new AnnotationQueryExpression("TRUE");

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
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        List<OMEImageData> images = dataBatch.getInputData("Image", OMEImageData.class);
        ArrayList<JIPipeAnnotation> annotations = new ArrayList<>(dataBatch.getAnnotations().values());
        for (int index = 0; index < images.size(); index++) {
            Path targetPath = RuntimeSettings.generateTempDirectory("OMERO-Upload");
            exportImages(images.get(index), annotations, targetPath, subProgress.resolve("Exporting images"), algorithmProgress, isCancelled);
            for (OMERODatasetReferenceData dataset : dataBatch.getInputData("Dataset", OMERODatasetReferenceData.class)) {
                uploadImages(targetPath, annotations, dataset.getDatasetId(), subProgress.resolve("Uploading"), algorithmProgress, isCancelled);
            }
            try {
                FileUtils.deleteDirectory(targetPath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void uploadImages(Path targetPath, List<JIPipeAnnotation> annotations, long datasetId, JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        List<String> filePaths = PathUtils.findFilesByExtensionIn(targetPath, ".ome.tif").stream().map(Path::toString).collect(Collectors.toList());
        algorithmProgress.accept(subStatus.resolve("Uploading " + filePaths.size() + " files"));
        LoginCredentials credentials = this.credentials.getCredentials();
        ImportConfig config = new ome.formats.importer.ImportConfig();

        config.email.set(OMEROSettings.getInstance().getEmail());
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);

        config.hostname.set(credentials.getServer().getHost());
        config.port.set(credentials.getServer().getPort());
        config.username.set(credentials.getUser().getUsername());
        config.password.set(credentials.getUser().getPassword());

        config.target.set("Dataset:" + datasetId);

        OMEROMetadataStoreClient store = null;
        List<Long> uploadedImages = new ArrayList<>();
        try {
            store = config.createStore();
            store.logVersionInfo(config.getIniVersionNumber());
            OMEROWrapper reader = new OMEROWrapper(config);
            ImportLibrary library = new ImportLibrary(store, reader);

            ErrorHandler handler = new ErrorHandler(config);
            library.addObserver(new OMEROUploadToJIPipeLogger(subStatus, algorithmProgress));

            ImportCandidates candidates = new ImportCandidates(reader, filePaths.toArray(new String[0]), handler);
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
            for (Pixels image : OMEROUtils.importImages(library, store, config, candidates)) {
                uploadedImages.add(image.getId().getValue());
            }

            store.logout();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if(store != null)
                store.logout();
        }

        // Connect annotations
        if(uploadAnnotations) {
            try(Gateway gateway = new Gateway(new OMEROToJIPipeLogger(subStatus, algorithmProgress))) {
                ExperimenterData user = gateway.connect(credentials);
                SecurityContext context = new SecurityContext(user.getGroupId());
                BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
                DataManagerFacility dataManagerFacility = gateway.getFacility(DataManagerFacility.class);
                List<NamedValue> namedValues = new ArrayList<>();
                for (JIPipeAnnotation annotation : uploadedAnnotationsFilter.queryAll(annotations)) {
                    namedValues.add(new NamedValue(annotation.getName(), annotation.getValue()));
                }
                MapAnnotationData mapAnnotationData = new MapAnnotationData();
                mapAnnotationData.setContent(namedValues);
                mapAnnotationData.setNameSpace(MapAnnotationData.NS_CLIENT_CREATED);
                for (Long uploadedImage : uploadedImages) {
                    ImageData image = browseFacility.getImage(context, uploadedImage);
                    dataManagerFacility.attachAnnotation(context, mapAnnotationData, image);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Write to output
        for (Long uploadedImage : uploadedImages) {
            getFirstOutputSlot().addData(new OMEROImageReferenceData(uploadedImage), annotations);
        }
    }

    private void exportImages(OMEImageData image, List<JIPipeAnnotation> annotations, Path targetPath, JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeDataSlot dummy = new JIPipeDataSlot(new JIPipeDataSlotInfo(OMEImageData.class, JIPipeSlotType.Input, null), this);
        dummy.addData(image, annotations);
        image.setExporterSettings(exporterSettings);

        // Export to BioFormats
        algorithmProgress.accept(subStatus.resolve("Image files will be written into " + targetPath));
        exporter.writeToFolder(dummy, targetPath, subStatus.resolve("Export images"), algorithmProgress, isCancelled);
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
}
