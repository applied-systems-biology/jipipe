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

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringMapQueryExpression;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "List projects", description = "Returns the ID(s) of project(s) according to search criteria.")
@JIPipeInputSlot(value = OMEROGroupReferenceData.class, slotName = "Group", autoCreate = true)
@JIPipeOutputSlot(value = OMEROProjectReferenceData.class, slotName = "Projects", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindProjectAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringQueryExpression projectNameFilters = new StringQueryExpression("");
    private StringMapQueryExpression keyValuePairFilters = new StringMapQueryExpression("");
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalAnnotationNameParameter projectNameAnnotation = new OptionalAnnotationNameParameter("Project", true);
    private StringMapQueryExpression tagFilters = new StringMapQueryExpression("");
    private OptionalAnnotationNameParameter tagAnnotation = new OptionalAnnotationNameParameter("Tags", true);

    public OMEROFindProjectAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public OMEROFindProjectAlgorithm(OMEROFindProjectAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.projectNameFilters = new StringQueryExpression(other.projectNameFilters);
        this.keyValuePairFilters = new StringMapQueryExpression(other.keyValuePairFilters);
        this.projectNameAnnotation = new OptionalAnnotationNameParameter(other.projectNameAnnotation);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagFilters = new StringMapQueryExpression(other.tagFilters);
        this.tagAnnotation = new OptionalAnnotationNameParameter(other.tagAnnotation);
        registerSubParameter(credentials);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        long groupId = dataBatch.getInputData("Group", OMEROGroupReferenceData.class, progressInfo).getGroupId();
        try (OMEROGateway gateway = new OMEROGateway(credentials.getCredentials(), progressInfo)) {
            SecurityContext context = new SecurityContext(groupId);
            try {
                for (ProjectData project : gateway.getBrowseFacility().getProjects(context)) {
                    if (!projectNameFilters.test(project.getName())) {
                        continue;
                    }
                    Map<String, String> keyValuePairs = OMEROUtils.getKeyValuePairAnnotations(gateway.getMetadata(), context, project);
                    if (!keyValuePairFilters.test(keyValuePairs))
                        continue;
                    Set<String> tags = OMEROUtils.getTagAnnotations(gateway.getMetadata(), context, project);
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
                    if (projectNameAnnotation.isEnabled()) {
                        annotations.add(new JIPipeTextAnnotation(projectNameAnnotation.getContent(), project.getName()));
                    }
                    getFirstOutputSlot().addData(new OMEROProjectReferenceData(project.getId()), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }
            } catch (DSOutOfServiceException | DSAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @JIPipeDocumentation(name = "Project name filters", description = "Filters for the project name. ")
    @JIPipeParameter("project-name-filters")
    public StringQueryExpression getProjectNameFilters() {
        return projectNameFilters;
    }

    @JIPipeParameter("project-name-filters")
    public void setProjectNameFilters(StringQueryExpression projectNameFilters) {
        this.projectNameFilters = projectNameFilters;
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

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }


    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        if (projectNameAnnotation.isEnabled()) {
            report.resolve("Annotate with name").checkNonEmpty(projectNameAnnotation.getContent(), this);
        }
        if (tagAnnotation.isEnabled()) {
            report.resolve("Annotate with tags").checkNonEmpty(tagAnnotation.getContent(), this);
        }
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

    @JIPipeDocumentation(name = "Add Key-Value pairs as annotations", description = "Adds OMERO project annotations as JIPipe annotations")
    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public boolean isAddKeyValuePairsAsAnnotations() {
        return addKeyValuePairsAsAnnotations;
    }

    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public void setAddKeyValuePairsAsAnnotations(boolean addKeyValuePairsAsAnnotations) {
        this.addKeyValuePairsAsAnnotations = addKeyValuePairsAsAnnotations;
    }

    @JIPipeDocumentation(name = "Tag filters", description = "Filters by tag values. ")
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
