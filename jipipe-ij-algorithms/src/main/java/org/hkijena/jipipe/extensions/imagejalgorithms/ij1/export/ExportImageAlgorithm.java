package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.export;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.DataExporterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Export image", description = "Exports incoming images into a non-JIPipe format (PNG, JPEG, BMP, AVI, TIFF). " +
        "Please note support for input images depending on the file format: " +
        "<ul>" +
        "<li>PNG, JPEG, BMP: 2D images only</li>" +
        "<li>AVI: 2D or 3D images only</li>" +
        "<li>TIFF: All images supported</li>" +
        "</ul>")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Exported file", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
public class ExportImageAlgorithm extends JIPipeIteratingAlgorithm {

    private final Set<String> existingMetadata = new HashSet<>();
    private JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");
    private FileFormat fileFormat = FileFormat.PNG;
    private int movieFrameTime = 100;
    private HyperstackDimension movieAnimatedDimension = HyperstackDimension.Frame;
    private AVICompression aviCompression = AVICompression.PNG;
    private int jpegQuality = 100;

    public ExportImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.exporter = new JIPipeDataByMetadataExporter(DataExporterSettings.getInstance());
        registerSubParameter(exporter);
    }

    public ExportImageAlgorithm(ExportImageAlgorithm other) {
        super(other);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.outputDirectory = other.outputDirectory;
        this.fileFormat = other.fileFormat;
        this.movieFrameTime = other.movieFrameTime;
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

        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        String baseName = exporter.generateMetadataString(getFirstInputSlot(), dataBatch.getInputSlotRows().get(getFirstInputSlot()), existingMetadata);
        Path outputFile;
        switch (fileFormat) {
            case JPEG: {
                outputFile = outputPath.resolve(baseName + ".jpg");
                IJ.saveAs(image, "jpeg", outputFile.toString());
            }
            break;
            case PNG: {
                outputFile = outputPath.resolve(baseName + ".png");
                IJ.saveAs(image, "png", outputFile.toString());
            }
            break;
            case TIFF: {
                outputFile = outputPath.resolve(baseName + ".tif");
                IJ.saveAs(image, "tiff", outputFile.toString());
            }
            break;
            case BMP: {
                outputFile = outputPath.resolve(baseName + ".bmp");
                IJ.saveAs(image, "bmp", outputFile.toString());
            }
            break;
            case AVI: {
                outputFile = outputPath.resolve(baseName + ".avi");
                ImageJUtils.writeImageToMovie(image, movieAnimatedDimension, movieFrameTime, outputFile, aviCompression, jpegQuality, progressInfo);
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new FileData(outputFile), progressInfo);
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

    @JIPipeDocumentation(name = "File format", description = "The file format that should be used. " +
            "<ul>" +
            "<li>PNG, JPEG, BMP: 2D images only</li>" +
            "<li>AVI: 2D or 3D images only</li>" +
            "<li>TIFF: All images supported</li>" +
            "</ul>")
    @JIPipeParameter(value = "file-format", uiOrder = -20)
    public FileFormat getFileFormat() {
        return fileFormat;
    }

    @JIPipeParameter("file-format")
    public void setFileFormat(FileFormat fileFormat) {
        this.fileFormat = fileFormat;
        triggerParameterUIChange();
    }

    @JIPipeDocumentation(name = "Animation speed (ms)", description = "Only used if the format is AVI. Determines how long a frame is shown.")
    @JIPipeParameter("movie-frame-time")
    public int getMovieFrameTime() {
        return movieFrameTime;
    }

    @JIPipeParameter("movie-frame-time")
    public void setMovieFrameTime(int movieFrameTime) {
        this.movieFrameTime = movieFrameTime;
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
        if (fileFormat != FileFormat.AVI) {
            if ("movie-frame-time".equals(access.getKey())) {
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

    public enum FileFormat {
        PNG,
        JPEG,
        BMP,
        AVI,
        TIFF
    }
}