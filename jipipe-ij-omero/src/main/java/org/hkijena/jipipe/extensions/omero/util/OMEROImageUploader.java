package org.hkijena.jipipe.extensions.omero.util;

import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import omero.gateway.LoginCredentials;
import omero.gateway.model.ImageData;
import omero.gateway.model.MapAnnotationData;
import omero.model.NamedValue;
import omero.model.Pixels;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OMEROImageUploader implements AutoCloseable {
    private final LoginCredentials credentials;
    private final long datasetId;
    private final int numProcessingThreads;
    private final int numUploadThreads;
    private final JIPipeProgressInfo progressInfo;
    private OMEROMetadataStoreClient store;
    private OMEROWrapper reader;
    private ErrorHandler handler;
    private ImportLibrary library;
    private ImportConfig config;

    public OMEROImageUploader(LoginCredentials credentials, long datasetId, int numProcessingThreads, int numUploadThreads, JIPipeProgressInfo progressInfo) {
        this.credentials = credentials;
        this.datasetId = datasetId;
        this.numProcessingThreads = numProcessingThreads;
        this.numUploadThreads = numUploadThreads;
        this.progressInfo = progressInfo;
        initialize();
    }

    private void initialize() {
        config = new ImportConfig();
        config.email.set(OMEROSettings.getInstance().getEmail());
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);
        config.parallelFileset.set(numProcessingThreads);
        config.parallelUpload.set(numUploadThreads);
        config.hostname.set(credentials.getServer().getHost());
        config.port.set(credentials.getServer().getPort());
        config.username.set(credentials.getUser().getUsername());
        config.password.set(credentials.getUser().getPassword());

        config.target.set("Dataset:" + datasetId);

        try {
            store = config.createStore();
            store.logVersionInfo(config.getIniVersionNumber());

            reader = new OMEROWrapper(config);
            library = new ImportLibrary(store, reader);

            handler = new ErrorHandler(config);
            library.addObserver(new OMEROUploadToJIPipeLogger(progressInfo));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Long> upload(Path imagePath, List<JIPipeAnnotation> annotationList, OMEROGateway gateway) {
        ImportCandidates candidates = new ImportCandidates(reader, new String[] {imagePath.toString()}, handler);
        reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
        List<Long> uploadedImages = new ArrayList<>();
        for (Pixels image : OMEROUtils.importImages(library, store, config, candidates)) {
            uploadedImages.add(image.getId().getValue());
        }
        if(annotationList != null && !annotationList.isEmpty()) {
            List<NamedValue> namedValues = new ArrayList<>();
            for (JIPipeAnnotation annotation : annotationList) {
                namedValues.add(new NamedValue(annotation.getName(), annotation.getValue()));
            }
            MapAnnotationData mapAnnotationData = new MapAnnotationData();
            mapAnnotationData.setContent(namedValues);
            mapAnnotationData.setNameSpace(MapAnnotationData.NS_CLIENT_CREATED);
            for (Long uploadedImage : uploadedImages) {
                try {
                    ImageData image = gateway.getBrowseFacility().getImage(gateway.getContext(), uploadedImage);
                    gateway.getDataManagerFacility().attachAnnotation(gateway.getContext(), mapAnnotationData, image);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return uploadedImages;
    }

    @Override
    public void close() throws Exception {
        if(store != null) {
            store.logout();
            store = null;
        }
    }
}
