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
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "List OMERO images", description = "Returns the ID(s) of images(s) according to search criteria. Requires project IDs as input.")
@AddJIPipeInputSlot(value = OMERODatasetReferenceData.class, slotName = "Datasets", create = true)
@AddJIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListImagesAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListImagesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListImagesAlgorithm(OMEROListImagesAlgorithm other) {
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
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO dataset", row, getFirstInputSlot().getRowCount());
                long datasetId = getFirstInputSlot().getData(row, OMERODatasetReferenceData.class, rowProgress).getDatasetId();
                DatasetData datasetData = gateway.getDataset(datasetId, -1);
                SecurityContext context = new SecurityContext(datasetData.getGroupId());
                progressInfo.log("Listing images in dataset ID=" + datasetData.getId());

                for (Object obj : datasetData.getImages()) {
                    if (obj instanceof ImageData) {
                        ImageData imageData = (ImageData) obj;
                        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                        variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
                        variables.put("name", imageData.getName());
                        variables.put("id", imageData.getId());
                        variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, imageData));
                        variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, imageData)));
                        if (filters.test(variables)) {
                            getFirstOutputSlot().addData(new OMEROImageReferenceData(imageData, environment), getFirstInputSlot().getDataContext(row).branch(this), rowProgress);
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

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter the returned images")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO data set")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @JIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @JIPipeExpressionParameterVariable(name = "OMERO image name", description = "Name of the image", key = "name")
    @JIPipeExpressionParameterVariable(name = "OMERO image id", description = "ID of the image", key = "id")
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
