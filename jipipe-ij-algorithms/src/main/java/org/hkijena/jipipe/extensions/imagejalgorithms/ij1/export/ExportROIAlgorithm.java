package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.export;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.DataExporterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Export ROI", description = "Exports a ROI list into one or multiple ROI files")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "ROI")
public class ExportROIAlgorithm extends JIPipeIteratingAlgorithm {

    private final Set<String> existingMetadata = new HashSet<>();
    private JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");
    private boolean exportAsROIFile = false;

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
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        existingMetadata.clear();
        super.runParameterSet(progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputPath = getFirstOutputSlot().getStoragePath().resolve(outputDirectory);
        } else {
            outputPath = outputDirectory;
        }

        // Generate subfolder
        Path subFolder = exporter.generateSubFolder(getFirstInputSlot(), dataBatch.getInputSlotRows().get(getFirstInputSlot()));
        if (subFolder != null) {
            outputPath = outputPath.resolve(subFolder);
        }

        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ROIListData rois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        String baseName = exporter.generateMetadataString(getFirstInputSlot(), dataBatch.getInputSlotRows().get(getFirstInputSlot()), existingMetadata);

        if (exportAsROIFile && rois.size() > 1) {
            Set<String> existing = new HashSet<>();
            for (Roi roi : rois) {
                ROIListData singleton = new ROIListData();
                singleton.add(roi);
                String roiName;
                if (!StringUtils.isNullOrEmpty(roi.getName())) {
                    roiName = StringUtils.makeUniqueString(baseName + "_" + roi.getName(), "_", existing);
                } else {
                    roiName = StringUtils.makeUniqueString(baseName, "_", existing);
                }
                singleton.saveTo(outputPath, roiName, true, progressInfo);
                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath.resolve(roiName + ".roi")), progressInfo);
            }
        } else {
            rois.saveTo(outputPath, baseName, true, progressInfo);
            if (rois.size() == 1) {
                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath.resolve(baseName + ".roi")), progressInfo);
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputPath.resolve(baseName + ".zip")), progressInfo);
            }
        }
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
