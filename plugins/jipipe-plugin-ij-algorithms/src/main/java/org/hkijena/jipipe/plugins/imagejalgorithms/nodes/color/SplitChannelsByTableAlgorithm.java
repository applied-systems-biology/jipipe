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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ChannelSplitter}
 */
@SetJIPipeDocumentation(name = "Split channels by table", description = "Splits multichannel images into multiple greyscale images. " +
        "This operation is applied for each 2D image slice. This node uses a reference table as secondary input that matches data to a channel assignment configuration.\n" +
        "To configure this node, setup the set of annotation columns and the set of channel columns. The channel columns must be named according to the real (not labelled!) output slot name.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true, description = "Contains the image to be split")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Table", create = true, description = "The table that matches images to channel configurations")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Split Channels (by table)")
public class SplitChannelsByTableAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean ignoreMissingChannels = false;
    private boolean ignoreMissingOutputs = true;
    private boolean ignoreMissingImages = false;
    private boolean ignoreAnnotationColumnsOnTrivialMatch = true;
    private OptionalTextAnnotationNameParameter channelIndexAnnotation = new OptionalTextAnnotationNameParameter("Channel index", true);
    private OptionalTextAnnotationNameParameter channelNameAnnotation = new OptionalTextAnnotationNameParameter("Channel", true);
    private JIPipeExpressionParameter annotationColumnFilter = new JIPipeExpressionParameter("STRING_STARTS_WITH(column_name, \"#\")");
    private JIPipeExpressionParameter channelColumnFilter = new JIPipeExpressionParameter("NOT STRING_STARTS_WITH(column_name, \"#\")");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SplitChannelsByTableAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addFromAnnotations(SplitChannelsByTableAlgorithm.class)
                .restrictOutputTo(JIPipe.getDataTypes().findDataTypesByInterfaces(ImagePlusData.class))
                .sealInput()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public SplitChannelsByTableAlgorithm(SplitChannelsByTableAlgorithm other) {
        super(other);
        this.ignoreMissingChannels = other.ignoreMissingChannels;
        this.channelIndexAnnotation = new OptionalTextAnnotationNameParameter(other.channelIndexAnnotation);
        this.channelNameAnnotation = new OptionalTextAnnotationNameParameter(other.channelNameAnnotation);
        this.annotationColumnFilter = new JIPipeExpressionParameter(other.annotationColumnFilter);
        this.channelColumnFilter = new JIPipeExpressionParameter(other.channelColumnFilter);
        this.ignoreAnnotationColumnsOnTrivialMatch = other.ignoreAnnotationColumnsOnTrivialMatch;
        this.ignoreMissingImages = other.ignoreMissingImages;
        this.ignoreMissingOutputs = other.ignoreMissingOutputs;
    }

    private static ImagePlus[] splitByChannel(ImagePlus imp) {
        ImagePlus[] split;
        if (!imp.isComposite() && imp.getType() != ImagePlus.COLOR_RGB) {
            imp = new CompositeImage(imp);
        }
        if (imp.isComposite()) {
            split = ChannelSplitter.split(imp);
        } else {
            String title = imp.getTitle();
            Calibration cal = imp.getCalibration();
            ImageStack[] channels = ChannelSplitter.splitRGB(imp.getStack(), true);
            ImagePlus rImp = new ImagePlus(title + " (red)", channels[0]);
            rImp.setCalibration(cal);
            ImagePlus gImp = new ImagePlus(title + " (green)", channels[1]);
            gImp.setCalibration(cal);
            ImagePlus bImp = new ImagePlus(title + " (blue)", channels[2]);
            bImp.setCalibration(cal);
            split = new ImagePlus[]{rImp, gImp, bImp};
        }
        return split;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ResultsTableData table = iterationStep.getInputData("Table", ResultsTableData.class, progressInfo);

        ImagePlus[] split = splitByChannel(imp);

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
        variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());

        // Match annotations
        int targetRow = -1;
        if (table.getRowCount() == 1 && ignoreAnnotationColumnsOnTrivialMatch) {
            progressInfo.log("Trivial annotations match to row=0");
            targetRow = 0;
        } else {
            // Select columns of interest
            Set<String> columnNames = table.getColumnNames().stream().filter(s -> {
                variablesMap.set("column_name", s);
                return annotationColumnFilter.test(variablesMap);
            }).collect(Collectors.toSet());

            // Get original annotations
            Map<String, String> imageAnnotations = JIPipeTextAnnotation.annotationListToMap(iterationStep.getOriginalTextAnnotations("Image"),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting);

            outer:
            for (int i = 0; i < table.getRowCount(); i++) {
                for (String columnName : columnNames) {
                    if (!Objects.equals(table.getValueAsString(i, columnName), imageAnnotations.getOrDefault(columnName, ""))) {
                        continue outer;
                    }
                }

                // Match detected
                progressInfo.log("Matched annotations to row=" + i);
                targetRow = i;
                break;
            }
        }

        if (targetRow == -1) {
            if (ignoreMissingImages) {
                progressInfo.log("Ignoring image without matching annotations");
                return;
            } else {
                throw new JIPipeValidationRuntimeException(new NullPointerException("Unable to find channel assignment!"),
                        "Unable to find channel assignment!",
                        "None of the assignments in the provided table match to the image",
                        "Please review the table or enable 'Ignore missing images'.");
            }
        }


        Set<String> channelAssignmentColumnNames = table.getColumnNames().stream().filter(s -> {
            variablesMap.set("column_name", s);
            return channelColumnFilter.test(variablesMap);
        }).collect(Collectors.toSet());

        for (String outputSlotName : channelAssignmentColumnNames) {
            if (getOutputSlot(outputSlotName) == null) {
                if (ignoreMissingOutputs) {
                    progressInfo.log("Ignoring missing output slot " + outputSlotName);
                    continue;
                } else {
                    throw new JIPipeValidationRuntimeException(new NullPointerException("Requested output slot " + outputSlotName + " was not found."),
                            "Could not find output slot with name " + outputSlotName,
                            "The node wants to output a channel to the slot '" + outputSlotName + "' but it is not present.",
                            "Please check if the slot is present. You can also enable 'Ignore missing outputs' to skip such occurrences silently.");
                }
            }

            int channelIndex = (int) table.getValueAsDouble(targetRow, outputSlotName);
            if (channelIndex < 0 || channelIndex >= split.length) {
                if (ignoreMissingChannels) {
                    progressInfo.log("Ignoring missing channel index " + channelIndex);
                    continue;
                } else {
                    throw new JIPipeValidationRuntimeException(new IndexOutOfBoundsException("Requested channel " + channelIndex + ", but only " + split.length + " channels are available."),
                            "Could not find channel with index " + channelIndex,
                            "You requested that the input channel " + channelIndex + " should be assigned to slot '" + outputSlotName + "', but there are only " + split.length + " channels available.",
                            "Please check if the index is correct. The first channel index is zero. You can also enable 'Ignore missing channels' to skip such occurrences silently.");
                }
            }

            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            channelIndexAnnotation.addAnnotationIfEnabled(annotations, channelIndex + "");
            channelNameAnnotation.addAnnotationIfEnabled(annotations, outputSlotName);

            iterationStep.addOutputData(outputSlotName, new ImagePlusData(split[channelIndex]), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Ignore 'Annotation columns' on trivial match", description = "If enabled and an image is matched to a table with exactly one row, " +
            "annotation matching is not applied and the singular row is used.")
    @JIPipeParameter("ignore-annotation-columns-on-trivial-match")
    public boolean isIgnoreAnnotationColumnsOnTrivialMatch() {
        return ignoreAnnotationColumnsOnTrivialMatch;
    }

    @JIPipeParameter("ignore-annotation-columns-on-trivial-match")
    public void setIgnoreAnnotationColumnsOnTrivialMatch(boolean ignoreAnnotationColumnsOnTrivialMatch) {
        this.ignoreAnnotationColumnsOnTrivialMatch = ignoreAnnotationColumnsOnTrivialMatch;
    }

    @SetJIPipeDocumentation(name = "Ignore missing images", description = "If enabled, the node will silently skip all images where no channel assignment is found. " +
            "Otherwise, the pipeline/partition/loop will stop.")
    @JIPipeParameter("ignore-missing-images")
    public boolean isIgnoreMissingImages() {
        return ignoreMissingImages;
    }

    @JIPipeParameter("ignore-missing-images")
    public void setIgnoreMissingImages(boolean ignoreMissingImages) {
        this.ignoreMissingImages = ignoreMissingImages;
    }

    @SetJIPipeDocumentation(name = "Ignore missing outputs", description = "If enabled, the node will silently ignore missing output slots. Otherwise, the pipeline/partition/loop will stop.")
    @JIPipeParameter("ignore-missing-outputs")
    public boolean isIgnoreMissingOutputs() {
        return ignoreMissingOutputs;
    }

    @JIPipeParameter("ignore-missing-outputs")
    public void setIgnoreMissingOutputs(boolean ignoreMissingOutputs) {
        this.ignoreMissingOutputs = ignoreMissingOutputs;
    }

    @SetJIPipeDocumentation(name = "Annotation columns", description = "Determines the table columns that are matched to the text annotations of the input images. " +
            "Please ensure that the annotations in the table exactly match the one in your data. " +
            "By default, all columns starting with a '#' are selected. Use <code>column_name IN ARRAY(...)</code> to set a predefined list of columns.")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "column_name", name = "Column name", description = "The name of the currently processed column")
    @JIPipeExpressionParameterSettings(hint = "per column")
    @JIPipeParameter(value = "annotation-column-filter", important = true, uiOrder = -99)
    public JIPipeExpressionParameter getAnnotationColumnFilter() {
        return annotationColumnFilter;
    }

    @JIPipeParameter("annotation-column-filter")
    public void setAnnotationColumnFilter(JIPipeExpressionParameter annotationColumnFilter) {
        this.annotationColumnFilter = annotationColumnFilter;
    }

    @SetJIPipeDocumentation(name = "Channel columns", description = "Determines the table columns that are matched to the output slots of this node. " +
            "The numeric value of these columns determine which channel index is put to which output. " +
            "Channel indices are zero-based (0 is the first channel). " +
            "By default, all columns NOT starting with a '#' are selected. Use <code>column_name IN ARRAY(...)</code> to set a predefined list of columns.")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "column_name", name = "Column name", description = "The name of the currently processed column")
    @JIPipeExpressionParameterSettings(hint = "per column")
    @JIPipeParameter(value = "channel-column-filter", uiOrder = -99, important = true)
    public JIPipeExpressionParameter getChannelColumnFilter() {
        return channelColumnFilter;
    }

    @JIPipeParameter("channel-column-filter")
    public void setChannelColumnFilter(JIPipeExpressionParameter channelColumnFilter) {
        this.channelColumnFilter = channelColumnFilter;
    }

    @SetJIPipeDocumentation(name = "Ignore missing channels", description = "If enabled, the algorithm silently skips invalid assignments like extracting the 4th channel of a 2-channel image. " +
            "If disabled, an error will be thrown if such a condition is detected.")
    @JIPipeParameter("ignore-missing-channels")
    public boolean isIgnoreMissingChannels() {
        return ignoreMissingChannels;
    }

    @JIPipeParameter("ignore-missing-channels")
    public void setIgnoreMissingChannels(boolean ignoreMissingChannels) {
        this.ignoreMissingChannels = ignoreMissingChannels;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel index", description = "If enabled, create an annotation that contains the channel index (starting with zero)")
    @JIPipeParameter("channel-index-annotation")
    public OptionalTextAnnotationNameParameter getChannelIndexAnnotation() {
        return channelIndexAnnotation;
    }

    @JIPipeParameter("channel-index-annotation")
    public void setChannelIndexAnnotation(OptionalTextAnnotationNameParameter channelIndexAnnotation) {
        this.channelIndexAnnotation = channelIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel name", description = "If enabled, create an annotation that contains the channel name as defined by the output slot")
    @JIPipeParameter("channel-name-annotation")
    public OptionalTextAnnotationNameParameter getChannelNameAnnotation() {
        return channelNameAnnotation;
    }

    @JIPipeParameter("channel-name-annotation")
    public void setChannelNameAnnotation(OptionalTextAnnotationNameParameter channelNameAnnotation) {
        this.channelNameAnnotation = channelNameAnnotation;
    }
}
