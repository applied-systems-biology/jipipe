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

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Registry for project settings.
 * Settings are organized in "sheets" (parameter collections)
 */
public class JIPipeProjectSettingsRegistry {

    private final JIPipe jiPipe;
    private final BiMap<String, Class<? extends JIPipeProjectSettingsSheet>> registeredSheetTypes = HashBiMap.create();

    public JIPipeProjectSettingsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }


    /**
     * Registers a new settings sheet
     *
     * @param sheetClass the sheet
     */
    public void register(Class<? extends JIPipeProjectSettingsSheet> sheetClass) {
        // Create a template we use to extract info from
        JIPipeProjectSettingsSheet sheet = (JIPipeProjectSettingsSheet) ReflectionUtils.newInstance(sheetClass);

        if (StringUtils.isNullOrEmpty(sheet.getId())) {
            throw new IllegalArgumentException("Invalid ID for settings sheet " + sheetClass);
        }
        if (StringUtils.isNullOrEmpty(sheet.getIcon())) {
            throw new IllegalArgumentException("Invalid icon for settings sheet " + sheetClass);
        }
        if (StringUtils.isNullOrEmpty(sheet.getCategory()) || sheet.getCategoryIcon() == null) {
            throw new IllegalArgumentException("Invalid category for settings sheet " + sheetClass);
        }
        registeredSheetTypes.put(sheet.getId(), sheetClass);
        getJIPipe().getProgressInfo().log("Registered project settings sheet id=" + sheet.getId() + " in category '" + sheet.getCategory() + "' object=" + sheetClass);
    }

    /**
     * Gets the settings type with given ID
     *
     * @param id the ID
     * @return the settings type.
     */
    public Class<? extends JIPipeProjectSettingsSheet> getById(String id) {
        return registeredSheetTypes.getOrDefault(id, null);
    }


    public BiMap<String, Class<? extends JIPipeProjectSettingsSheet>> getRegisteredSheetTypes() {
        return ImmutableBiMap.copyOf(registeredSheetTypes);
    }


    public JIPipe getJIPipe() {
        return jiPipe;
    }

}
