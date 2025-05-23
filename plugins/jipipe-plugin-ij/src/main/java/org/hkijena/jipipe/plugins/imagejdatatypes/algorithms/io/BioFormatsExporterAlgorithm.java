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

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.OMEExporterSettings;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDataExporterApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Bio-Formats exporter", description = "Deprecated. Please use the new node. Writes the OME images into an *.ome.tif file.")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeInputSlot(value = OMEImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Output", create = true)
@AddJIPipeCitation("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
        "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
        "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nBio-Formats", aliasName = "Bio-Formats Exporter")
@LabelAsJIPipeHidden
@Deprecated
public class BioFormatsExporterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Set<String> existingMetadata = new HashSet<>();
    private OMEExporterSettings exporterSettings = new OMEExporterSettings();
    private JIPipeDataByMetadataExporter exporter;
    private Path outputDirectory = Paths.get("exported-data");

    public BioFormatsExporterAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporterSettings);
        this.exporter = new JIPipeDataByMetadataExporter(JIPipeDataExporterApplicationSettings.getInstance());
        registerSubParameter(exporter);
    }

    public BioFormatsExporterAlgorithm(BioFormatsExporterAlgorithm other) {
        super(other);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        this.exporter = new JIPipeDataByMetadataExporter(other.exporter);
        this.outputDirectory = other.outputDirectory;
        registerSubParameter(exporterSettings);
        registerSubParameter(exporter);
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        existingMetadata.clear();
        super.runParameterSet(runContext, progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path outputPath;
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputPath = getFirstOutputSlot().getSlotStoragePath().resolve(outputDirectory);
        } else {
            outputPath = outputDirectory;
        }

        // Generate the path
        Path generatedPath = exporter.generatePath(getFirstInputSlot(), iterationStep.getInputSlotRows().get(getFirstInputSlot()), existingMetadata);

        // If absolute -> use the path, otherwise use output directory
        if (generatedPath.isAbsolute()) {
            outputPath = generatedPath;
        } else {
            outputPath = outputPath.resolve(generatedPath);
        }

        // Postprocess path
        outputPath = PathUtils.ensureExtension(outputPath, ".ome.tif", "ome.tiff");
        PathUtils.ensureParentDirectoriesExist(outputPath);


        OMEImageData input = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        OMEImageData.OMEExport(input, outputPath, exporterSettings);

        iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }

    @SetJIPipeDocumentation(name = "File name generation", description = "Following settings control how the output file names are generated from metadata columns.")
    @JIPipeParameter("exporter")
    public JIPipeDataByMetadataExporter getExporter() {
        return exporter;
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
}
