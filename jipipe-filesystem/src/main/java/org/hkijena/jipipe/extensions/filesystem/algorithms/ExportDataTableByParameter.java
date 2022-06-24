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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeDocumentation(name = "Export data table", description = "Exports all incoming data as data table directory.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Path", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataTableByParameter extends JIPipeMergingAlgorithm {

    private Path outputDirectory = Paths.get("exported-data");
    private boolean relativeToProjectDir = false;

    public ExportDataTableByParameter(JIPipeNodeInfo info) {
        super(info);
        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
    }

    public ExportDataTableByParameter(ExportDataTableByParameter other) {
        super(other);
        this.outputDirectory = other.outputDirectory;
        this.relativeToProjectDir = other.relativeToProjectDir;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path outputPath = this.outputDirectory;
        if (!outputPath.isAbsolute()) {
            if (relativeToProjectDir && getProjectDirectory() != null) {
                outputPath = getProjectDirectory().resolve(StringUtils.nullToEmpty(outputDirectory));
            } else {
                outputPath = getFirstOutputSlot().getSlotStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
            }
        }
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        JIPipeDataSlot batchSlot = dataBatch.toDummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output", ""), this, getInputSlot("Data"));
        batchSlot.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputPath), progressInfo);
        dataBatch.addOutputData("Path", new PathData(outputPath), progressInfo);
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

    @JIPipeDocumentation(name = "Output relative to project directory", description = "If enabled, outputs will be preferably generated relative to the project directory. " +
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
}

