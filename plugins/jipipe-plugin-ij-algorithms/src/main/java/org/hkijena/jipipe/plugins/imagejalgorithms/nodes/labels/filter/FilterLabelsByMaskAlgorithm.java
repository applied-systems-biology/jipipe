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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels.filter;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;

@SetJIPipeDocumentation(name = "Keep/Delete labels by overlap", description = "Deletes or keeps labels by a mask image. If the mask is white (value larger than zero) " +
        "on overlapping a pixel, the associated label is kept or deleted (depending on the setting)")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class FilterLabelsByMaskAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean deleteMaskedLabels = false;

    public FilterLabelsByMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByMaskAlgorithm(FilterLabelsByMaskAlgorithm other) {
        super(other);
        this.deleteMaskedLabels = other.deleteMaskedLabels;
    }

    @SetJIPipeDocumentation(name = "Operation on masked labels", description = "Labels that overlap with a masked pixel (value larger than zero) can be either kept or deleted.")
    @JIPipeParameter("delete-masked-labels")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Delete labels (keep others)", falseLabel = "Keep labels (delete others)")
    public boolean isDeleteMaskedLabels() {
        return deleteMaskedLabels;
    }

    @JIPipeParameter("delete-masked-labels")
    public void setDeleteMaskedLabels(boolean deleteMaskedLabels) {
        this.deleteMaskedLabels = deleteMaskedLabels;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus labelImage = iterationStep.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus maskImage = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        if (!ImageJUtils.imagesHaveSameSize(labelImage, maskImage)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        TIntSet labelsToKeep = new TIntHashSet();
        ImageJIterationUtils.forEachIndexedZCTSlice(labelImage, (labelProcessor, index) -> {
            labelsToKeep.clear();
            int z = Math.min(index.getZ(), maskImage.getNSlices() - 1);
            int c = Math.min(index.getC(), maskImage.getNChannels() - 1);
            int t = Math.min(index.getT(), maskImage.getNFrames() - 1);
            ImageProcessor maskProcessor = ImageJUtils.getSliceZero(maskImage, c, z, t);
            byte[] maskBytes = (byte[]) maskProcessor.getPixels();

            if (labelProcessor instanceof ByteProcessor) {
                byte[] labelBytes = (byte[]) labelProcessor.getPixels();
                for (int i = 0; i < maskBytes.length; i++) {
                    if ((maskBytes[i] & 0xff) > 0) {
                        labelsToKeep.add(Byte.toUnsignedInt(labelBytes[i]));
                    }
                }
            } else if (labelProcessor instanceof ShortProcessor) {
                short[] labelBytes = (short[]) labelProcessor.getPixels();
                for (int i = 0; i < maskBytes.length; i++) {
                    if ((maskBytes[i] & 0xff) > 0) {
                        labelsToKeep.add(Short.toUnsignedInt(labelBytes[i]));
                    }
                }
            } else if (labelProcessor instanceof FloatProcessor) {
                float[] labelBytes = (float[]) labelProcessor.getPixels();
                for (int i = 0; i < maskBytes.length; i++) {
                    if ((maskBytes[i] & 0xff) > 0) {
                        labelsToKeep.add((int) labelBytes[i]);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported label type!");
            }

            if (deleteMaskedLabels)
                LabelImages.replaceLabels(labelProcessor, labelsToKeep.toArray(), 0);
            else {
                ImageJAlgorithmUtils.removeLabelsExcept(labelProcessor, labelsToKeep.toArray());
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(labelImage), progressInfo);
    }
}
