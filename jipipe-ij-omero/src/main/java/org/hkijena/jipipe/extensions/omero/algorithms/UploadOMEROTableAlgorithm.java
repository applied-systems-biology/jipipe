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
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
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

    private OMEROCredentials credentials = new OMEROCredentials();
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();

    public UploadOMEROTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public UploadOMEROTableAlgorithm(UploadOMEROTableAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        registerSubParameter(credentials);
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<OMEROImageReferenceData> images = dataBatch.getInputData("Image", OMEROImageReferenceData.class, progressInfo);
        List<ResultsTableData> tables = dataBatch.getInputData("Table", ResultsTableData.class, progressInfo);
        JIPipeDataSlot dummy = dataBatch.toDummySlot(new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Input), null, getInputSlot("Table"));

        try (OMEROGateway gateway = new OMEROGateway(credentials.getCredentials(), progressInfo)) {

            TablesFacility tablesFacility = gateway.getGateway().getFacility(TablesFacility.class);

            // Generate file names
            Set<String> existingFileNames = new HashSet<>();
            List<String> fileNames = new ArrayList<>();
            for (int i = 0; i < dummy.getRowCount(); i++) {
                String fileName = exporter.generateMetadataString(dummy, i, existingFileNames);
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

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }

    @JIPipeDocumentation(name = "File name", description = "Determines the file name of the exported tables.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }
}
