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

package org.hkijena.jipipe.plugins.omero.util;

import omero.ServerError;
import omero.api.IQueryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.*;
import omero.model.IObject;
import omero.model.NamedValue;
import omero.model.TagAnnotation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.util.*;

public class OMEROGateway implements AutoCloseable {

    private final LoginCredentials credentials;
    private final JIPipeProgressInfo progressInfo;
    private ExperimenterData user;
    private Gateway gateway;
    private BrowseFacility browseFacility;
    private DataManagerFacility dataManagerFacility;
    private MetadataFacility metadataFacility;

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
            metadataFacility = gateway.getFacility(MetadataFacility.class);

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

    public MetadataFacility getMetadataFacility() {
        return metadataFacility;
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

    public Map<String, TagAnnotationData> getKnownTags(SecurityContext context) throws DSOutOfServiceException, ServerError {
        IQueryPrx qs = gateway.getQueryService(context);
        List<IObject> tmp = qs.findAll(TagAnnotation.class.getSimpleName(), null);
        Map<String, TagAnnotationData> knownTags = new HashMap<>();
        for (IObject object : tmp) {
            TagAnnotationData tagAnnotationData = new TagAnnotationData((TagAnnotation) object);
            knownTags.put(tagAnnotationData.getTagValue(), tagAnnotationData);
        }
        return knownTags;
    }

    public void attachKeyValuePairs(Map<String, String> keyValuePairs, DataObject data, SecurityContext context) throws DSOutOfServiceException, DSAccessException {
        List<NamedValue> namedValues = new ArrayList<>();
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            namedValues.add(new NamedValue(entry.getKey(), entry.getValue()));
        }
        MapAnnotationData mapAnnotationData = new MapAnnotationData();
        mapAnnotationData.setContent(namedValues);
        mapAnnotationData.setNameSpace(MapAnnotationData.NS_CLIENT_CREATED);
        dataManagerFacility.attachAnnotation(context, mapAnnotationData, data);
    }

    public void attachTags(Set<String> tags, DataObject data, SecurityContext context) throws DSOutOfServiceException, DSAccessException, ServerError {
        Map<String, TagAnnotationData> knownTags = getKnownTags(context);
        for (String tag : tags) {
            TagAnnotationData tagAnnotationData = knownTags.getOrDefault(tag, null);
            if (tagAnnotationData == null) {
                tagAnnotationData = new TagAnnotationData(tag);
            }
            dataManagerFacility.attachAnnotation(context, tagAnnotationData, data);
        }
    }
}
