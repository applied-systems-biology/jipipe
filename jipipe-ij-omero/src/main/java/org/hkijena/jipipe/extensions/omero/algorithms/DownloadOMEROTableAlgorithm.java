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

package org.hkijena.jipipe.extensions.omero.algorithms;

import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROToJIPipeLogger;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Download from OMERO", description = "Imports tables attached to an OMERO image as ImageJ table. " +
        "Please note that OMERO tables have a wider range of allowed data types, while ImageJ only supports numeric and string columns. " +
        "Any unsupported table object is converted into a string.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class DownloadOMEROTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalStringParameter tagAnnotation = new OptionalStringParameter("Tags", true);
    private OptionalStringParameter fileNameAnnotation = new OptionalStringParameter("Filename", true);


    public DownloadOMEROTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public DownloadOMEROTableAlgorithm(DownloadOMEROTableAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagAnnotation = new OptionalStringParameter(other.tagAnnotation);
        this.fileNameAnnotation = new OptionalStringParameter(other.fileNameAnnotation);
        registerSubParameter(credentials);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEROImageReferenceData imageReferenceData = dataBatch.getInputData(getFirstInputSlot(), OMEROImageReferenceData.class, progressInfo);
        try (Gateway gateway = new Gateway(new OMEROToJIPipeLogger(progressInfo))) {
            ExperimenterData user = gateway.connect(credentials.getCredentials());
            SecurityContext context = new SecurityContext(user.getGroupId());
            BrowseFacility browseFacility = gateway.getFacility(BrowseFacility.class);
            TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
            ImageData imageData = browseFacility.getImage(context, imageReferenceData.getImageId());
            for (FileAnnotationData fileAnnotationData : tablesFacility.getAvailableTables(context, imageData)) {
                String fileName = fileAnnotationData.getFileName();
                long fileID = fileAnnotationData.getFileID();
                TableData table = tablesFacility.getTable(context, fileID);
                ResultsTableData resultsTableData = OMEROUtils.tableFromOMERO(table);
                List<JIPipeAnnotation> annotations = new ArrayList<>();
                if (fileNameAnnotation.isEnabled())
                    annotations.add(new JIPipeAnnotation(fileNameAnnotation.getContent(), fileName));
                dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        if (tagAnnotation.isEnabled())
            report.resolve("Tag annotation").checkNonEmpty(getTagAnnotation().getContent(), null);
        if (fileNameAnnotation.isEnabled())
            report.resolve("Annotate with file name").checkNonEmpty(getFileNameAnnotation().getContent(), null);
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

    @JIPipeDocumentation(name = "Add Key-Value pairs as annotations", description = "Adds OMERO project annotations as JIPipe annotations")
    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public boolean isAddKeyValuePairsAsAnnotations() {
        return addKeyValuePairsAsAnnotations;
    }

    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public void setAddKeyValuePairsAsAnnotations(boolean addKeyValuePairsAsAnnotations) {
        this.addKeyValuePairsAsAnnotations = addKeyValuePairsAsAnnotations;
    }

    @JIPipeDocumentation(name = "Annotate with file name", description = "Attach the file name of the table file to the output")
    @JIPipeParameter("file-name-annotation")
    public OptionalStringParameter getFileNameAnnotation() {
        return fileNameAnnotation;
    }

    @JIPipeParameter("file-name-annotation")
    public void setFileNameAnnotation(OptionalStringParameter fileNameAnnotation) {
        this.fileNameAnnotation = fileNameAnnotation;
    }
}
