package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels.filter;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Keep/Delete labels by overlap", description = "Deletes or keeps labels by a mask image. If the mask is white (value larger than zero) " +
        "on overlapping a pixel, the associated label is kept or deleted (depending on the setting)")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class FilterLabelsByMaskAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean deleteMaskedLabels = false;

    public FilterLabelsByMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByMaskAlgorithm(FilterLabelsByMaskAlgorithm other) {
        super(other);
        this.deleteMaskedLabels = other.deleteMaskedLabels;
    }

    @JIPipeDocumentation(name = "Operation on masked labels", description = "Labels that overlap with a masked pixel (value larger than zero) can be either kept or deleted.")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labelImage = dataBatch.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus maskImage = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        if (!ImageJUtils.imagesHaveSameSize(labelImage, maskImage)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        TIntSet labelsToKeep = new TIntHashSet();
        ImageJUtils.forEachIndexedZCTSlice(labelImage, (labelProcessor, index) -> {
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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(labelImage), progressInfo);
    }
}
