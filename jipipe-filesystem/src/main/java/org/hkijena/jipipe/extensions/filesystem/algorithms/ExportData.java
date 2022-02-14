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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Export data (path input)", description = "Exports all incoming data to the path specified by the path input data. " +
        "The files will be named according to the last path component. Depending on the data type one or multiple files " +
        "that contain the last path component in their name might be created. " +
        "Duplicate files might be silently overwritten, meaning that the paths should be unique." +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@JIPipeInputSlot(JIPipeData.class)
@JIPipeInputSlot(PathData.class)
@JIPipeOutputSlot(PathData.class)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportData extends JIPipeIteratingAlgorithm {

    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();

    public ExportData(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Data", JIPipeData.class)
                .addInputSlot("Path", PathData.class)
                .addOutputSlot("Path", PathData.class, null)
                .seal()
                .build());
        registerSubParameter(exporter);
    }

    public ExportData(ExportData other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path outputDirectory = dataBatch.getInputData("Path", PathData.class, progressInfo).toPath();
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputDirectory = getFirstOutputSlot().getStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
        }
        if (!Files.exists(outputDirectory)) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        JIPipeDataSlot batchSlot = dataBatch.toDummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output"), this, getInputSlot("Data"));
        exporter.writeToFolder(batchSlot, outputDirectory, progressInfo);
        dataBatch.addOutputData("Path", new PathData(outputDirectory), progressInfo);
    }

    @JIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }
}

