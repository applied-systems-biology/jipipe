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

package org.hkijena.jipipe.plugins.imagejdatatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.OMETIFFCompression;

public class ImageJDataTypesSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:ij-datatypes";
    private boolean useBioFormats = true;
    private boolean saveRGBWithImageJ = true;
    private OMETIFFCompression bioFormatsCompression = OMETIFFCompression.Uncompressed;

    public static ImageJDataTypesSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageJDataTypesSettings.class);
    }

    @SetJIPipeDocumentation(name = "Use Bio-Formats for saving & loading", description = "If enabled, Bio-Formats is used to save and load results. Otherwise the native ImageJ functions are used that " +
            "might have issues with some files.")
    @JIPipeParameter("use-bio-formats")
    public boolean isUseBioFormats() {
        return useBioFormats;
    }

    @JIPipeParameter("use-bio-formats")
    public void setUseBioFormats(boolean useBioFormats) {
        this.useBioFormats = useBioFormats;
    }

    @SetJIPipeDocumentation(name = "Bio-Formats compression", description = "Compression algorithm to use when saving via Bio-Formats.")
    @JIPipeParameter("bio-formats-compression")
    public OMETIFFCompression getBioFormatsCompression() {
        return bioFormatsCompression;
    }

    @JIPipeParameter("bio-formats-compression")
    public void setBioFormatsCompression(OMETIFFCompression bioFormatsCompression) {
        this.bioFormatsCompression = bioFormatsCompression;
    }

    @SetJIPipeDocumentation(name = "Save RGB images with ImageJ", description = "If enabled, RGB images are always saved with ImageJ, even if Bio-Formats is used by default. This can increase performance.")
    @JIPipeParameter("save-rgb-with-imagej")
    public boolean isSaveRGBWithImageJ() {
        return saveRGBWithImageJ;
    }

    @JIPipeParameter("save-rgb-with-imagej")
    public void setSaveRGBWithImageJ(boolean saveRGBWithImageJ) {
        this.saveRGBWithImageJ = saveRGBWithImageJ;
    }
}
