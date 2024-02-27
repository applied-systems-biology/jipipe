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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.filters.NonGenericImagePlusDataClassFilter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Create empty image", description = "Creates a new image that is black.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Annotations", create = true, optional = true, description = "Optional annotations that can be referenced in the expression")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class GenerateZeroImage extends JIPipeSimpleIteratingAlgorithm {

    private int width = 256;
    private int height = 256;
    private int sizeZ = 1;
    private int sizeC = 1;
    private int sizeT = 1;
    private JIPipeDataInfoRef dataType = new JIPipeDataInfoRef(ImagePlusGreyscale8UData.class);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateZeroImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateZeroImage(GenerateZeroImage other) {
        super(other);
        this.dataType = new JIPipeDataInfoRef(other.dataType);
        this.width = other.width;
        this.height = other.height;
        this.sizeZ = other.sizeZ;
        this.sizeC = other.sizeC;
        this.sizeT = other.sizeT;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(),
                JIPipe.createData(dataType.getInfo().getDataClass(), new ImageDimensions(width, height, sizeZ, sizeC, sizeT)),
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Data type", description = "The data type that should be created.")
    @JIPipeParameter("data-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class, dataClassFilter = NonGenericImagePlusDataClassFilter.class)
    public JIPipeDataInfoRef getDataType() {
        return dataType;
    }

    @JIPipeParameter("data-type")
    public void setDataType(JIPipeDataInfoRef dataType) {
        this.dataType = dataType;
        if (dataType.getInfo() != null) {
            getFirstOutputSlot().setAcceptedDataType(dataType.getInfo().getDataClass());
        }
    }

    @SetJIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter(value = "width", uiOrder = -20)
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @SetJIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter(value = "height", uiOrder = -15)
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @SetJIPipeDocumentation(name = "Number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public int getSizeZ() {
        return sizeZ;
    }

    @JIPipeParameter("size-z")
    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    @SetJIPipeDocumentation(name = "Number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public int getSizeC() {
        return sizeC;
    }

    @JIPipeParameter("size-c")
    public void setSizeC(int sizeC) {
        this.sizeC = sizeC;
    }

    @SetJIPipeDocumentation(name = "Number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public int getSizeT() {
        return sizeT;
    }

    @JIPipeParameter("size-t")
    public void setSizeT(int sizeT) {
        this.sizeT = sizeT;
    }
}
