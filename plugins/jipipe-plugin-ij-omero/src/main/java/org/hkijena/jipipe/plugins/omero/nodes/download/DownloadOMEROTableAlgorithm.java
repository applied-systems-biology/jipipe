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

package org.hkijena.jipipe.plugins.omero.nodes.download;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OMEROSettings;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Download table from OMERO", description = "Imports tables attached to an OMERO image as ImageJ table. " +
        "Please note that OMERO tables have a wider range of allowed data types, while ImageJ only supports numeric and string columns. " +
        "Any unsupported table object is converted into a string.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class DownloadOMEROTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalStringParameter fileNameAnnotation = new OptionalStringParameter("Filename", true);


    public DownloadOMEROTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter();
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter();
        registerSubParameter(tagToAnnotationImporter);
    }

    public DownloadOMEROTableAlgorithm(DownloadOMEROTableAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.fileNameAnnotation = new OptionalStringParameter(other.fileNameAnnotation);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter(other.keyValuePairToAnnotationImporter);
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter(other.tagToAnnotationImporter);
        registerSubParameter(tagToAnnotationImporter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        OMEROImageReferenceData imageReferenceData = iterationStep.getInputData(getFirstInputSlot(), OMEROImageReferenceData.class, progressInfo);
        try (OMEROGateway gateway = new OMEROGateway(environment.toLoginCredentials(), progressInfo)) {
            TablesFacility tablesFacility = gateway.getGateway().getFacility(TablesFacility.class);
            ImageData imageData = gateway.getImage(imageReferenceData.getImageId(), -1);
            SecurityContext context = new SecurityContext(imageData.getGroupId());
            for (FileAnnotationData fileAnnotationData : tablesFacility.getAvailableTables(context, imageData)) {
                String fileName = fileAnnotationData.getFileName();
                long fileID = fileAnnotationData.getFileID();
                TableData table = tablesFacility.getTable(context, fileID);
                ResultsTableData resultsTableData = OMEROUtils.tableFromOMERO(table);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (fileNameAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(fileNameAnnotation.getContent(), fileName));
                }
                try {
                    tagToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, fileAnnotationData);
                    keyValuePairToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, fileAnnotationData);
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }
                iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        } catch (Exception e) {
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

    @SetJIPipeDocumentation(name = "Annotate with file name", description = "Attach the file name of the table file to the output")
    @JIPipeParameter("file-name-annotation")
    public OptionalStringParameter getFileNameAnnotation() {
        return fileNameAnnotation;
    }

    @JIPipeParameter("file-name-annotation")
    public void setFileNameAnnotation(OptionalStringParameter fileNameAnnotation) {
        this.fileNameAnnotation = fileNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Import key-value pairs", description = "OMERO key-value pairs can be imported into annotations")
    @JIPipeParameter("key-value-pair-to-annotation-importer")
    public OMEROKeyValuePairToAnnotationImporter getKeyValuePairToAnnotationImporter() {
        return keyValuePairToAnnotationImporter;
    }

    @SetJIPipeDocumentation(name = "Import tags", description = "OMERO tags can be imported into annotations")
    @JIPipeParameter("tag-to-annotation-importer")
    public OMEROTagToAnnotationImporter getTagToAnnotationImporter() {
        return tagToAnnotationImporter;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(reportContext, this), environment);
    }
}
