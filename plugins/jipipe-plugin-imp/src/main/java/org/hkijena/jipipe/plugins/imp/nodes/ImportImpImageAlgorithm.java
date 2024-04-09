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

package org.hkijena.jipipe.plugins.imp.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Import IMP image", description = "Imports an image via the Image Manipulation Pipeline.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "File", create = true, description = "The file to be imported")
@AddJIPipeOutputSlot(value = ImpImageData.class, slotName = "Image", create = true, description = "The image")
public class ImportImpImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean greyscaleCorrection = true;

    public ImportImpImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportImpImageAlgorithm(ImportImpImageAlgorithm other) {
        super(other);
        this.greyscaleCorrection = other.greyscaleCorrection;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path file = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        BufferedImage image = BufferedImageUtils.read(file, greyscaleCorrection);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImpImageData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Apply greyscale correction", description = "If enabled, convert greyscale images to RGBA to prevent washed-out colors")
    @JIPipeParameter("greyscale-correction")
    public boolean isGreyscaleCorrection() {
        return greyscaleCorrection;
    }

    @JIPipeParameter("greyscale-correction")
    public void setGreyscaleCorrection(boolean greyscaleCorrection) {
        this.greyscaleCorrection = greyscaleCorrection;
    }
}
