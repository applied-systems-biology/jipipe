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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io;

import com.google.common.primitives.Ints;
import ij.ImagePlus;
import ome.xml.meta.OMEXMLMetadata;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ZARRUtils;
import org.hkijena.jipipe.plugins.strings.URIData;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Export OME ZARR (IJ1)", description = "Writes an OME ZARR")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeInputSlot(value = OMEImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = URIData.class, name = "Output", create = true)
@AddJIPipeCitation("https://ngff.openmicroscopy.org/latest/")
public class ExportImagePlusAsOMEZARRAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();
    private OutputFormat outputFormat = OutputFormat.ZippedZARR;
    private Compression compression = Compression.GZIP;
    private boolean createPyramid = true;
    private OptionalJIPipeExpressionParameter chunkSizeExpression = new OptionalJIPipeExpressionParameter(false, "ARRAY(default.cs.x, default.cs.y, default.cs.c, default.cs.z, default.cs.t)");

    public ExportImagePlusAsOMEZARRAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportImagePlusAsOMEZARRAlgorithm(ExportImagePlusAsOMEZARRAlgorithm other) {
        super(other);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        this.outputFormat = other.outputFormat;
        this.compression = other.compression;
        this.createPyramid = other.createPyramid;
        this.chunkSizeExpression = new OptionalJIPipeExpressionParameter(other.chunkSizeExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData omeImageData = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        ImagePlus imp = omeImageData.getImage();
        int[] defaultChunkSize = ZARRUtils.computeOptimalChunkSizes(new int[] { imp.getWidth(), imp.getHeight(), imp.getNChannels(), imp.getNSlices(), imp.getNFrames() });
        if(chunkSizeExpression.isEnabled()) {
            JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
            variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
            variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());
            variablesMap.put("default.cz.x", defaultChunkSize[0]);
            variablesMap.put("default.cz.y", defaultChunkSize[1]);
            variablesMap.put("default.cz.c", defaultChunkSize[2]);
            variablesMap.put("default.cz.z", defaultChunkSize[3]);
            variablesMap.put("default.cz.t", defaultChunkSize[4]);
            List<Double> evaluationResults = chunkSizeExpression.getContent().evaluateToDoubleList(variablesMap);
            for (int i = 0; i < Math.min(evaluationResults.size(), defaultChunkSize.length); i++) {
                defaultChunkSize[i] = evaluationResults.get(i).intValue();
            }
        }

        // Cleanup chunk sizes
        int[] updatedChunkSize = ZARRUtils.cleanupIJ1ChunkSize(defaultChunkSize);
        progressInfo.log("Chunk size for ZARR export is " + JsonUtils.toJsonString(updatedChunkSize));

        // Calculate final path
        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                imp.toString(),
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));


        if(outputFormat == OutputFormat.ZippedZARR) {
            // Add extensions and delete existing file/directory
            outputPath = PathUtils.ensureExtension(outputPath, ".zarr.zip");
            PathUtils.deleteIfExists(outputPath, progressInfo);

            // Create URI (unofficial one)
            String uri = ZARRUtils.pathToZARRURI(outputPath);

            // Create a temporary storage
            try(JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo.resolve("ZIP"), outputPath)) {
                Path tmpPath = storage.getFileSystemPath();
                writeZARR(imp, omeImageData.getMetadata(), updatedChunkSize,tmpPath, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            iterationStep.addOutputData(getFirstOutputSlot(), new URIData(uri), progressInfo);
        }
        else if(outputFormat == OutputFormat.ZARRDirectory) {

            // Add extensions and delete existing file/directory
            outputPath = PathUtils.ensureExtension(outputPath, ".zarr");
            PathUtils.deleteIfExists(outputPath, progressInfo);
            PathUtils.createDirectories(outputPath);

            // Create URI (unofficial one)
            String uri = ZARRUtils.pathToZARRURI(outputPath);

            writeZARR(imp, omeImageData.getMetadata(), updatedChunkSize, outputPath, progressInfo);

            iterationStep.addOutputData(getFirstOutputSlot(), new URIData(uri), progressInfo);
        }
        else {
            throw new RuntimeException("Unsupported output format " + outputFormat);
        }


    }

    private void writeZARR(ImagePlus imp, OMEXMLMetadata metadata, int[] updatedChunkSize, Path path, JIPipeProgressInfo progressInfo) {
        N5ScalePyramidExporter exporter = new N5ScalePyramidExporter();
        exporter.setOptions(imp,
                ZARRUtils.pathToZARRURI(path),
                "",
                N5ScalePyramidExporter.ZARR_FORMAT,
                Ints.join(",", updatedChunkSize),
                createPyramid,
                N5ScalePyramidExporter.DOWN_SAMPLE,
                N5Importer.MetadataOmeZarrKey,
                compression.getNativeValue());
        progressInfo.log("Exporting ZARR to " + path + " (URI " + ZARRUtils.pathToZARRURI(path) + ")");
        exporter.run();

        if(metadata != null) {
            ZARRUtils.writeOMEXMLToZARR(path, metadata, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Chunk size", description = "If enabled, sets the chunk size via an expression that must return an array of five numbers, corresponding to the ImageJ1 axis order is X,Y,C,Z,T. " +
            "Please note that if you come from the N5 plugin, we recommend to always return all 5 axis sizes (regardless whether the size is one), as this node will apply automated processing of the inputs. " +
            "The default chunk sizes are calculated for each axis with the following principle: " +
            "<ul>" +
            "<li>Less than 32: no chunking</li>" +
            "<li>Between 32 and 128: chunk size 32</li>" +
            "<li>Between 128 and 512: chunk size 64</li>" +
            "<li>Other: chunk size 128</li>" +
            "</ul>")
    @JIPipeParameter("chunk-size")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "default.cz.x", name = "Default chunk size (X)", description = "The default X chunk size")
    @AddJIPipeExpressionParameterVariable(key = "default.cz.y", name = "Default chunk size (Y)", description = "The default Y chunk size")
    @AddJIPipeExpressionParameterVariable(key = "default.cz.c", name = "Default chunk size (C)", description = "The default C chunk size")
    @AddJIPipeExpressionParameterVariable(key = "default.cz.z", name = "Default chunk size (Z)", description = "The default Z chunk size")
    @AddJIPipeExpressionParameterVariable(key = "default.cz.t", name = "Default chunk size (T)", description = "The default T chunk size")
    public OptionalJIPipeExpressionParameter getChunkSizeExpression() {
        return chunkSizeExpression;
    }

    @JIPipeParameter("chunk-size")
    public void setChunkSizeExpression(OptionalJIPipeExpressionParameter chunkSizeExpression) {
        this.chunkSizeExpression = chunkSizeExpression;
    }

    @SetJIPipeDocumentation(name = "Create multi-resolution pyramid", description = "Writes multiple resolutions. Increases size and processing time, but makes the ZARR more useful for cloud storage and image viewers.")
    @JIPipeParameter("create-pyramid")
    public boolean isCreatePyramid() {
        return createPyramid;
    }

    @JIPipeParameter("create-pyramid")
    public void setCreatePyramid(boolean createPyramid) {
        this.createPyramid = createPyramid;
    }

    @SetJIPipeDocumentation(name = "Compression", description = "The compression that should be applied")
    @JIPipeParameter("compression")
    public Compression getCompression() {
        return compression;
    }

    @JIPipeParameter("compression")
    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    @SetJIPipeDocumentation(name = "Output path", description = "The output folder path or ZIP file")
    @JIPipeParameter(value = "file-path", important = true)
    public DataExportExpressionParameter getFilePath() {
        return filePath;
    }

    @JIPipeParameter("file-path")
    public void setFilePath(DataExportExpressionParameter filePath) {
        this.filePath = filePath;
    }

    @SetJIPipeDocumentation(name = "Output format", description = "The output format. Either a ZARR directory or a ZARR directory compressed as *.zip file.")
    @JIPipeParameter(value = "output-format", important = true)
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    @JIPipeParameter("output-format")
    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Export to ZARR directory", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectOutputDirectoryDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(), canvasUI.getWorkbench(), "Select output file", PathType.DirectoriesOnly);
        if (result != null) {
            setOutputFormat(OutputFormat.ZARRDirectory);
            setFilePath(result);
            emitParameterChangedEvent("file-path");
        }
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Export to zipped ZARR directory", description = "Selects where the data should be exported", icon = "actions/document-export.png", buttonIcon = "actions/color-select.png", buttonText = "Select")
    public void selectOutputZipDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        DataExportExpressionParameter result = DataExportExpressionParameter.showPathChooser(canvasUI.getDesktopWorkbench().getWindow(), canvasUI.getWorkbench(), "Select output file", PathType.FilesOnly, UIUtils.EXTENSION_FILTER_ZARR_ZIP);
        if (result != null) {
            setOutputFormat(OutputFormat.ZippedZARR);
            setFilePath(result);
            emitParameterChangedEvent("file-path");
        }
    }

    public enum Compression {
        GZIP("GZIP (Default)", "gzip"),
        Raw("Uncompressed", "raw"),
        LZ4("LZ4", "lz4"),
        XZ("XZ", "xz"),
        BLOSC("BLOSC", "blosc"),
        ZSTD("ZSTD", "zstd");

        private final String label;
        private final String nativeValue;

        Compression(String label, String nativeValue) {
            this.label = label;
            this.nativeValue = nativeValue;
        }

        public String getLabel() {
            return label;
        }

        public String getNativeValue() {
            return nativeValue;
        }


        @Override
        public String toString() {
            return label;
        }
    }

    public enum OutputFormat {
        ZARRDirectory("ZARR directory *.zarr"),
        ZippedZARR("Zipped ZARR (*.zarr.zip)");

        private final String name;

        OutputFormat(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
