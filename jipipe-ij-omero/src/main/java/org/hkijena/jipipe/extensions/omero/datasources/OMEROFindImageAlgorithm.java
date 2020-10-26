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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
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
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPredicatePair;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "List images", description = "Returns the ID(s) of images(s) according to search criteria. Requires project IDs as input.")
@JIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Datasets", autoCreate = true)
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindImageAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringPredicate.List imageNameFilters = new StringPredicate.List();
    private OptionalStringParameter projectNameAnnotation = new OptionalStringParameter("Project", true);
    private OptionalStringParameter datasetNameAnnotation = new OptionalStringParameter("Dataset", true);
    private OptionalStringParameter imageNameAnnotation = new OptionalStringParameter("Filename", true);
    private StringAndStringPredicatePair.List keyValuePairFilters = new StringAndStringPredicatePair.List();
    private boolean addKeyValuePairsAsAnnotations = true;
    private StringPredicate.List tagFilters = new StringPredicate.List();
    private OptionalStringParameter tagAnnotation = new OptionalStringParameter("Tags", true);

    public OMEROFindImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        Set<Long> datasetIds = new HashSet<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            datasetIds.add(getFirstInputSlot().getData(row, OMERODatasetReferenceData.class).getDatasetId());
        }

        LoginCredentials credentials = this.credentials.getCredentials();
        algorithmProgress.accept(subProgress.resolve("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost()));
        try (Gateway gateway = new Gateway(new OMEROToJIPipeLogger(subProgress, algorithmProgress))) {
            ExperimenterData user = gateway.connect(credentials);
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            MetadataFacility metadata = gateway.getFacility(MetadataFacility.class);
            for (Long datasetId : datasetIds) {
                DatasetData datasetData = browseFacility.getDatasets(context, Collections.singletonList(datasetId)).iterator().next();
                algorithmProgress.accept(subProgress.resolve("Listing datasets in dataset ID=" + datasetData.getId()));
                for (Object obj : datasetData.getImages()) {
                    if (obj instanceof ImageData) {
                        ImageData imageData = (ImageData) obj;
                        if (!imageNameFilters.isEmpty() && !imageNameFilters.test(imageData.getName())) {
                            continue;
                        }
                        Map<String, String> keyValuePairs = new HashMap<>();
                        if(!keyValuePairFilters.isEmpty() || addKeyValuePairsAsAnnotations) {
                            keyValuePairs = OMEROUtils.getKeyValuePairAnnotations(metadata, context, imageData);
                        }
                        if(!keyValuePairFilters.isEmpty()) {
                            if(!keyValuePairFilters.test(keyValuePairs, LogicalOperation.LogicalAnd, LogicalOperation.LogicalOr))
                                continue;
                        }
                        Set<String> tags = new HashSet<>();
                        if(!tagFilters.isEmpty() || tagAnnotation.isEnabled()) {
                            tags = OMEROUtils.getTagAnnotations(metadata, context, imageData);
                        }
                        if(!tagFilters.isEmpty() && !tagFilters.test(tags, LogicalOperation.LogicalOr, LogicalOperation.LogicalOr)) {
                            continue;
                        }
                        List<JIPipeAnnotation> annotations = new ArrayList<>();
                        if(addKeyValuePairsAsAnnotations) {
                            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                                annotations.add(new JIPipeAnnotation(entry.getKey(), entry.getValue()));
                            }
                        }
                        if(tagAnnotation.isEnabled()) {
                            List<String> sortedTags = tags.stream().sorted().collect(Collectors.toList());
                            String value = JsonUtils.toJsonString(sortedTags);
                            annotations.add(new JIPipeAnnotation(tagAnnotation.getContent(), value));
                        }
                        if (datasetNameAnnotation.isEnabled())
                            annotations.add(new JIPipeAnnotation(datasetNameAnnotation.getContent(), datasetData.getName()));
                        getFirstOutputSlot().addData(new OMEROImageReferenceData(imageData.getId()), annotations);
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OMEROFindImageAlgorithm(OMEROFindImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.imageNameFilters = new StringPredicate.List(other.imageNameFilters);
        this.datasetNameAnnotation = new OptionalStringParameter(other.datasetNameAnnotation);
        this.projectNameAnnotation = new OptionalStringParameter(other.projectNameAnnotation);
        this.imageNameAnnotation = new OptionalStringParameter(other.imageNameAnnotation);
        this.keyValuePairFilters = new StringAndStringPredicatePair.List(other.keyValuePairFilters);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagFilters = new StringPredicate.List(other.tagFilters);
        this.tagAnnotation = new OptionalStringParameter(other.tagAnnotation);
        registerSubParameter(credentials);
    }

    @JIPipeDocumentation(name = "Project name filters", description = "Filters for the project name. A project is returned if one of the filters apply. If the list is empty, all projects are returned.")
    @JIPipeParameter("project-name-filters")
    public StringPredicate.List getImageNameFilters() {
        return imageNameFilters;
    }

    @JIPipeParameter("project-name-filters")
    public void setImageNameFilters(StringPredicate.List imageNameFilters) {
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
        if(tagAnnotation.isEnabled()) {
            report.forCategory("Annotate with tags").checkNonEmpty(tagAnnotation.getContent(), this);
        }
    }

    @JIPipeDocumentation(name = "Annotate with dataset name", description = "Creates an annotation with the dataset name")
    @JIPipeParameter("dataset-name-annotation")
    public OptionalStringParameter getDatasetNameAnnotation() {
        return datasetNameAnnotation;
    }

    @JIPipeParameter("dataset-name-annotation")
    public void setDatasetNameAnnotation(OptionalStringParameter datasetNameAnnotation) {
        this.datasetNameAnnotation = datasetNameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with project name", description = "Creates an annotation with the project name")
    @JIPipeParameter("project-name-annotation")
    public OptionalStringParameter getProjectNameAnnotation() {
        return projectNameAnnotation;
    }

    @JIPipeParameter("project-name-annotation")
    public void setProjectNameAnnotation(OptionalStringParameter projectNameAnnotation) {
        this.projectNameAnnotation = projectNameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with image name", description = "Creates an annotation with the image name")
    @JIPipeParameter("image-name-annotation")
    public OptionalStringParameter getImageNameAnnotation() {
        return imageNameAnnotation;
    }

    @JIPipeParameter("image-name-annotation")
    public void setImageNameAnnotation(OptionalStringParameter imageNameAnnotation) {
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

    @JIPipeDocumentation(name = "Key-Value pair filters", description = "Filters projects by attached key value pairs. Filters with same keys are connected via an AND operation. Filters with different keys are connected via an OR operation. If the list is empty, no filtering is applied.")
    @JIPipeParameter("key-value-pair-filters")
    public StringAndStringPredicatePair.List getKeyValuePairFilters() {
        return keyValuePairFilters;
    }

    @JIPipeParameter("key-value-pair-filters")
    public void setKeyValuePairFilters(StringAndStringPredicatePair.List keyValuePairFilters) {
        this.keyValuePairFilters = keyValuePairFilters;
    }
}
