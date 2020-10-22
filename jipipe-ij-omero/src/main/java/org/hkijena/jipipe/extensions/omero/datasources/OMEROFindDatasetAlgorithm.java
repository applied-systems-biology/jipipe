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
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "List datasets", description = "Returns the ID(s) of dataset(s) according to search criteria. Requires project IDs as input.")
@JIPipeInputSlot(value = OMEROProjectReferenceData.class, slotName = "Projects", autoCreate = true)
@JIPipeOutputSlot(value = OMERODatasetReferenceData.class, slotName = "Datasets", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindDatasetAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringPredicate.List datasetNameFilters = new StringPredicate.List();
    private OptionalStringParameter projectNameAnnotation = new OptionalStringParameter("Project", true);
    private OptionalStringParameter datasetNameAnnotation = new OptionalStringParameter("Dataset", true);

    public OMEROFindDatasetAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        Set<Long> projectIds = new HashSet<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            projectIds.add(getFirstInputSlot().getData(row, OMEROProjectReferenceData.class).getProjectId());
        }

        LoginCredentials credentials = this.credentials.getCredentials();
        algorithmProgress.accept(subProgress.resolve("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost()));
        try(Gateway gateway = new Gateway(new OMEROToJIPipeLogger(subProgress, algorithmProgress))) {
            ExperimenterData user = gateway.connect(credentials);
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            for (Long projectId : projectIds) {
                algorithmProgress.accept(subProgress.resolve("Listing datasets in project ID=" + projectId));
                ProjectData projectData = browseFacility.getProjects(context, Collections.singletonList(projectId)).iterator().next();
                for (DatasetData dataset : projectData.getDatasets()) {
                    if(!datasetNameFilters.isEmpty() && !datasetNameFilters.test(dataset.getName())) {
                        continue;
                    }
                    List<JIPipeAnnotation> annotations = new ArrayList<>();
                    if(projectNameAnnotation.isEnabled())
                        annotations.add(new JIPipeAnnotation(projectNameAnnotation.getContent(), projectData.getName()));
                    if(datasetNameAnnotation.isEnabled())
                        annotations.add(new JIPipeAnnotation(datasetNameAnnotation.getContent(), dataset.getName()));
                    getFirstOutputSlot().addData(new OMERODatasetReferenceData(dataset.getId()), annotations);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OMEROFindDatasetAlgorithm(OMEROFindDatasetAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.datasetNameFilters = new StringPredicate.List(other.datasetNameFilters);
        this.datasetNameAnnotation = new OptionalStringParameter(other.datasetNameAnnotation);
        this.projectNameAnnotation = new OptionalStringParameter(other.projectNameAnnotation);
        registerSubParameter(credentials);
    }

    @JIPipeDocumentation(name = "Project name filters", description = "Filters for the project name. A project is returned if one of the filters apply. If the list is empty, all projects are returned.")
    @JIPipeParameter("project-name-filters")
    public StringPredicate.List getDatasetNameFilters() {
        return datasetNameFilters;
    }

    @JIPipeParameter("project-name-filters")
    public void setDatasetNameFilters(StringPredicate.List datasetNameFilters) {
        this.datasetNameFilters = datasetNameFilters;
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
        if(datasetNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with dataset name").checkNonEmpty(datasetNameAnnotation.getContent(), this);
        }
        if(projectNameAnnotation.isEnabled()) {
            report.forCategory("Annotate with project name").checkNonEmpty(projectNameAnnotation.getContent(), this);
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
}
