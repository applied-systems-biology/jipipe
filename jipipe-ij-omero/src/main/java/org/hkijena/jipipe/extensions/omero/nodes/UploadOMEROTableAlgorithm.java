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

package org.hkijena.jipipe.extensions.omero.nodes;

import omero.gateway.SecurityContext;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Upload to OMERO", description = "Uploads tables to OMERO. The table is attached to an image.")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Tables")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Table", autoCreate = true)
@JIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Image", autoCreate = true)
public class UploadOMEROTableAlgorithm extends JIPipeMergingAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();

    public UploadOMEROTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UploadOMEROTableAlgorithm(UploadOMEROTableAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<OMEROImageReferenceData> images = dataBatch.getInputData("Image", OMEROImageReferenceData.class, progressInfo);
        List<ResultsTableData> tables = dataBatch.getInputData("Table", ResultsTableData.class, progressInfo);
        JIPipeDataSlot dummy = dataBatch.toDummySlot(new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Input), null, getInputSlot("Table"), progressInfo);

        OMEROCredentialsEnvironment credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        try (OMEROGateway gateway = new OMEROGateway(credentials.toLoginCredentials(), progressInfo)) {

            TablesFacility tablesFacility = gateway.getGateway().getFacility(TablesFacility.class);

            // Generate file names
            Set<String> existingFileNames = new HashSet<>();
            List<String> fileNames = new ArrayList<>();
            for (int i = 0; i < dummy.getRowCount(); i++) {
                String fileName = exporter.generateName(dummy, i, existingFileNames);
                fileNames.add(fileName + ".csv");
                progressInfo.log("Will add table: " + fileName + ".csv");
            }

            // Attach tables for each image
            for (OMEROImageReferenceData image : images) {
                progressInfo.log("Attaching tables to Image ID=" + image.getImageId());
                ImageData imageData = gateway.getImage(image.getImageId(), -1);
                SecurityContext context = new SecurityContext(imageData.getGroupId());
                for (int i = 0; i < tables.size(); i++) {
                    progressInfo.resolve("Attaching tables to Image ID=" + image.getImageId()).resolveAndLog((i + 1) + "/" + tables.size());
                    ResultsTableData resultsTableData = tables.get(i);
                    TableData tableData = OMEROUtils.tableToOMERO(resultsTableData);
                    String fileName = fileNames.get(i);
                    tablesFacility.addTable(context, imageData, fileName, tableData);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @JIPipeDocumentation(name = "File name", description = "Determines the file name of the exported tables.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }
}
