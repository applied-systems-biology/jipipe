package org.hkijena.jipipe.extensions.omero.util;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ExperimenterData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class OMEROGateway implements AutoCloseable {

    private final LoginCredentials credentials;
    private final JIPipeProgressInfo progressInfo;
    private Gateway gateway;
    private SecurityContext context;
    private BrowseFacility browseFacility;
    private DataManagerFacility dataManagerFacility;
    private MetadataFacility metadata;

    public OMEROGateway(LoginCredentials credentials, JIPipeProgressInfo progressInfo) {
        this.credentials = credentials;
        this.progressInfo = progressInfo;
        initialize();
    }

    private void initialize() {
        gateway = new Gateway(new OMEROToJIPipeLogger(progressInfo));
        try {
            ExperimenterData user = gateway.connect(credentials);

            context = new SecurityContext(user.getGroupId());
            browseFacility = gateway.getFacility(BrowseFacility.class);
            dataManagerFacility = gateway.getFacility(DataManagerFacility.class);
            metadata = gateway.getFacility(MetadataFacility.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LoginCredentials getCredentials() {
        return credentials;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public SecurityContext getContext() {
        return context;
    }

    public BrowseFacility getBrowseFacility() {
        return browseFacility;
    }

    public DataManagerFacility getDataManagerFacility() {
        return dataManagerFacility;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void close() throws Exception {
        gateway.close();
    }

    public MetadataFacility getMetadata() {
        return metadata;
    }
}
