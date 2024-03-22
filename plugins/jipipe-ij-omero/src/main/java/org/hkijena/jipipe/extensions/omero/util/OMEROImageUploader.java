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

package org.hkijena.jipipe.extensions.omero.util;

import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import omero.ServerError;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ImageData;
import omero.model.Pixels;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OMEROImageUploader implements AutoCloseable {
    private final LoginCredentials credentials;
    private final String eMail;
    private final long datasetId;
    private final SecurityContext context;
    private final JIPipeProgressInfo progressInfo;
    private OMEROMetadataStoreClient store;
    private OMEROWrapper reader;
    private ErrorHandler handler;
    private ImportLibrary library;
    private ImportConfig config;

    public OMEROImageUploader(LoginCredentials credentials, String eMail, long datasetId, SecurityContext context, JIPipeProgressInfo progressInfo) {
        this.credentials = credentials;
        this.eMail = eMail;
        this.datasetId = datasetId;
        this.context = context;
        this.progressInfo = progressInfo;
        initialize();
    }

    private void initialize() {
        config = new ImportConfig();
        config.email.set(eMail);
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);
        config.parallelFileset.set(1);
        config.parallelUpload.set(1);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Long> upload(Path imagePath, Set<String> tags, Map<String, String> keyValuePairs, OMEROGateway gateway, JIPipeProgressInfo progressInfo) throws DSOutOfServiceException, DSAccessException, ServerError {
        ImportCandidates candidates = new ImportCandidates(reader, new String[]{imagePath.toString()}, handler);
        reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
        List<Long> uploadedImages = new ArrayList<>();

        progressInfo.log("Uploading data ...");
        for (Pixels image : OMEROUtils.importImages(library, store, config, candidates)) {
            uploadedImages.add(image.getId().getValue());
        }

        if (!keyValuePairs.isEmpty()) {
            progressInfo.log("Attaching key-value pairs ...");

            for (Long uploadedImage : uploadedImages) {
                ImageData image = gateway.getBrowseFacility().getImage(context, uploadedImage);
                gateway.attachKeyValuePairs(keyValuePairs, image, context);
            }
        }

        if (!tags.isEmpty()) {
            progressInfo.log("Attaching tags ...");

            for (Long uploadedImage : uploadedImages) {
                ImageData image = gateway.getBrowseFacility().getImage(context, uploadedImage);
                gateway.attachTags(tags, image, context);
            }
        }

        return uploadedImages;
    }

    @Override
    public void close() {
        if (store != null) {
            store.logout();
            store = null;
        }
    }
}
