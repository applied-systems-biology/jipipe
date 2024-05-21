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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Map;

/**
 * Settings for default data exporters
 */
public class JIPipeDataExporterApplicationSettings extends JIPipeDataByMetadataExporter implements JIPipeApplicationSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:legacy-data-exporter";

    public static JIPipeDataExporterApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeDataExporterApplicationSettings.class);
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

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/document-export.png");
    }

    @Override
    public String getName() {
        return "Data export";
    }

    @Override
    public String getCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Data.getCategory();
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Data.getIcon();
    }

    @Override
    public String getDescription() {
        return "Default settings for data exporters";
    }
}
