package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.settings.DataExporterSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Export image (directory slot)", description = "Exports incoming images into a non-JIPipe format (PNG, JPEG, BMP, AVI, TIFF). " +
        "Please note support for input images depending on the file format: " +
        "<ul>" +
        "<li>PNG, JPEG, BMP: 2D images only</li>" +
        "<li>AVI: 2D or 3D images only</li>" +
        "<li>TIFF: All images supported</li>" +
        "</ul>")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = FolderData.class, slotName = "Output directory", autoCreate = true, description = "Relative to the working directory of the current slot. Convert to absolute path to allow writing outside the output directory.")
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportImage2Algorithm extends JIPipeIteratingAlgorithm {

    private final Set<String> existingMetadata = new HashSet<>();
    private JIPipeDataByMetadataExporter exporter;
    private ExportImageAlgorithm.FileFormat fileFormat = ExportImageAlgorithm.FileFormat.PNG;
    private int movieFPS = 100;
    private HyperstackDimension movieAnimatedDimension = HyperstackDimension.Frame;
    private AVICompression aviCompression = AVICompression.PNG;
    private int jpegQuality = 100;

    public ExportImage2Algorithm(JIPipeNodeInfo info) {
        super(info);
        this.exporter = new JIPipeDataByMetadataExporter(DataExporterSettings.getInstance());
        registerSubParameter(exporter);
    }

    public ExportImage2Algorithm(ExportImage2Algorithm other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.fileFormat = other.fileFormat;
        this.movieFPS = other.movieFPS;
        this.movieAnimatedDimension = other.movieAnimatedDimension;
        this.aviCompression = other.aviCompression;
        this.jpegQuality = other.jpegQuality;
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        existingMetadata.clear();
        super.runParameterSet(progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot imageSlot = getInputSlot("Input");

        Path outputDirectory = dataBatch.getInputData("Output directory", FolderData.class, progressInfo).toPath();
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputPath = getFirstOutputSlot().getSlotStoragePath().resolve(outputDirectory);
        } else {
            outputPath = outputDirectory;
        }

        // Generate the path
        Path generatedPath = exporter.generatePath(getFirstInputSlot(), dataBatch.getInputSlotRows().get(getFirstInputSlot()), existingMetadata);

        // If absolute -> use the path, otherwise use output directory
        if (generatedPath.isAbsolute()) {
            outputPath = generatedPath;
        } else {
            outputPath = outputPath.resolve(generatedPath);
        }

        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        Path outputFile;
        switch (fileFormat) {
            case JPEG: {
                outputFile = PathUtils.ensureExtension(outputPath, ".jpg", ".jpeg");
                PathUtils.ensureParentDirectoriesExist(outputFile);
                IJ.saveAs(image, "jpeg", outputFile.toString());
            }
            break;
            case PNG: {
                outputFile = PathUtils.ensureExtension(outputPath, ".png");
                PathUtils.ensureParentDirectoriesExist(outputFile);
                IJ.saveAs(image, "png", outputFile.toString());
            }
            break;
            case TIFF: {
                outputFile = PathUtils.ensureExtension(outputPath, ".tif", ".tiff");
                PathUtils.ensureParentDirectoriesExist(outputFile);
                IJ.saveAs(image, "tiff", outputFile.toString());
            }
            break;
            case BMP: {
                outputFile = PathUtils.ensureExtension(outputPath, ".bmp");
                PathUtils.ensureParentDirectoriesExist(outputFile);
                IJ.saveAs(image, "bmp", outputFile.toString());
            }
            break;
            case AVI: {
                outputFile = PathUtils.ensureExtension(outputPath, ".avi");
                PathUtils.ensureParentDirectoriesExist(outputFile);
                ImageJUtils.writeImageToMovie(image, movieAnimatedDimension, movieFPS, outputFile, aviCompression, jpegQuality, progressInfo.detachProgress());
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
    }

    @JIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
    }

    @JIPipeDocumentation(name = "File format", description = "The file format that should be used. " +
            "<ul>" +
            "<li>PNG, JPEG, BMP: 2D images only</li>" +
            "<li>AVI: 2D or 3D images only</li>" +
            "<li>TIFF: All images supported</li>" +
            "</ul>")
    @JIPipeParameter(value = "file-format", uiOrder = -20)
    public ExportImageAlgorithm.FileFormat getFileFormat() {
        return fileFormat;
    }

    @JIPipeParameter("file-format")
    public void setFileFormat(ExportImageAlgorithm.FileFormat fileFormat) {
        this.fileFormat = fileFormat;
        emitParameterUIChangedEvent();
    }

    @JIPipeDocumentation(name = "Animation FPS", description = "Only used if the format is AVI. The number of frames shown per second.")
    @JIPipeParameter("movie-fps")
    public int getMovieFPS() {
        return movieFPS;
    }

    @JIPipeParameter("movie-fps")
    public void setMovieFPS(int movieFPS) {
        this.movieFPS = movieFPS;
    }

    @JIPipeDocumentation(name = "Animated dimension", description = "Only used if the format is AVI. Determines which dimension is animated")
    @JIPipeParameter("movie-animated-dimension")
    public HyperstackDimension getMovieAnimatedDimension() {
        return movieAnimatedDimension;
    }

    @JIPipeParameter("movie-animated-dimension")
    public void setMovieAnimatedDimension(HyperstackDimension movieAnimatedDimension) {
        this.movieAnimatedDimension = movieAnimatedDimension;
    }

    @JIPipeDocumentation(name = "AVI compression", description = "Only used if the format is AVI. Determines how the frames are compressed.")
    @JIPipeParameter("avi-compression")
    public AVICompression getAviCompression() {
        return aviCompression;
    }

    @JIPipeParameter("avi-compression")
    public void setAviCompression(AVICompression aviCompression) {
        this.aviCompression = aviCompression;
    }

    @JIPipeDocumentation(name = "AVI JPEG quality", description = "Only used if the format is AVI and the compression is JPEG. Value from 0-100 " +
            "that determines the JPEG quality (100 is best).")
    @JIPipeParameter("jpeg-quality")
    public int getJpegQuality() {
        return jpegQuality;
    }

    @JIPipeParameter("jpeg-quality")
    public boolean setJpegQuality(int jpegQuality) {
        if (jpegQuality < 0 || jpegQuality > 100)
            return false;
        this.jpegQuality = jpegQuality;
        return true;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (fileFormat != ExportImageAlgorithm.FileFormat.AVI) {
            if ("movie-fps".equals(access.getKey())) {
                return false;
            }
            if ("movie-animated-dimension".equals(access.getKey())) {
                return false;
            }
            if ("avi-compression".equals(access.getKey())) {
                return false;
            }
            if ("jpeg-quality".equals(access.getKey())) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }
}
