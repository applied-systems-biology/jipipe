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

package org.hkijena.jipipe.plugins.omero.nodes.navigate;

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.PlateData;
import omero.gateway.model.ScreenData;
import omero.gateway.model.WellData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROPlateReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROScreenReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROWellReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "List OMERO wells", description = "Returns the ID(s) of wells according to search criteria.")
@AddJIPipeInputSlot(value = OMEROPlateReferenceData.class, name = "Plates", create = true)
@AddJIPipeOutputSlot(value = OMEROWellReferenceData.class, name = "Wells", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListWellsAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListWellsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListWellsAlgorithm(OMEROListWellsAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO plate", row, getFirstInputSlot().getRowCount());
                long plateId = getFirstInputSlot().getData(row, OMEROPlateReferenceData.class, rowProgress).getPlateId();
                PlateData plateData1 = gateway.getPlate(plateId, -1);
                SecurityContext context = new SecurityContext(plateData1.getGroupId());
                progressInfo.log("Listing wells in plate ID=" + plateData1.getId());

                for (WellData wellData : gateway.getBrowseFacility().getWells(context, plateId)) {
                    if(wellData != null) {
                        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
                        variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));

                        variables.put("well.type", wellData.getWellType());
                        variables.put("well.status", wellData.getStatus());
                        variables.put("well.row", wellData.getRow());
                        variables.put("well.column", wellData.getColumn());
                        variables.put("well.color.r", wellData.getRed());
                        variables.put("well.color.g", wellData.getGreen());
                        variables.put("well.color.b", wellData.getBlue());
                        variables.put("well.color.a", wellData.getBlue());

                        variables.put("id", wellData.getId());
                        variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, wellData));
                        variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, wellData)));
                        if (filters.test(variables)) {
                            getFirstOutputSlot().addData(new OMEROWellReferenceData(wellData, environment), getFirstInputSlot().getDataContext(row).branch(this), rowProgress);
                        }
                    }
                }
            }
        } catch (DSOutOfServiceException | DSAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }

    @SetJIPipeDocumentation(name = "Keep plate if", description = "Allows to filter the returned images")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO plate")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @AddJIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well id", description = "ID of the well", key = "id")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well type", description = "Type of the well", key = "well.type")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well status", description = "Status of the well", key = "well.status")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well row", description = "Row location of the well", key = "well.row")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well column", description = "Column location of the well", key = "well.column")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well color red", description = "Red of the well", key = "well.color.r")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well color green", description = "Green of the well", key = "well.color.g")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well color blue", description = "Blue of the well", key = "well.color.b")
    @AddJIPipeExpressionParameterVariable(name = "OMERO well color alpha", description = "Alpha of the well", key = "well.color.a")
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
            reportConfiguredOMEROEnvironmentValidity(reportContext, report);
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredOMEROCredentialsEnvironment());
    }
}
