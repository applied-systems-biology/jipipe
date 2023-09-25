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
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "List groups", description = "Returns the ID(s) of groups(s) according to search criteria.")
@JIPipeOutputSlot(value = OMEROGroupReferenceData.class, slotName = "Groups", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindGroupAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private StringQueryExpression groupNameFilters = new StringQueryExpression("");
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalAnnotationNameParameter groupNameAnnotation = new OptionalAnnotationNameParameter("Group", true);

    public OMEROFindGroupAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROFindGroupAlgorithm(OMEROFindGroupAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.groupNameFilters = new StringQueryExpression(other.groupNameFilters);
        this.groupNameAnnotation = new OptionalAnnotationNameParameter(other.groupNameAnnotation);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        LoginCredentials credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials()).toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (Gateway gateway = new Gateway(new OMEROToJIPipeLogger(progressInfo))) {
            ExperimenterData user = gateway.connect(credentials);
            progressInfo.log("Listing groups");
            for (GroupData groupData : user.getGroups()) {
                if (!groupNameFilters.test(groupData.getName())) {
                    continue;
                }
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (groupNameAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(groupNameAnnotation.getContent(), groupData.getName()));
                }
                getFirstOutputSlot().addData(new OMEROGroupReferenceData(groupData.getId()), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Group name filters", description = "Filters for the group name. ")
    @JIPipeParameter("group-name-filters")
    public StringQueryExpression getGroupNameFilters() {
        return groupNameFilters;
    }

    @JIPipeParameter("group-name-filters")
    public void setGroupNameFilters(StringQueryExpression groupNameFilters) {
        this.groupNameFilters = groupNameFilters;
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

    @JIPipeDocumentation(name = "Annotate with group name", description = "Creates an annotation with the project name")
    @JIPipeParameter("group-name-annotation")
    public OptionalAnnotationNameParameter getGroupNameAnnotation() {
        return groupNameAnnotation;
    }

    @JIPipeParameter("group-name-annotation")
    public void setGroupNameAnnotation(OptionalAnnotationNameParameter groupNameAnnotation) {
        this.groupNameAnnotation = groupNameAnnotation;
    }
}
