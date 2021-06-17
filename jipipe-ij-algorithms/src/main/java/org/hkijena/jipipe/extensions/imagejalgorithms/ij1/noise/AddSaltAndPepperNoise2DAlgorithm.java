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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise;

import ij.ImagePlus;
import ij.plugin.filter.SaltAndPepper;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Add salt and pepper noise 2D", description = "Adds salt and pepper noise to the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Noise", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Output", autoCreate = true)
public class AddSaltAndPepperNoise2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double percent = 5;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public AddSaltAndPepperNoise2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public AddSaltAndPepperNoise2DAlgorithm(AddSaltAndPepperNoise2DAlgorithm other) {
        super(other);
        this.percent = other.percent;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        SaltAndPepper saltAndPepper = new SaltAndPepper();
        ImageJUtils.forEachSlice(img, ip -> saltAndPepper.add(ip, percent / 100.0), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Percentage").checkIfWithin(this, percent, 0, 100, true, true);
    }

    @JIPipeDocumentation(name = "Percentage", description = "Percentage of pixels that will be replaced by noise")
    @JIPipeParameter("percent")
    public double getPercent() {
        return percent;
    }

    @JIPipeParameter("percent")
    public void setPercent(double percent) {
        this.percent = percent;

    }
}
