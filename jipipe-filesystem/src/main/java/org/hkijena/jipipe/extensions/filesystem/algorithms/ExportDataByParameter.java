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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@JIPipeDocumentation(name = "Export data", description = "Collects all incoming data into one or multiple folders that contain the raw output files. " +
        "The output files are named according to the metadata columns and can be easily processed by humans or third-party scripts. " +
        "The output of this algorithm is the selected output directory. " +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output path", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataByParameter extends JIPipeAlgorithm {

    private boolean splitByInputSlots = true;
    private Path outputDirectory = Paths.get("exported-data");
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();

    public ExportDataByParameter(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporter);
    }

    public ExportDataByParameter(ExportDataByParameter other) {
        super(other);
        this.splitByInputSlots = other.splitByInputSlots;
        this.outputDirectory = other.outputDirectory;
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        registerSubParameter(exporter);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputPath = getFirstOutputSlot().getStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
        } else {
            outputPath = outputDirectory;
        }
        if (isPassThrough()) {
            progressInfo.log("Data passed through to output");
            getFirstOutputSlot().addData(new FolderData(outputPath), progressInfo);
            return;
        }

        if (splitByInputSlots) {
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                if (progressInfo.isCancelled())
                    return;
                exporter.writeToFolder(Collections.singletonList(inputSlot), outputPath.resolve(StringUtils.makeFilesystemCompatible(inputSlot.getName())), progressInfo.resolve("Slot '" + inputSlot.getName() + "'"));
                getFirstOutputSlot().addData(new FolderData(outputPath.resolve(inputSlot.getName())), progressInfo);
            }
        } else {
            exporter.writeToFolder(getInputSlots(), outputPath, progressInfo);
            getFirstOutputSlot().addData(new FolderData(outputPath), progressInfo);
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @JIPipeDocumentation(name = "Split by input slots", description = "If enabled, a folder for each input slot is created. Otherwise all output is stored within one folder.")
    @JIPipeParameter("split-by-input-slots")
    public boolean isSplitByInputSlots() {
        return splitByInputSlots;
    }

    @JIPipeParameter("split-by-input-slots")
    public void setSplitByInputSlots(boolean splitByInputSlots) {
        this.splitByInputSlots = splitByInputSlots;
    }

    @JIPipeDocumentation(name = "Output directory", description = "Can be a relative or absolute directory. All collected files will be put into this directory. " +
            "If relative, it is relative to the output slot's output directory that is generated based on the current run's output path.")
    @JIPipeParameter("output-directory")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly)
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    @JIPipeParameter("output-directory")
    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @JIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }
}
