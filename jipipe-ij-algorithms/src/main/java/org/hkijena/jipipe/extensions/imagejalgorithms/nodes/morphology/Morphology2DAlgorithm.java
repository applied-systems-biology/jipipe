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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform.AddBorder2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.ImageQueryExpressionVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;
import java.util.ArrayList;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Morphological operation 2D", description = "Applies a morphological operation to greyscale images. " +
        "Following operations are included: Erosion, Dilation, Opening, Closing, White Top Hat, Black Top Hat, Gradient, Laplacian, Internal Gradient, and External Gradient. " +
        "More information (including examples) can be found at https://imagej.net/MorphoLibJ.html\n" +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@DefineJIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering", aliasName = "Morphological Filters")
public class Morphology2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final AddBorder2DAlgorithm addBorder2DAlgorithm;
    private Morphology.Operation operation = Morphology.Operation.DILATION;
    private Strel.Shape element = Strel.Shape.DISK;
    private int radius = 1;
    private boolean addBorder = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public Morphology2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.addBorder2DAlgorithm = JIPipe.createNode(AddBorder2DAlgorithm.class);
        registerSubParameter(addBorder2DAlgorithm);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public Morphology2DAlgorithm(Morphology2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.radius = other.radius;
        this.element = other.element;
        this.addBorder = other.addBorder;
        this.addBorder2DAlgorithm = new AddBorder2DAlgorithm(other.addBorder2DAlgorithm);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus originalImg = inputData.getImage();
        ImagePlus img;
        Rectangle crop = null;
        if (!addBorder) {
            img = inputData.getDuplicateImage();
        } else {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            ImageQueryExpressionVariablesInfo.buildVariablesSet(originalImg, variables);

            int left = (int) (addBorder2DAlgorithm.getMarginLeft().evaluateToNumber(variables));
            int top = (int) (addBorder2DAlgorithm.getMarginTop().evaluateToNumber(variables));
//            int right = (int) (addBorder2DAlgorithm.getMarginRight().evaluateToNumber(variables));
//            int bottom = (int) (addBorder2DAlgorithm.getMarginBottom().evaluateToNumber(variables));

            crop = new Rectangle(left, top, originalImg.getWidth(), originalImg.getHeight());
            addBorder2DAlgorithm.clearSlotData();
            addBorder2DAlgorithm.getFirstInputSlot().addData(inputData, new ArrayList<>(iterationStep.getMergedTextAnnotations().values()), JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            addBorder2DAlgorithm.run(runContext, progressInfo.resolve("Add border"));
            img = addBorder2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
            addBorder2DAlgorithm.clearSlotData();
        }
        Strel strel = element.fromRadius(radius);
        ImageStack outputStack = new ImageStack(originalImg.getWidth(), originalImg.getHeight(), originalImg.getProcessor().getColorModel());
        Rectangle finalCrop = crop;
        ImageJUtils.forEachSlice(img, ip -> {
            // apply morphological operation
            ImageProcessor resultProcessor = operation.apply(ip, strel);

            // Keep same color model
            resultProcessor.setColorModel(ip.getColorModel());

            // Crop border if one is set
            if (addBorder) {
                resultProcessor.setRoi(finalCrop);
                resultProcessor = resultProcessor.crop();
            }

            outputStack.addSlice(resultProcessor);
        }, progressInfo);
        ImagePlus result = new ImagePlus(operation.toString(), outputStack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Operation", description = "The morphological operation. Following operations are supported: " +
            "<ul>" +
            "<li><b>Erosion: </b>A local minimum filter</li>" +
            "<li><b>Dilation: </b>A local maximum filter</li>" +
            "<li><b>Opening: </b>Erosion, followed by dilation</li>" +
            "<li><b>Closing: </b>Dilation, followed by erosion</li>" +
            "<li><b>White Top Hat: </b>Image - Opening</li>" +
            "<li><b>Black Top Hat (Black Hat): </b>Closing - Image</li>" +
            "<li><b>Gradient: </b>Dilation - Erosion</li>" +
            "<li><b>Laplacian: </b>(Dilation + Erosion) / 2 - Image</li>" +
            "<li><b>Internal gradient: </b>Image - Erosion</li>" +
            "<li><b>External gradient: </b>Dilation - Image</li>" +
            "</ul>")
    @JIPipeParameter("operation")
    public Morphology.Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Morphology.Operation operation) {
        this.operation = operation;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "Radius of the filter kernel in pixels.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Structure element", description = "The structure element.")
    @JIPipeParameter("element")
    public Strel.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel.Shape element) {
        this.element = element;
    }

    @SetJIPipeDocumentation(name = "Add border before applying operation", description = "If enabled, a custom border is created around the image and removed afterwards. Otherwise, MorphoLibJ will assume " +
            "a default outside value determined by ImageJ.")
    @JIPipeParameter("add-border")
    public boolean isAddBorder() {
        return addBorder;
    }

    @JIPipeParameter("add-border")
    public void setAddBorder(boolean addBorder) {
        if (this.addBorder != addBorder) {
            this.addBorder = addBorder;
            emitParameterUIChangedEvent();
        }
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (!addBorder && subParameter == addBorder2DAlgorithm) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @SetJIPipeDocumentation(name = "Border settings", description = "The following settings control how the border is created around the image before applying the morphological operation. " +
            "The border is automatically removed afterwards.")
    @JIPipeParameter("border-parameters")
    public AddBorder2DAlgorithm getAddBorder2DAlgorithm() {
        return addBorder2DAlgorithm;
    }
}
