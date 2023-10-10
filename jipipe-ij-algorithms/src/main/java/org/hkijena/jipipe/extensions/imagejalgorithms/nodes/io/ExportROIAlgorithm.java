package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.io;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.DataExporterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Export ROI", description = "Deprecated. Please use the new node. Exports a ROI list into one or multiple ROI files")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "ROI")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
@Deprecated
@JIPipeHidden
public class ExportROIAlgorithm extends JIPipeIteratingAlgorithm {

    private final Set<String> existingMetadata = new HashSet<>();
    private JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");
    private boolean exportAsROIFile = false;
    private boolean relativeToProjectDir = false;

    public ExportROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.exporter = new JIPipeDataByMetadataExporter(DataExporterSettings.getInstance());
        registerSubParameter(exporter);
    }

    public ExportROIAlgorithm(ExportROIAlgorithm other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.outputDirectory = other.outputDirectory;
        this.exportAsROIFile = other.exportAsROIFile;
        this.relativeToProjectDir = other.relativeToProjectDir;
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        existingMetadata.clear();
        super.runParameterSet(progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
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

        // Generate subfolder
        Path generatedPath = exporter.generatePath(getFirstInputSlot(), dataBatch.getInputSlotRows().get(getFirstInputSlot()), existingMetadata);

        // If absolute -> use the path, otherwise use output directory
        if (generatedPath.isAbsolute()) {
            outputPath = generatedPath;
        } else {
            outputPath = outputPath.resolve(generatedPath);
        }
        PathUtils.ensureParentDirectoriesExist(outputPath);

        ROIListData rois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        if (exportAsROIFile && rois.size() > 1) {
            Set<String> existing = new HashSet<>();
            final String baseName = outputPath.getFileName().toString();
            for (Roi roi : rois) {
                String roiName;
                if (!StringUtils.isNullOrEmpty(roi.getName())) {
                    roiName = StringUtils.makeUniqueString(baseName + "_" + roi.getName(), "_", existing);
                } else {
                    roiName = StringUtils.makeUniqueString(baseName, "_", existing);
                }
                Path path = PathUtils.ensureExtension(outputPath.getParent().resolve(roiName), ".roi");
                ROIListData.saveSingleRoi(roi, path);
                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(path), progressInfo);
            }
        } else {
            outputPath = rois.saveToRoiOrZip(outputPath);
            dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
        }
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

    @JIPipeDocumentation(name = "Force exporting *.roi files", description = "If true, the exporter will always export *.roi files and if necessary split the ROI list.")
    @JIPipeParameter("export-as-roi-file")
    public boolean isExportAsROIFile() {
        return exportAsROIFile;
    }

    @JIPipeParameter("export-as-roi-file")
    public void setExportAsROIFile(boolean exportAsROIFile) {
        this.exportAsROIFile = exportAsROIFile;
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
