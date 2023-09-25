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

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringMapQueryExpression;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "List images", description = "Returns the ID(s) of images(s) according to search criteria. Requires project IDs as input.")
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Datasets", autoCreate = true)
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindImageAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
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
    }

    public OMEROFindImageAlgorithm(OMEROFindImageAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.imageNameFilters = new StringQueryExpression(other.imageNameFilters);
        this.datasetNameAnnotation = new OptionalAnnotationNameParameter(other.datasetNameAnnotation);
        this.projectNameAnnotation = new OptionalAnnotationNameParameter(other.projectNameAnnotation);
        this.imageNameAnnotation = new OptionalAnnotationNameParameter(other.imageNameAnnotation);
        this.keyValuePairFilters = new StringMapQueryExpression(other.keyValuePairFilters);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagFilters = new StringMapQueryExpression(other.tagFilters);
        this.tagAnnotation = new OptionalAnnotationNameParameter(other.tagAnnotation);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        Set<Long> datasetIds = new HashSet<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            datasetIds.add(getFirstInputSlot().getData(row, OMERODatasetReferenceData.class, progressInfo).getDatasetId());
        }

        LoginCredentials credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials()).toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (Long datasetId : datasetIds) {
                DatasetData datasetData = gateway.getDataset(datasetId, -1);
                SecurityContext context = new SecurityContext(datasetData.getGroupId());
                progressInfo.log("Listing images in dataset ID=" + datasetData.getId());
                for (Object obj : datasetData.getImages()) {
                    if (obj instanceof ImageData) {
                        ImageData imageData = (ImageData) obj;
                        if (!imageNameFilters.test(imageData.getName())) {
                            continue;
                        }
                        Map<String, String> keyValuePairs = OMEROUtils.getKeyValuePairAnnotations(gateway.getMetadata(), context, imageData);
                        if (!keyValuePairFilters.test(keyValuePairs))
                            continue;
                        Set<String> tags = OMEROUtils.getTagAnnotations(gateway.getMetadata(), context, imageData);
                        if (!tagFilters.test(tags)) {
                            continue;
                        }
                        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                        if (addKeyValuePairsAsAnnotations) {
                            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                                annotations.add(new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
                            }
                        }
                        if (tagAnnotation.isEnabled()) {
                            List<String> sortedTags = tags.stream().sorted().collect(Collectors.toList());
                            String value = JsonUtils.toJsonString(sortedTags);
                            annotations.add(new JIPipeTextAnnotation(tagAnnotation.getContent(), value));
                        }
                        if (datasetNameAnnotation.isEnabled())
                            annotations.add(new JIPipeTextAnnotation(datasetNameAnnotation.getContent(), datasetData.getName()));
                        getFirstOutputSlot().addData(new OMEROImageReferenceData(imageData.getId()), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Image name filters", description = "Filters for the image name. ")
    @JIPipeParameter("image-name-filters")
    public StringQueryExpression getImageNameFilters() {
        return imageNameFilters;
    }

    @JIPipeParameter("image-name-filters")
    public void setImageNameFilters(StringQueryExpression imageNameFilters) {
        this.imageNameFilters = imageNameFilters;
    }

    @JIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
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

    @JIPipeDocumentation(name = "Key-Value pair filters", description = "Filters projects by attached key value pairs. ")
    @JIPipeParameter("key-value-pair-filters")
    public StringMapQueryExpression getKeyValuePairFilters() {
        return keyValuePairFilters;
    }

    @JIPipeParameter("key-value-pair-filters")
    public void setKeyValuePairFilters(StringMapQueryExpression keyValuePairFilters) {
        this.keyValuePairFilters = keyValuePairFilters;
    }

    @JIPipeDocumentation(name = "Tag filters", description = "Filters by tag values. If no tag is matched, the data set is skipped. ")
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
