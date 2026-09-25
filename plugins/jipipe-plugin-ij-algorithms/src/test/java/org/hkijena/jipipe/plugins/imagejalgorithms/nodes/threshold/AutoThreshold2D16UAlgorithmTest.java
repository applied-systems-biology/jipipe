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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.ImagePlus;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMutableIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoThreshold2D16UAlgorithmTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * Bimodal 16-bit image (32x32): left half 1000 (dark background), right half 20000 (bright objects).
     */
    private static ImagePlus bimodalImage() {
        ShortProcessor processor = new ShortProcessor(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                processor.set(x, y, x < 16 ? 1000 : 20000);
            }
        }
        return new ImagePlus("test", processor);
    }

    /**
     * Populates the node's Input slot with one image and returns a manually wired iteration step that references row 0.
     */
    private static JIPipeSingleIterationStep wireSingleInput(JIPipeGraphNode node, ImagePlus image) {
        node.getInputSlot("Input").addData(new ImagePlusGreyscale16UData(image), new JIPipeMutableDataContext(), new JIPipeProgressInfo());
        JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(node);
        iterationStep.setInputData(node.getInputSlot("Input"), 0);
        return iterationStep;
    }

    /**
     * Checks the output mask of a threshold node run on the bimodal image:
     * 8-bit output where exactly the bright half (512 pixels) is foreground.
     */
    private static void assertBrightHalfIsForeground(JIPipeGraphNode node) {
        assertEquals(1, node.getOutputSlot("Output").getRowCount());
        ImagePlusGreyscaleMaskData output = node.getOutputSlot("Output").getData(0, ImagePlusGreyscaleMaskData.class, new JIPipeProgressInfo());
        ImagePlus mask = output.getImage();
        assertEquals(8, mask.getBitDepth());
        int foreground = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                if (mask.getProcessor().get(x, y) == 255) foreground++;
            }
        }
        assertEquals(32 * 16, foreground, "Exactly the bright half must be foreground");
        assertEquals(0, mask.getProcessor().get(0, 0), "Dark background must not be foreground");
        assertEquals(255, mask.getProcessor().get(31, 31), "Bright pixels must be foreground");
    }

    /**
     * Bimodal 16-bit image (dark background 1000, bright objects 20000):
     * the 16-bit auto threshold must produce a mask with exactly the bright pixels set.
     */
    @Test
    void testBimodal16BitOtsu() {
        AutoThreshold2D16UAlgorithm node = JIPipe.createNode(AutoThreshold2D16UAlgorithm.class);
        node.setMethod(AutoThresholdMethod.Otsu);
        JIPipeSingleIterationStep iterationStep = wireSingleInput(node, bimodalImage());
        node.runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());
        assertBrightHalfIsForeground(node);
    }

    /**
     * The custom-bins node with 256 bins over the full 16-bit range must separate the same bimodal image:
     * bin width is 256, so the dark mode is in bin ~3 and the bright mode in bin ~78.
     */
    @Test
    void testCustomBinsNode() {
        AutoThreshold2DNBinsAlgorithm node = JIPipe.createNode(AutoThreshold2DNBinsAlgorithm.class);
        node.setMethod(AutoThresholdMethod.Otsu);
        assertTrue(node.setNbins(256), "setNbins(256) must be accepted");
        JIPipeSingleIterationStep iterationStep = wireSingleInput(node, bimodalImage());
        node.runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());
        assertBrightHalfIsForeground(node);
    }

    /**
     * Percentile threshold (default 50) on the bimodal 16-bit image must produce a mask
     * with exactly the bright pixels set, in true 8-bit output.
     */
    @Test
    void testPercentileThreshold16U() {
        PercentileThreshold16U2DAlgorithm node = JIPipe.createNode(PercentileThreshold16U2DAlgorithm.class);
        assertEquals(50, node.getPercentile(), 1e-9, "Default percentile must be 50");
        JIPipeSingleIterationStep iterationStep = wireSingleInput(node, bimodalImage());
        node.runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());
        assertBrightHalfIsForeground(node);
    }
}
