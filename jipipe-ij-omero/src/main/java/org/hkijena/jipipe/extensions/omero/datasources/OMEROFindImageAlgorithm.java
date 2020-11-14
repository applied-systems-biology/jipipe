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

package org.hkijena.jipipe.extensions.omero.datasources;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.expressions.StringMapQueryExpression;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "List images", description = "Returns the ID(s) of images(s) according to search criteria. Requires project IDs as input.")
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Datasets", autoCreate = true)
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindImageAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringQueryExpression imageNameFilters = new StringQueryExpression("");
    private OptionalAnnotationNameParameter projectNameAnnotation = new OptionalAnnotationNameParameter("Project", true);
    private OptionalAnnotationNameParameter datasetNameAnnotation = new OptionalAnnotationNameParameter("Dataset", true);
    private OptionalAnnotationNameParameter imageNameAnnotation = new OptionalAnnotationNameParameter("Filename", true);
    private StringMapQueryExpression keyValuePairFilters = new StringMapQueryExpression("");
    private boolean addKeyValuePairsAsAnnotations = true;
    private StringMapQueryExpression tagFilters = new StringMapQueryExpression("");
    private OptionalAnnotationNameParameter tagAnnotation = new OptionalAnnotationNameParameter("Tags", true);

    public OMEROFindImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public OMEROFindImageAlgorithm(OMEROFindImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.imageNameFilters = new StringQueryExpression(other.imageNameFilters);
        this.datasetNameAnnotation = new OptionalAnnotationNameParameter(other.datasetNameAnnotation);
        this.projectNameAnnotation = new OptionalAnnotationNameParameter(other.projectNameAnnotation);
        this.imageNameAnnotation = new OptionalAnnotationNameParameter(other.imageNameAnnotation);
        this.keyValuePairFilters = new StringMapQueryExpression(other.keyValuePairFilters);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagFilters = new StringMapQueryExpression(other.tagFilters);
        this.tagAnnotation = new OptionalAnnotationNameParameter(other.tagAnnotation);
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progress, List<JIPipeAnnotation> parameterAnnotations) {
        Set<Long> datasetIds = new HashSet<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            datasetIds.add(getFirstInputSlot().getData(row, OMERODatasetReferenceData.class).getDatasetId());
        }

        LoginCredentials credentials = this.credentials.getCredentials();
        progress.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (Gateway gateway = new Gateway(new OMEROToJIPipeLogger(progress))) {
            ExperimenterData user = gateway.connect(credentials);
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            MetadataFacility metadata = gateway.getFacility(MetadataFacility.class);
            for (Long datasetId : datasetIds) {
                DatasetData datasetData = browseFacility.getDatasets(context, Collections.singletonList(datasetId)).iterator().next();
                progress.log("Listing datasets in dataset ID=" + datasetData.getId());
                for (Object obj : datasetData.getImages()) {
                    if (obj instanceof ImageData) {
                        ImageData imageData = (ImageData) obj;
                        if (!imageNameFilters.test(imageData.getName())) {
                            continue;
                        }
                        Map<String, String> keyValuePairs = OMEROUtils.getKeyValuePairAnnotations(metadata, context, imageData);
                        if (!keyValuePairFilters.test(keyValuePairs))
                            continue;
                        Set<String> tags = OMEROUtils.getTagAnnotations(metadata, context, imageData);
                        if (!tagFilters.test(tags)) {
                            continue;
                        }
                        List<JIPipeAnnotation> annotations = new ArrayList<>();
                        if (addKeyValuePairsAsAnnotations) {
                            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                                annotations.add(new JIPipeAnnotation(entry.getKey(), entry.getValue()));
                            }
                        }
                        if (tagAnnotation.isEnabled()) {
                            List<String> sortedTags = tags.stream().sorted().collect(Collectors.toList());
                            String value = JsonUtils.toJsonString(sortedTags);
                            annotations.add(new JIPipeAnnotation(tagAnnotation.getContent(), value));
                        }
                        if (datasetNameAnnotation.isEnabled())
                            annotations.add(new JIPipeAnnotation(datasetNameAnnotation.getContent(), datasetData.getName()));
                        getFirstOutputSlot().addData(new OMEROImageReferenceData(imageData.getId()), annotations, JIPipeAnnotationMergeStrategy.Merge);
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Image name filters", description = "Filters for the image name. " + StringMapQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("image-name-filters")
    public StringQueryExpression getImageNameFilters() {
        return imageNameFilters;
    }

    @JIPipeParameter("image-name-filters")
    public void setImageNameFilters(StringQueryExpression imageNameFilters) {
        this.imageNameFilters = imageNameFilters;
    }

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (datasetNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with dataset name").checkNonEmpty(datasetNameAnnotation.getContent(), this);
        }
        if (projectNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with project name").checkNonEmpty(projectNameAnnotation.getContent(), this);
        }
        if (imageNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with file name").checkNonEmpty(imageNameAnnotation.getContent(), this);
        }
        if (tagAnnotation.isEnabled()) {
            report.forCategory("Annotate with tags").checkNonEmpty(tagAnnotation.getContent(), this);
        }
    }

    @JIPipeDocumentation(name = "Annotate with dataset name", description = "Creates an annotation with the dataset name")
    @JIPipeParameter("dataset-name-annotation")
    public OptionalAnnotationNameParameter getDatasetNameAnnotation() {
        return datasetNameAnnotation;
    }

    @JIPipeParameter("dataset-name-annotation")
    public void setDatasetNameAnnotation(OptionalAnnotationNameParameter datasetNameAnnotation) {
        this.datasetNameAnnotation = datasetNameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with project name", description = "Creates an annotation with the project name")
    @JIPipeParameter("project-name-annotation")
    public OptionalAnnotationNameParameter getProjectNameAnnotation() {
        return projectNameAnnotation;
    }

    @JIPipeParameter("project-name-annotation")
    public void setProjectNameAnnotation(OptionalAnnotationNameParameter projectNameAnnotation) {
        this.projectNameAnnotation = projectNameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with image name", description = "Creates an annotation with the image name")
    @JIPipeParameter("image-name-annotation")
    public OptionalAnnotationNameParameter getImageNameAnnotation() {
        return imageNameAnnotation;
    }

    @JIPipeParameter("image-name-annotation")
    public void setImageNameAnnotation(OptionalAnnotationNameParameter imageNameAnnotation) {
        this.imageNameAnnotation = imageNameAnnotation;
    }

    @JIPipeDocumentation(name = "Add Key-Value pairs as annotations", description = "Adds OMERO project annotations as JIPipe annotations")
    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public boolean isAddKeyValuePairsAsAnnotations() {
        return addKeyValuePairsAsAnnotations;
    }

    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public void setAddKeyValuePairsAsAnnotations(boolean addKeyValuePairsAsAnnotations) {
        this.addKeyValuePairsAsAnnotations = addKeyValuePairsAsAnnotations;
    }

    @JIPipeDocumentation(name = "Key-Value pair filters", description = "Filters projects by attached key value pairs. " + StringMapQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("key-value-pair-filters")
    public StringMapQueryExpression getKeyValuePairFilters() {
        return keyValuePairFilters;
    }

    @JIPipeParameter("key-value-pair-filters")
    public void setKeyValuePairFilters(StringMapQueryExpression keyValuePairFilters) {
        this.keyValuePairFilters = keyValuePairFilters;
    }

    @JIPipeDocumentation(name = "Tag filters", description = "Filters by tag values. If no tag is matched, the data set is skipped. " + StringMapQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("tag-filters")
    public StringMapQueryExpression getTagFilters() {
        return tagFilters;
    }

    @JIPipeParameter("tag-filters")
    public void setTagFilters(StringMapQueryExpression tagFilters) {
        this.tagFilters = tagFilters;
    }

    @JIPipeDocumentation(name = "Annotate with tags", description = "Creates an annotation with given key and writes the tags into them in JSON format.")
    @JIPipeParameter("tag-annotation")
    public OptionalAnnotationNameParameter getTagAnnotation() {
        return tagAnnotation;
    }

    @JIPipeParameter("tag-annotation")
    public void setTagAnnotation(OptionalAnnotationNameParameter tagAnnotation) {
        this.tagAnnotation = tagAnnotation;
    }
}
