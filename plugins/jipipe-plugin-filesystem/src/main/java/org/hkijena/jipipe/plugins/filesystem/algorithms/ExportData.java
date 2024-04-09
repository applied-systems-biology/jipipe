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

package org.hkijena.jipipe.plugins.filesystem.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Export data (path input)", description = "Exports all incoming data to the path specified by the path input data. " +
        "The files will be named according to the last path component. Depending on the data type one or multiple files " +
        "that contain the last path component in their name might be created. " +
        "Duplicate files might be silently overwritten, meaning that the paths should be unique." +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data", description = "The data to be exported", create = true)
@AddJIPipeInputSlot(value = PathData.class, slotName = "Path", description = "The directory where the data will be stored", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Path", description = "The directory where the data was stored", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportData extends JIPipeIteratingAlgorithm {

    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private boolean splitBySlotName;

    public ExportData(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporter);
    }

    public ExportData(ExportData other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.splitBySlotName = other.splitBySlotName;
        registerSubParameter(exporter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path outputDirectory = iterationStep.getInputData("Path", PathData.class, progressInfo).toPath();
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputDirectory = getFirstOutputSlot().getSlotStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
        }
        if (!Files.exists(outputDirectory)) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        JIPipeDataSlot batchSlot = iterationStep.toDummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output", ""), this, getInputSlot("Data"), progressInfo);
        exporter.writeToFolder(batchSlot, outputDirectory, progressInfo);
        iterationStep.addOutputData("Path", new PathData(outputDirectory), progressInfo);
    }

    @SetJIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }

    @SetJIPipeDocumentation(name = "Split by output name", description = "If enabled, the exporter will attempt to split data by their output name. Has no effect if the exported data table is not an output of a node.")
    @JIPipeParameter("split-by-slot-name")
    public boolean isSplitBySlotName() {
        return splitBySlotName;
    }

    @JIPipeParameter("split-by-slot-name")
    public void setSplitBySlotName(boolean splitBySlotName) {
        this.splitBySlotName = splitBySlotName;
    }
}

