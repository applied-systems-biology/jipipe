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
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPredicatePairParameter;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "List projects", description = "Returns the ID(s) of project(s) according to search criteria.")
@JIPipeOutputSlot(value = OMEROProjectReferenceData.class, slotName = "Projects", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindProjectAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringPredicate.List projectNameFilters = new StringPredicate.List();
    private StringAndStringPredicatePairParameter.List keyValuePairFilters = new StringAndStringPredicatePairParameter.List();
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalStringParameter projectNameAnnotation = new OptionalStringParameter("Project", true);
    private StringPredicate.List tagFilters = new StringPredicate.List();
    private OptionalStringParameter tagAnnotation = new OptionalStringParameter("Tags", true);

    public OMEROFindProjectAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        LoginCredentials credentials = this.credentials.getCredentials();
        algorithmProgress.accept(subProgress.resolve("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost()));
        try(Gateway gateway = new Gateway(new OMEROToJIPipeLogger(subProgress, algorithmProgress))) {
            ExperimenterData user = gateway.connect(credentials);
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            MetadataFacility metadata = gateway.getFacility(MetadataFacility.class);
            algorithmProgress.accept(subProgress.resolve("Listing projects"));
            try {
                for (ProjectData project : browseFacility.getProjects(context)) {
                    if(!projectNameFilters.isEmpty() && !projectNameFilters.test(project.getName())) {
                        continue;
                    }
                    Map<String, String> keyValuePairs = new HashMap<>();
                    if(!keyValuePairFilters.isEmpty() || addKeyValuePairsAsAnnotations) {
                        keyValuePairs = OMEROUtils.getKeyValuePairAnnotations(metadata, context, project);
                    }
                    if(!keyValuePairFilters.isEmpty()) {
                        if(!keyValuePairFilters.test(keyValuePairs, LogicalOperation.LogicalAnd, LogicalOperation.LogicalOr))
                            continue;
                    }
                    Set<String> tags = new HashSet<>();
                    if(!tagFilters.isEmpty() || tagAnnotation.isEnabled()) {
                        tags = OMEROUtils.getTagAnnotations(metadata, context, project);
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
                    if(projectNameAnnotation.isEnabled()) {
                        annotations.add(new JIPipeAnnotation(projectNameAnnotation.getContent(), project.getName()));
                    }
                    getFirstOutputSlot().addData(new OMEROProjectReferenceData(project.getId()), annotations);
                }
            } catch (DSOutOfServiceException | DSAccessException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OMEROFindProjectAlgorithm(OMEROFindProjectAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.projectNameFilters = new StringPredicate.List(other.projectNameFilters);
        this.keyValuePairFilters = new StringAndStringPredicatePairParameter.List(other.keyValuePairFilters);
        this.projectNameAnnotation = new OptionalStringParameter(other.projectNameAnnotation);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagFilters = new StringPredicate.List(other.tagFilters);
        this.tagAnnotation = new OptionalStringParameter(other.tagAnnotation);
        registerSubParameter(credentials);
    }

    @JIPipeDocumentation(name = "Project name filters", description = "Filters for the project name. A project is returned if one of the filters apply. If the list is empty, all projects are returned.")
    @JIPipeParameter("project-name-filters")
    public StringPredicate.List getProjectNameFilters() {
        return projectNameFilters;
    }

    @JIPipeParameter("project-name-filters")
    public void setProjectNameFilters(StringPredicate.List projectNameFilters) {
        this.projectNameFilters = projectNameFilters;
    }

    @JIPipeDocumentation(name = "Key-Value pair filters", description = "Filters projects by attached key value pairs. Filters with same keys are connected via an AND operation. Filters with different keys are connected via an OR operation. If the list is empty, no filtering is applied.")
    @JIPipeParameter("key-value-pair-filters")
    public StringAndStringPredicatePairParameter.List getKeyValuePairFilters() {
        return keyValuePairFilters;
    }

    @JIPipeParameter("key-value-pair-filters")
    public void setKeyValuePairFilters(StringAndStringPredicatePairParameter.List keyValuePairFilters) {
        this.keyValuePairFilters = keyValuePairFilters;
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
        if(projectNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with name").checkNonEmpty(projectNameAnnotation.getContent(), this);
        }
        if(tagAnnotation.isEnabled()) {
            report.forCategory("Annotate with tags").checkNonEmpty(tagAnnotation.getContent(), this);
        }
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

    @JIPipeDocumentation(name = "Add Key-Value pairs as annotations", description = "Adds OMERO project annotations as JIPipe annotations")
    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public boolean isAddKeyValuePairsAsAnnotations() {
        return addKeyValuePairsAsAnnotations;
    }

    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public void setAddKeyValuePairsAsAnnotations(boolean addKeyValuePairsAsAnnotations) {
        this.addKeyValuePairsAsAnnotations = addKeyValuePairsAsAnnotations;
    }

    @JIPipeDocumentation(name = "Tag filters", description = "Filters by tag values. Filters are connected via OR. If the list is empty, no filtering is applied.")
    @JIPipeParameter("tag-filters")
    public StringPredicate.List getTagFilters() {
        return tagFilters;
    }

    @JIPipeParameter("tag-filters")
    public void setTagFilters(StringPredicate.List tagFilters) {
        this.tagFilters = tagFilters;
    }

    @JIPipeDocumentation(name = "Annotate with tags", description = "Creates an annotation with given key and writes the tags into them in JSON format.")
    @JIPipeParameter("tag-annotation")
    public OptionalStringParameter getTagAnnotation() {
        return tagAnnotation;
    }

    @JIPipeParameter("tag-annotation")
    public void setTagAnnotation(OptionalStringParameter tagAnnotation) {
        this.tagAnnotation = tagAnnotation;
    }
}
