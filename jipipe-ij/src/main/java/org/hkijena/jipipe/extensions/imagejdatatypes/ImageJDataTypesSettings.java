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

package org.hkijena.jipipe.extensions.imagejdatatypes;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMETIFFCompression;

public class ImageJDataTypesSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:ij-datatypes";

    private final EventBus eventBus = new EventBus();
    private boolean useBioFormats = true;
    private OMETIFFCompression bioFormatsCompression = OMETIFFCompression.Uncompressed;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Use Bio-Formats for saving & loading", description = "If enabled, Bio-Formats is used to save and load results. Otherwise the native ImageJ functions are used that " +
            "might have issues with some files.")
    @JIPipeParameter("use-bio-formats")
    public boolean isUseBioFormats() {
        return useBioFormats;
    }

    @JIPipeParameter("use-bio-formats")
    public void setUseBioFormats(boolean useBioFormats) {
        this.useBioFormats = useBioFormats;
    }

    @JIPipeDocumentation(name = "Bio-Formats compression", description = "Compression algorithm to use when saving via Bio-Formats.")
    @JIPipeParameter("bio-formats-compression")
    public OMETIFFCompression getBioFormatsCompression() {
        return bioFormatsCompression;
    }

    @JIPipeParameter("bio-formats-compression")
    public void setBioFormatsCompression(OMETIFFCompression bioFormatsCompression) {
        this.bioFormatsCompression = bioFormatsCompression;
    }


    public static ImageJDataTypesSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageJDataTypesSettings.class);
    }
}
