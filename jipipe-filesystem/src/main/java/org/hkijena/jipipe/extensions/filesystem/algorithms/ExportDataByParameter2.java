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
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@JIPipeDocumentation(name = "Export data", description = "Exports the input data via JIPipe's standard exporter. The output is the directory that contains the generated file(s).")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output path", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataByParameter2 extends JIPipeSimpleIteratingAlgorithm {

    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();
    private boolean forceName = true;

    public ExportDataByParameter2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportDataByParameter2(ExportDataByParameter2 other) {
        super(other);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        this.forceName = other.forceName;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData inputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                inputData.toString(),
                dataBatch.getInputRow(getFirstInputSlot()),
                new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));

        if (forceName) {
            inputData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputPath.getParent()), outputPath.getFileName().toString(), true, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(outputPath.getParent()), progressInfo);
        } else {
            inputData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputPath), "data", false, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(outputPath), progressInfo);
        }

    }

    @JIPipeDocumentation(name = "Force name", description = "If enabled, the last component of the file path is used as name. Files will be written into the parent directory. " +
            "If disabled, the file path is used as directory.")
    @JIPipeParameter("force-name")
    public boolean isForceName() {
        return forceName;
    }

    @JIPipeParameter("force-name")
    public void setForceName(boolean forceName) {
        this.forceName = forceName;
    }

    @JIPipeDocumentation(name = "File path", description = "Expression that generates the output file path")
    @JIPipeParameter("file-path")
    public DataExportExpressionParameter getFilePath() {
        return filePath;
    }

    @JIPipeParameter("file-path")
    public void setFilePath(DataExportExpressionParameter filePath) {
        this.filePath = filePath;
    }
}
