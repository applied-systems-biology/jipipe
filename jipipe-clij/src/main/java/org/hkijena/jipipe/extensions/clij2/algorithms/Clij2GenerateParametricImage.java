package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateParametricImage;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateParametricImage}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Parametric Image", description = "Take a labelmap and a vector of values to replace label 1 with the 1st value in the vector. " + "Note that indexing in the vector starts at zero. The 0th entry corresponds to background in the label map.Internally this method just calls ReplaceIntensities." + " Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "label_map", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "parameter_value_vector", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "parametric_image_destination", autoCreate = true)

public class Clij2GenerateParametricImage extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GenerateParametricImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateParametricImage(Clij2GenerateParametricImage other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer label_map = dataBatch.getInputData(getInputSlot("label_map"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer parameter_value_vector = dataBatch.getInputData(getInputSlot("parameter_value_vector"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer parametric_image_destination = clij2.create(label_map);
        GenerateParametricImage.generateParametricImage(clij2, label_map, parameter_value_vector, parametric_image_destination);

        dataBatch.addOutputData(getOutputSlot("parametric_image_destination"), new CLIJImageData(parametric_image_destination), progressInfo);
    }

}