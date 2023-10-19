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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@JIPipeDocumentation(name = "Bio-Formats exporter", description = "Writes the OME images into an *.ome.tif file.")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
        "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
        "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nBio-Formats", aliasName = "Bio-Formats Exporter")
public class BioFormatsExporter2 extends JIPipeSimpleIteratingAlgorithm {
    private OMEExporterSettings exporterSettings = new OMEExporterSettings();
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    public BioFormatsExporter2(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporterSettings);
    }

    public BioFormatsExporter2(BioFormatsExporter2 other) {
        super(other);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        registerSubParameter(exporterSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);

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
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));

        // Postprocess path
        outputPath = PathUtils.ensureExtension(outputPath, ".ome.tif", "ome.tiff");
        PathUtils.ensureParentDirectoriesExist(outputPath);

        OMEImageData.OMEExport(inputData, outputPath, exporterSettings);

        iterationStep.addOutputData(getFirstOutputSlot(), new FileData(outputPath), progressInfo);
    }

    @JIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
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
