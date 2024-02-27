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

package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.Map;

/**
 * Settings for default data exporters
 */
public class DataExporterSettings extends JIPipeDataByMetadataExporter {
    public static final String ID = "data-exporter";

    public static DataExporterSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DataExporterSettings.class);
    }

    /**
     * Copies parameters into the settings storage
     *
     * @param exporter the exporter
     */
    public void copyFrom(JIPipeDataByMetadataExporter exporter) {
        JIPipeParameterTree source = new JIPipeParameterTree(exporter);
        JIPipeParameterTree target = new JIPipeParameterTree(this);
        for (Map.Entry<String, JIPipeParameterAccess> entry : source.getParameters().entrySet()) {
            target.getParameters().get(entry.getKey()).set(entry.getValue().get(Object.class));
        }
    }
}
