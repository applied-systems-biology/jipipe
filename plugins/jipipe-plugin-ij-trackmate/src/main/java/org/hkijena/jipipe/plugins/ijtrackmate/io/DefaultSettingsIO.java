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

package org.hkijena.jipipe.plugins.ijtrackmate.io;

public class DefaultSettingsIO implements SettingsIO {

    private final Class<?> fieldType;

    public DefaultSettingsIO(Class<?> fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public Object settingToParameter(Object obj) {
        return obj;
    }

    @Override
    public Object parameterToSetting(Object obj) {
        return obj;
    }

    @Override
    public Class<?> getSettingClass() {
        return fieldType;
    }

    @Override
    public Class<?> getParameterClass() {
        return fieldType;
    }
}
