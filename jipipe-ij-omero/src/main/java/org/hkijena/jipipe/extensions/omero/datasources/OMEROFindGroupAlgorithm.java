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
import org.hkijena.jipipe.api.JIPipeIssueReport;
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
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "List groups", description = "Returns the ID(s) of groups(s) according to search criteria.")
@JIPipeOutputSlot(value = OMEROGroupReferenceData.class, slotName = "Groups", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROFindGroupAlgorithm extends JIPipeParameterSlotAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private StringQueryExpression groupNameFilters = new StringQueryExpression("");
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalAnnotationNameParameter groupNameAnnotation = new OptionalAnnotationNameParameter("Group", true);

    public OMEROFindGroupAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public OMEROFindGroupAlgorithm(OMEROFindGroupAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.groupNameFilters = new StringQueryExpression(other.groupNameFilters);
        this.groupNameAnnotation = new OptionalAnnotationNameParameter(other.groupNameAnnotation);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        LoginCredentials credentials = this.credentials.getCredentials();
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

    @JIPipeDocumentation(name = "Group name filters", description = "Filters for the group name. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("group-name-filters")
    public StringQueryExpression getGroupNameFilters() {
        return groupNameFilters;
    }

    @JIPipeParameter("group-name-filters")
    public void setGroupNameFilters(StringQueryExpression groupNameFilters) {
        this.groupNameFilters = groupNameFilters;
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
        if (groupNameAnnotation.isEnabled()) {
            report.resolve("Annotate with name").checkNonEmpty(groupNameAnnotation.getContent(), this);
        }
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
