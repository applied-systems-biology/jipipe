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

package org.hkijena.jipipe.plugins.scene3d.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.plugins.scene3d.utils.Scene3DToColladaExporter;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@SetJIPipeDocumentation(name = "Export 3D scene", description = "Exports a 3D scene to Collada 1.4.1 (DAE)")
@AddJIPipeInputSlot(value = Scene3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Exported file", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "3D scenes")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Scene3DData scene3DData = iterationStep.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo);

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
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));

        Path outputFile = PathUtils.ensureExtension(outputPath, ".dae");
        PathUtils.ensureParentDirectoriesExist(outputPath);

        Scene3DToColladaExporter scene3DToColladaExporter = new Scene3DToColladaExporter(scene3DData, outputFile);
        scene3DToColladaExporter.setIndexMeshes(indexMeshes);
        scene3DToColladaExporter.setProgressInfo(progressInfo.resolve("Export DAE"));
        scene3DToColladaExporter.run();

        iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
    }

    @SetJIPipeDocumentation(name = "File path", description = "Expression that generates the output file path")
    @JIPipeParameter("file-path")
    public DataExportExpressionParameter getFilePath() {
        return filePath;
    }

    @JIPipeParameter("file-path")
    public void setFilePath(DataExportExpressionParameter filePath) {
        this.filePath = filePath;
    }

    @SetJIPipeDocumentation(name = "Index/simplify meshes", description = "If enabled, meshes are automatically indexed (simplified), which reduces the size of the output file, but needs additional processing time")
    @JIPipeParameter("index-meshes")
    public boolean isIndexMeshes() {
        return indexMeshes;
    }

    @JIPipeParameter("index-meshes")
    public void setIndexMeshes(boolean indexMeshes) {
        this.indexMeshes = indexMeshes;
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Configure exported path", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectFilePathDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(),
                canvasUI.getWorkbench(),
                "Select output file",
                PathType.FilesOnly,
                new FileNameExtensionFilter("Collada (*.dae)", "dae"));
        if (result != null) {
            setFilePath(result);
            emitParameterChangedEvent("file-path");
        }
    }
}
