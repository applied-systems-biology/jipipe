package org.hkijena.jipipe.extensions.scene3d.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DToColladaExporter;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@JIPipeDocumentation(name = "Export 3D scene", description = "Exports a 3D scene to Collada 1.4.1 (DAE)")
@JIPipeInputSlot(value = Scene3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "3D scenes")
public class ExportScene3DToColladaAlgorithm2 extends JIPipeIteratingAlgorithm {

    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();
    private boolean indexMeshes = true;

    public ExportScene3DToColladaAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportScene3DToColladaAlgorithm2(ExportScene3DToColladaAlgorithm2 other) {
        super(other);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        this.indexMeshes = other.indexMeshes;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Scene3DData scene3DData = dataBatch.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo);

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                scene3DData.toString(),
                dataBatch.getInputRow(getFirstInputSlot()),
                new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));

        Path outputFile = PathUtils.ensureExtension(outputPath, ".dae");
        PathUtils.ensureParentDirectoriesExist(outputPath);

        Scene3DToColladaExporter scene3DToColladaExporter = new Scene3DToColladaExporter(scene3DData, outputFile);
        scene3DToColladaExporter.setIndexMeshes(indexMeshes);
        scene3DToColladaExporter.setProgressInfo(progressInfo.resolve("Export DAE"));
        scene3DToColladaExporter.run();

        dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
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

    @JIPipeDocumentation(name = "Index/simplify meshes", description = "If enabled, meshes are automatically indexed (simplified), which reduces the size of the output file, but needs additional processing time")
    @JIPipeParameter("index-meshes")
    public boolean isIndexMeshes() {
        return indexMeshes;
    }

    @JIPipeParameter("index-meshes")
    public void setIndexMeshes(boolean indexMeshes) {
        this.indexMeshes = indexMeshes;
    }
}
