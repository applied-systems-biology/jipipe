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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels.filter;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TByteByteMap;
import gnu.trove.map.TFloatFloatMap;
import gnu.trove.map.TShortShortMap;
import gnu.trove.map.hash.TByteByteHashMap;
import gnu.trove.map.hash.TFloatFloatHashMap;
import gnu.trove.map.hash.TShortShortHashMap;
import gnu.trove.set.TFloatSet;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Filter labels by thickness 2D", description = "Filter labels by their thickness as estimated by a EDT per label")
@ConfigureJIPipeNode(menuPath = "Labels\nFilter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class FilterLabelsByThicknessAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("thickness > 10");
    private boolean excludeZero = true;

    private float deletedLabel = 0;

    public FilterLabelsByThicknessAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByThicknessAlgorithm(FilterLabelsByThicknessAlgorithm other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.excludeZero = other.excludeZero;
        this.deletedLabel = other.deletedLabel;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        if (image.getBitDepth() == 8) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {

                // Collect existing labels
                byte[] pixels = (byte[]) ip.getPixels();
                TIntSet existingLabels = new TIntHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    existingLabels.add(Byte.toUnsignedInt(pixels[i]));
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TIntList existingLabelsList = new TIntArrayList(existingLabels.toArray());

                // Mapping
                TByteByteMap mapping = new TByteByteHashMap();

                // Go through each label
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < existingLabelsList.size(); labelIndex++) {

                    final int targetLabel = existingLabelsList.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / existingLabelsList.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (Byte.toUnsignedInt(pixels[i]) == targetLabel) {
                            tempPixels[i] = (byte) 255;
                        } else {
                            tempPixels[i] = 0;
                        }
                    }

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    byte deletedLabel_ = (byte) deletedLabel;
                    double thickness = edtProcessor.getStats().max * 2;
                    variables.set("thickness", thickness);
                    if (filter.test(variables)) {
                        // Remap
                        mapping.put((byte) targetLabel, (byte) targetLabel);
                    } else {
                        mapping.put((byte) targetLabel, deletedLabel_);
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else if (image.getBitDepth() == 16) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {
                // Collect existing labels
                short[] pixels = (short[]) ip.getPixels();
                TIntSet existingLabels = new TIntHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    existingLabels.add(Short.toUnsignedInt(pixels[i]));
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TIntList existingLabelsList = new TIntArrayList(existingLabels.toArray());

                // Mapping
                TShortShortMap mapping = new TShortShortHashMap();

                // Go through each label
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < existingLabelsList.size(); labelIndex++) {

                    final int targetLabel = existingLabelsList.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / existingLabelsList.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (Short.toUnsignedInt(pixels[i]) == targetLabel) {
                            tempPixels[i] = (byte) 255;
                        } else {
                            tempPixels[i] = 0;
                        }
                    }

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    short deletedLabel_ = (short) deletedLabel;
                    double thickness = edtProcessor.getStats().max * 2;
                    variables.set("thickness", thickness);
                    if (filter.test(variables)) {
                        // Remap
                        mapping.put((short) targetLabel, (short) targetLabel);
                    } else {
                        mapping.put((short) targetLabel, deletedLabel_);
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else if (image.getBitDepth() == 32) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {
                // Collect existing labels
                float[] pixels = (float[]) ip.getPixels();
                TFloatSet existingLabels = new TFloatHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    existingLabels.add(pixels[i]);
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TFloatList existingLabelsList = new TFloatArrayList(existingLabels.toArray());

                // Mapping
                TFloatFloatMap mapping = new TFloatFloatHashMap();

                // Go through each label
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < existingLabelsList.size(); labelIndex++) {

                    final float targetLabel = existingLabelsList.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / existingLabelsList.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (pixels[i] == targetLabel) {
                            tempPixels[i] = (byte) 255;
                        } else {
                            tempPixels[i] = 0;
                        }
                    }

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    double thickness = edtProcessor.getStats().max * 2;
                    variables.set("thickness", thickness);
                    if (filter.test(variables)) {
                        // Remap
                        mapping.put(targetLabel, targetLabel);
                    } else {
                        mapping.put(targetLabel, deletedLabel);
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else {
            throw new UnsupportedOperationException("Unsupported bit depth: " + image.getBitDepth());
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Determines whether a label is kept or removed")
    @JIPipeParameter("filter")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @SetJIPipeDocumentation(name = "Exclude zero", description = "If enabled, do not apply the algorithm to the zero label.")
    @JIPipeParameter("exclude-zero")
    public boolean isExcludeZero() {
        return excludeZero;
    }

    @JIPipeParameter("exclude-zero")
    public void setExcludeZero(boolean excludeZero) {
        this.excludeZero = excludeZero;
    }

    @SetJIPipeDocumentation(name = "Deleted label value", description = "Value assigned to labels that are deleted")
    @JIPipeParameter("deleted-labels")
    public float getDeletedLabel() {
        return deletedLabel;
    }

    @JIPipeParameter("deleted-labels")
    public void setDeletedLabel(float deletedLabel) {
        this.deletedLabel = deletedLabel;
    }
}
