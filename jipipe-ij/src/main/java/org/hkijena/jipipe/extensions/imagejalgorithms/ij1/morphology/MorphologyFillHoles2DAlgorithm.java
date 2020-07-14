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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.Prefs;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Morphological hole filling 2D", description = "Applies a morphological hole filling operation to binary images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Morphology", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class MorphologyFillHoles2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public MorphologyFillHoles2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleMaskData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MorphologyFillHoles2DAlgorithm(MorphologyFillHoles2DAlgorithm other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ip -> {
            int fg = Prefs.blackBackground ? 255 : 0;
            int foreground = ip.isInvertedLut() ? 255 - fg : fg;
            int background = 255 - foreground;
            fill(ip, foreground, background);
        });
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img));
    }

    /**
     * Binary fill by Gabriel Landini, G.Landini at bham.ac.uk
     * 21/May/2008
     *
     * @param ip         Image processor
     * @param foreground Foreground value
     * @param background Background value
     */
    void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y = 0; y < height; y++) {
            if (ip.getPixel(0, y) == background) ff.fill(0, y);
            if (ip.getPixel(width - 1, y) == background) ff.fill(width - 1, y);
        }
        for (int x = 0; x < width; x++) {
            if (ip.getPixel(x, 0) == background) ff.fill(x, 0);
            if (ip.getPixel(x, height - 1) == background) ff.fill(x, height - 1);
        }
        byte[] pixels = (byte[]) ip.getPixels();
        int n = width * height;
        for (int i = 0; i < n; i++) {
            if (pixels[i] == 127)
                pixels[i] = (byte) background;
            else
                pixels[i] = (byte) foreground;
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
