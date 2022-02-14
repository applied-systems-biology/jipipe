package org.hkijena.jipipe.extensions.omero.util;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.util.Collection;
import java.util.Collections;

public class OMEROGateway implements AutoCloseable {

    private final LoginCredentials credentials;
    private final JIPipeProgressInfo progressInfo;
    private ExperimenterData user;
    private Gateway gateway;
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
            user = gateway.connect(credentials);
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

    public BrowseFacility getBrowseFacility() {
        return browseFacility;
    }

    public DataManagerFacility getDataManagerFacility() {
        return dataManagerFacility;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public ExperimenterData getUser() {
        return user;
    }

    @Override
    public void close() {
        try {
            gateway.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MetadataFacility getMetadata() {
        return metadata;
    }

    /**
     * Gets a project
     *
     * @param projectId the project ID
     * @param groupId   the group ID. Can be negative; then all available group Ids will be tested
     * @return the project or null if it was not found
     */
    public ProjectData getProject(long projectId, long groupId) {
        if (groupId >= 0) {
            try {
                Collection<ProjectData> projects = browseFacility.getProjects(new SecurityContext(groupId), Collections.singletonList(projectId));
                if (!projects.isEmpty()) {
                    return projects.iterator().next();
                }
            } catch (DSOutOfServiceException | DSAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (GroupData group : user.getGroups()) {
                try {
                    Collection<ProjectData> projects = browseFacility.getProjects(new SecurityContext(group.getId()), Collections.singletonList(projectId));
                    if (!projects.isEmpty()) {
                        return projects.iterator().next();
                    }
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    /**
     * Gets a dataset
     *
     * @param datasetId the project ID
     * @param groupId   the group ID. Can be negative; then all available group Ids will be tested
     * @return the project or null if it was not found
     */
    public DatasetData getDataset(long datasetId, long groupId) {
        if (groupId >= 0) {
            try {
                Collection<DatasetData> datasets = browseFacility.getDatasets(new SecurityContext(groupId), Collections.singletonList(datasetId));
                if (!datasets.isEmpty()) {
                    return datasets.iterator().next();
                }
            } catch (DSOutOfServiceException | DSAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (GroupData group : user.getGroups()) {
                try {
                    Collection<DatasetData> datasets = browseFacility.getDatasets(new SecurityContext(group.getId()), Collections.singletonList(datasetId));
                    if (!datasets.isEmpty()) {
                        return datasets.iterator().next();
                    }
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    /**
     * Gets a dataset
     *
     * @param imageId the image ID
     * @param groupId the group ID. Can be negative; then all available group Ids will be tested
     * @return the project or null if it was not found
     */
    public ImageData getImage(long imageId, long groupId) {
        if (groupId >= 0) {
            try {
                Collection<ImageData> images = browseFacility.getImages(new SecurityContext(groupId), Collections.singletonList(imageId));
                if (!images.isEmpty()) {
                    return images.iterator().next();
                }
            } catch (DSOutOfServiceException | DSAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (GroupData group : user.getGroups()) {
                try {
                    Collection<ImageData> images = browseFacility.getImages(new SecurityContext(group.getId()), Collections.singletonList(imageId));
                    if (!images.isEmpty()) {
                        return images.iterator().next();
                    }
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
