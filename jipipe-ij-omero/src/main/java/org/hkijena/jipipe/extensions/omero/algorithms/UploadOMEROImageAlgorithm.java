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
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import omero.gateway.LoginCredentials;
import omero.model.Pixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataByMetadataExporter;
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
import org.hkijena.jipipe.extensions.omero.util.OMEROUploadToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
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

    public UploadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
        registerSubParameter(exporterSettings);
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path targetPath = RuntimeSettings.generateTempDirectory("OMERO-Upload");
        exportImages(targetPath, subProgress.resolve("Exporting images"), algorithmProgress, isCancelled);
        for (OMERODatasetReferenceData dataset : dataBatch.getInputData("Dataset", OMERODatasetReferenceData.class)) {
            uploadImages(targetPath, dataset.getDatasetId(), subProgress.resolve("Uploading"), algorithmProgress, isCancelled);
        }
    }

    private void uploadImages(Path targetPath, long datasetId, JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
        try {
            store = config.createStore();
            store.logVersionInfo(config.getIniVersionNumber());
            OMEROWrapper reader = new OMEROWrapper(config);
            ImportLibrary library = new ImportLibrary(store, reader);

            ErrorHandler handler = new ErrorHandler(config);
            library.addObserver(new OMEROUploadToJIPipeLogger(subStatus, algorithmProgress));

            ImportCandidates candidates = new ImportCandidates(reader, filePaths.toArray(new String[0]), handler);
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
//            if(!library.importCandidates(config, candidates))
//                throw new UserFriendlyRuntimeException("Error while uploading data to OMERO!",
//                        "There was an error while uploading.",
//                        getName(),
//                        "Cannot be determined.",
//                        "Check with OMERO insight if you are able to upload.");
            for (Pixels image : OMEROUtils.importImages(library, store, config, candidates)) {
                getFirstOutputSlot().addData(new OMEROImageReferenceData(image.getId().getValue()));
            }
            store.logout();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if(store != null)
                store.logout();
        }
    }

    private void exportImages(Path targetPath, JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Update the exporter settings
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            getFirstInputSlot().getData(row, OMEImageData.class).setExporterSettings(exporterSettings);
        }

        // Export to BioFormats
        algorithmProgress.accept(subStatus.resolve("Image files will be written into " + targetPath));
        exporter.writeToFolder(getFirstInputSlot(), targetPath, subStatus.resolve("Export images"), algorithmProgress, isCancelled);
    }

    public UploadOMEROImageAlgorithm(UploadOMEROImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        registerSubParameter(credentials);
        registerSubParameter(exporterSettings);
        registerSubParameter(exporter);
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
}
