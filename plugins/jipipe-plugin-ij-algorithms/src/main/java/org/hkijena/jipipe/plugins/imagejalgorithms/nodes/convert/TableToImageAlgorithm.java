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
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.filters.NonGenericImagePlusDataClassFilter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ZeroTableColumn;
import org.hkijena.jipipe.utils.ReflectionUtils;

@SetJIPipeDocumentation(name = "Convert table to image", description = "Converts a table of pixel information into an image")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class TableToImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final JIPipeDynamicParameterCollection columnAssignment;
    private JIPipeDataInfoRef outputImageType = new JIPipeDataInfoRef(ImagePlusGreyscale8UData.class);
    private OptionalIntegerParameter customWidth = new OptionalIntegerParameter();
    private OptionalIntegerParameter customHeight = new OptionalIntegerParameter();
    private OptionalIntegerParameter customSizeZ = new OptionalIntegerParameter();
    private OptionalIntegerParameter customSizeC = new OptionalIntegerParameter();
    private OptionalIntegerParameter customSizeT = new OptionalIntegerParameter();


    public TableToImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.columnAssignment = new JIPipeDynamicParameterCollection(false);
        updateColumnAssignment();
    }

    public TableToImageAlgorithm(TableToImageAlgorithm other) {
        super(other);
        this.columnAssignment = new JIPipeDynamicParameterCollection(other.columnAssignment);
        this.outputImageType = other.outputImageType;
        this.customWidth = new OptionalIntegerParameter(other.customWidth);
        this.customHeight = new OptionalIntegerParameter(other.customHeight);
        this.customSizeC = new OptionalIntegerParameter(other.customSizeC);
        this.customSizeZ = new OptionalIntegerParameter(other.customSizeZ);
        this.customSizeT = new OptionalIntegerParameter(other.customSizeT);
        updateColumnAssignment();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImageTypeInfo typeInfo = outputImageType.getInfo().getDataClass().getAnnotation(ImageTypeInfo.class);
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        progressInfo.log("Determining image properties");
        int width = findSizeFromTableOrParameter(table, customWidth, "x");
        int height = findSizeFromTableOrParameter(table, customHeight, "y");
        int sizeZ = typeInfo.numDimensions() > 2 ? findSizeFromTableOrParameter(table, customSizeZ, "z") : 1;
        int sizeC = typeInfo.numDimensions() > 2 ? findSizeFromTableOrParameter(table, customSizeC, "c") : 1;
        int sizeT = typeInfo.numDimensions() > 2 ? findSizeFromTableOrParameter(table, customSizeT, "t") : 1;

        // Collect data and create image

        ColorSpace colorSpace = (ColorSpace) ReflectionUtils.newInstance(typeInfo.colorSpace());
        ImagePlusData outputData = (ImagePlusData) JIPipe.createData(outputImageType.getInfo().getDataClass(), new ImageDimensions(width, height, sizeZ, sizeC, sizeT));
        ImagePlus img = outputData.getImage();

        // Standard columns
        TableColumnSourceExpressionParameter xColumnSource = columnAssignment.getValue("x", TableColumnSourceExpressionParameter.class);
        TableColumnSourceExpressionParameter yColumnSource = columnAssignment.getValue("y", TableColumnSourceExpressionParameter.class);
        TableColumn xColumn = xColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
        TableColumn yColumn = yColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());

        // Extended columns
        TableColumn zColumn = new ZeroTableColumn();
        TableColumn cColumn = new ZeroTableColumn();
        TableColumn tColumn = new ZeroTableColumn();

        if (typeInfo.numDimensions() > 2) {
            TableColumnSourceExpressionParameter zColumnSource = columnAssignment.getValue("z", TableColumnSourceExpressionParameter.class);
            TableColumnSourceExpressionParameter cColumnSource = columnAssignment.getValue("c", TableColumnSourceExpressionParameter.class);
            TableColumnSourceExpressionParameter tColumnSource = columnAssignment.getValue("t", TableColumnSourceExpressionParameter.class);
            zColumn = zColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
            cColumn = cColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
            tColumn = tColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
        }

        progressInfo.log("Generating image from " + table.getRowCount() + " rows (this might take long)");

        if (colorSpace instanceof GreyscaleColorSpace) {
            TableColumnSourceExpressionParameter valueColumnSource = columnAssignment.getValue("value", TableColumnSourceExpressionParameter.class);
            TableColumn valueColumn = valueColumnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());

            ImageProcessor lastProcessor = null;
            int lastZ = -1;
            int lastC = -1;
            int lastT = -1;

            for (int row = 0; row < table.getRowCount(); row++) {
                // Get z,c,t
                // then get the appropriate processor
                int z = (int) zColumn.getRowAsDouble(row);
                int c = (int) cColumn.getRowAsDouble(row);
                int t = (int) tColumn.getRowAsDouble(row);
                if (lastProcessor == null || z != lastZ || c != lastC || t != lastT) {
                    lastProcessor = ImageJUtils.getSliceZero(img, c, z, t);
                    lastC = c;
                    lastZ = z;
                    lastT = t;
                }

                // Get x,y
                int x = (int) xColumn.getRowAsDouble(row);
                int y = (int) yColumn.getRowAsDouble(row);

                // Write value
                float value = (float) valueColumn.getRowAsDouble(row);
                lastProcessor.setf(x, y, value);
            }
        } else {
            int[] buffer = new int[colorSpace.getNChannels()];
            TableColumn[] channelColumns = new TableColumn[colorSpace.getNChannels()];
            for (int i = 0; i < colorSpace.getNChannels(); i++) {
                channelColumns[i] = columnAssignment.getValue(colorSpace.getChannelName(i), TableColumnSourceExpressionParameter.class).pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
            }

            ImageProcessor lastProcessor = null;
            int lastZ = -1;
            int lastC = -1;
            int lastT = -1;

            for (int row = 0; row < table.getRowCount(); row++) {
                // Get z,c,t
                // then get the appropriate processor
                int z = (int) zColumn.getRowAsDouble(row);
                int c = (int) cColumn.getRowAsDouble(row);
                int t = (int) tColumn.getRowAsDouble(row);
                if (lastProcessor == null || z != lastZ || c != lastC || t != lastT) {
                    lastProcessor = ImageJUtils.getSliceZero(img, c, z, t);
                    lastC = c;
                    lastZ = z;
                    lastT = t;
                }

                // Get x,y
                int x = (int) xColumn.getRowAsDouble(row);
                int y = (int) yColumn.getRowAsDouble(row);

                // channel values
                for (int i = 0; i < colorSpace.getNChannels(); i++) {
                    buffer[i] = (int) channelColumns[i].getRowAsDouble(row);
                }

                // Write value
                int value = colorSpace.composePixel(buffer);
                lastProcessor.set(x, y, value);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    private int findSizeFromTableOrParameter(ResultsTableData table, OptionalIntegerParameter parameter, String key) {
        if (parameter.isEnabled()) {
            return parameter.getContent();
        } else {
            return findSizeFromTable(table, columnAssignment.getValue(key, TableColumnSourceExpressionParameter.class));
        }
    }

    private int findSizeFromTable(ResultsTableData table, TableColumnSourceExpressionParameter columnSource) {
        TableColumn tableColumn = columnSource.pickOrGenerateColumn(table, new JIPipeExpressionVariablesMap());
        int max = 0;
        for (int i = 0; i < tableColumn.getRows(); i++) {
            max = Math.max(max, (int) tableColumn.getRowAsDouble(i));
        }
        return max + 1;
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
            updateColumnAssignment();
            if (outputImageType.getInfo() != null) {
                getFirstOutputSlot().setAcceptedDataType(outputImageType.getInfo().getDataClass());
            }
        }
    }

    @SetJIPipeDocumentation(name = "Custom width", description = "If enabled, the image width is determined by this parameter. Otherwise, it is inferred from the table.")
    @JIPipeParameter("custom-width")
    public OptionalIntegerParameter getCustomWidth() {
        return customWidth;
    }

    @JIPipeParameter("custom-width")
    public void setCustomWidth(OptionalIntegerParameter customWidth) {
        this.customWidth = customWidth;
    }

    @SetJIPipeDocumentation(name = "Custom height", description = "If enabled, the image height is determined by this parameter. Otherwise, it is inferred from the table.")
    @JIPipeParameter("custom-height")
    public OptionalIntegerParameter getCustomHeight() {
        return customHeight;
    }

    @JIPipeParameter("custom-height")
    public void setCustomHeight(OptionalIntegerParameter customHeight) {
        this.customHeight = customHeight;
    }

    @SetJIPipeDocumentation(name = "Custom depth", description = "If enabled, the image depth is determined by this parameter. Otherwise, it is inferred from the table.")
    @JIPipeParameter("custom-z-size")
    public OptionalIntegerParameter getCustomSizeZ() {
        return customSizeZ;
    }

    @JIPipeParameter("custom-z-size")
    public void setCustomSizeZ(OptionalIntegerParameter customSizeZ) {
        this.customSizeZ = customSizeZ;
    }

    @SetJIPipeDocumentation(name = "Custom channel size", description = "If enabled, the number of channel slices is determined by this parameter. Otherwise, it is inferred from the table.")
    @JIPipeParameter("custom-c-size")
    public OptionalIntegerParameter getCustomSizeC() {
        return customSizeC;
    }

    @JIPipeParameter("custom-c-size")
    public void setCustomSizeC(OptionalIntegerParameter customSizeC) {
        this.customSizeC = customSizeC;
    }

    @SetJIPipeDocumentation(name = "Custom frames size", description = "If enabled, the number of frame slices is determined by this parameter. Otherwise, it is inferred from the table.")
    @JIPipeParameter("custom-t-size")
    public OptionalIntegerParameter getCustomSizeT() {
        return customSizeT;
    }

    @JIPipeParameter("custom-t-size")
    public void setCustomSizeT(OptionalIntegerParameter customSizeT) {
        this.customSizeT = customSizeT;
    }

    private void updateColumnAssignment() {
        getColumnAssignment().clear();
        if (outputImageType.getInfo() != null) {
            ImageTypeInfo typeInfo = outputImageType.getInfo().getDataClass().getAnnotation(ImageTypeInfo.class);
            if (typeInfo == null) {
                return;
            }
            boolean wantsZCT = typeInfo.numDimensions() > 2;
            ColorSpace colorSpace = (ColorSpace) ReflectionUtils.newInstance(typeInfo.colorSpace());
            columnAssignment.addParameter("x",
                    TableColumnSourceExpressionParameter.class,
                    "X",
                    "The pixel x coordinate");
            columnAssignment.get("x").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"x\""));
            columnAssignment.addParameter("y",
                    TableColumnSourceExpressionParameter.class,
                    "Y",
                    "The pixel y coordinate");
            columnAssignment.get("y").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"y\""));
            if (wantsZCT) {
                columnAssignment.addParameter("z",
                        TableColumnSourceExpressionParameter.class,
                        "Z",
                        "The pixel z coordinate");
                columnAssignment.get("z").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"z\""));
                columnAssignment.addParameter("c",
                        TableColumnSourceExpressionParameter.class,
                        "c",
                        "The pixel channel coordinate");
                columnAssignment.get("c").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"c\""));
                columnAssignment.addParameter("t",
                        TableColumnSourceExpressionParameter.class,
                        "T",
                        "The pixel frame (T) coordinate");
                columnAssignment.get("t").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"t\""));
            }
            if (colorSpace instanceof GreyscaleColorSpace) {
                columnAssignment.addParameter("value",
                        TableColumnSourceExpressionParameter.class,
                        "Value",
                        "The greyscale value");
                columnAssignment.get("value").set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"value\""));
            } else {
                for (int i = 0; i < colorSpace.getNChannels(); i++) {
                    columnAssignment.addParameter(colorSpace.getChannelName(i),
                            TableColumnSourceExpressionParameter.class,
                            colorSpace.getChannelName(i),
                            "The values for the channel '" + colorSpace.getChannelName(i) + "'");
                    columnAssignment.get(colorSpace.getChannelName(i)).set(new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn,
                            "\"" + colorSpace.getChannelShortName(i) + "\""));
                }
            }
        }
        emitParameterStructureChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Column assignments", description = "Following parameters determine how pixels are extracted from the table.")
    @JIPipeParameter("column-assignments")
    public JIPipeDynamicParameterCollection getColumnAssignment() {
        return columnAssignment;
    }
}
