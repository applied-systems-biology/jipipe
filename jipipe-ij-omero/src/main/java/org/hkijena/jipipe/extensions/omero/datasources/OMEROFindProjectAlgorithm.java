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
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.LongList;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Find project", description = "Returns the ID(s) of project(s) according to search criteria.")
@JIPipeOutputSlot(value = OMEROProjectReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindProjectAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringPredicate.List projectNameFilters = new StringPredicate.List();

    public OMEROFindProjectAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public OMEROFindProjectAlgorithm(OMEROFindProjectAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.projectNameFilters = new StringPredicate.List(other.projectNameFilters);
        registerSubParameter(credentials);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        try(FileWriter writer = new FileWriter("properties-test.prop")) {
            System.getProperties().store(writer, "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        LoginCredentials credentials = this.credentials.getCredentials();
        try(Gateway gateway = new Gateway(new OMEROToJIPipeLogger(subProgress, algorithmProgress))) {
            ExperimenterData user = gateway.connect(credentials);
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            for (ProjectData project : findProjects(browseFacility, context)) {
                dataBatch.addOutputData(getFirstOutputSlot(), new OMEROProjectReferenceData(project.getId()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }

    /**
     * Finds projects according to the current settings
     * @param browse the browser
     * @param context the security context
     * @return list of projects
     */
    public List<ProjectData> findProjects(BrowseFacility browse, SecurityContext context) {
        List<ProjectData> result = new ArrayList<>();
        try {
            for (ProjectData project : browse.getProjects(context)) {
                if(projectNameFilters.isEmpty() || projectNameFilters.test(project.getName())) {
                    result.add(project);
                }
            }
        } catch (DSOutOfServiceException | DSAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
