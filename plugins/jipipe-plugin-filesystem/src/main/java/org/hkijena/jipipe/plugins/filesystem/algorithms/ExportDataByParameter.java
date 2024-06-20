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
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@SetJIPipeDocumentation(name = "Export data (old version)", description = "Collects all incoming data into one or multiple folders that contain the raw output files. " +
        "The output files are named according to the metadata columns and can be easily processed by humans or third-party scripts. " +
        "The output of this algorithm is the selected output directory. " +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeOutputSlot(value = FolderData.class, name = "Output path", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataByParameter extends JIPipeAlgorithm {

    private Path outputDirectory = Paths.get("exported-data");
    private boolean relativeToProjectDir = false;
    private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private boolean splitBySlotName;

    public ExportDataByParameter(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporter);
    }

    public ExportDataByParameter(ExportDataByParameter other) {
        super(other);
        this.outputDirectory = other.outputDirectory;
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.relativeToProjectDir = other.relativeToProjectDir;
        this.splitBySlotName = other.splitBySlotName;
        registerSubParameter(exporter);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            if (relativeToProjectDir && getProjectDirectory() != null) {
                outputPath = getProjectDirectory().resolve(StringUtils.nullToEmpty(outputDirectory));
            } else {
                outputPath = getFirstOutputSlot().getSlotStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
            }
        } else {
            outputPath = outputDirectory;
        }
        if (isPassThrough()) {
            progressInfo.log("Data passed through to output");
            getFirstOutputSlot().addData(new FolderData(outputPath), JIPipeDataContext.create(this), progressInfo);
            return;
        }

        exporter.writeToFolder(getInputSlots(), outputPath, progressInfo);
        getFirstOutputSlot().addData(new FolderData(outputPath), JIPipeDataContext.create(this), progressInfo);
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Output relative to project directory", description = "If enabled, outputs will be preferably generated relative to the project directory. " +
            "Otherwise, JIPipe will store the results in an automatically generated directory. " +
            "Has no effect if an absolute path is provided.")
    @JIPipeParameter("relative-to-project-dir")
    public boolean isRelativeToProjectDir() {
        return relativeToProjectDir;
    }

    @JIPipeParameter("relative-to-project-dir")
    public void setRelativeToProjectDir(boolean relativeToProjectDir) {
        this.relativeToProjectDir = relativeToProjectDir;
    }

    @SetJIPipeDocumentation(name = "Output directory", description = "Can be a relative or absolute directory. All collected files will be put into this directory. " +
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
