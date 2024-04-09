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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.filters.NonGenericImagePlusDataClassFilter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "Convert matrix to image", description = "Converts a table that represents a matrix into an image. The matrix can contain color values (HEX colors) if a color image is requested.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output", create = true)
public class MatrixToImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private JIPipeDataInfoRef outputImageType = new JIPipeDataInfoRef(ImagePlusGreyscale32FData.class);

    public MatrixToImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MatrixToImageAlgorithm(MatrixToImageAlgorithm other) {
        super(other);
        this.outputImageType = other.outputImageType;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImageTypeInfo typeInfo = outputImageType.getInfo().getDataClass().getAnnotation(ImageTypeInfo.class);
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        int width = table.getColumnCount();
        int height = table.getRowCount();

        // Collect data and create image
        RGBColorSpace rgbColorSpace = new RGBColorSpace();
        ColorSpace colorSpace = (ColorSpace) ReflectionUtils.newInstance(typeInfo.colorSpace());
        ImagePlusData outputData = (ImagePlusData) JIPipe.createData(outputImageType.getInfo().getDataClass(), new ImageDimensions(width, height, 1, 1, 1));
        ImagePlus img = outputData.getImage();
        ImageProcessor ip = img.getProcessor();

        final boolean parseColor = ip instanceof ColorProcessor;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (parseColor) {
                    String value = table.getValueAsString(y, x);
                    Color color = ColorUtils.parseColor(value);
                    if (color == null) {
                        // Parse as double
                        double numericValue = StringUtils.parseDouble(value);
                        if (Double.isNaN(numericValue) || Double.isInfinite(numericValue)) {
                            numericValue = 0;
                        } else {
                            numericValue = Math.max(0, Math.min(255, numericValue));
                        }
                        color = new Color((int) numericValue, (int) numericValue, (int) numericValue);
                        int newPixel = colorSpace.convert(color.getRGB(), rgbColorSpace);
                        ip.set(x, y, newPixel);
                    } else {
                        int newPixel = colorSpace.convert(color.getRGB(), rgbColorSpace);
                        ip.set(x, y, newPixel);
                    }
                } else {
                    ip.setf(x, y, (float) table.getValueAsDouble(y, x));
                }
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Output image type", description = "The image type that is generated.")
    @JIPipeParameter(value = "output-image-type", important = true)
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class, dataClassFilter = NonGenericImagePlusDataClassFilter.class)
    public JIPipeDataInfoRef getOutputImageType() {
        return outputImageType;
    }

    @JIPipeParameter("output-image-type")
    public void setOutputImageType(JIPipeDataInfoRef outputImageType) {
        if (outputImageType.getInfo() != this.outputImageType.getInfo()) {
            this.outputImageType = outputImageType;
            if (outputImageType.getInfo() != null) {
                getFirstOutputSlot().setAcceptedDataType(outputImageType.getInfo().getDataClass());
            }
        }
    }
}
