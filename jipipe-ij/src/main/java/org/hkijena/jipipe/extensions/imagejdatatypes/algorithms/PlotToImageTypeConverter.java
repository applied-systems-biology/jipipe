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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;

import java.awt.image.BufferedImage;

public class PlotToImageTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return PlotData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ImagePlus2DColorRGBData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        PlotData plotData = (PlotData)input;
        BufferedImage bufferedImage = plotData.getChart().createBufferedImage(plotData.getExportWidth(), plotData.getExportHeight());
        return new ImagePlus2DColorRGBData(new ImagePlus("Plot", bufferedImage));
    }
}
