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

package org.hkijena.jipipe.api.validation;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings that are passed to the validation report functions
 */
public class JIPipeValidationReportSettings {

    public static final JIPipeValidationReportSettings DEFAULT = new JIPipeValidationReportSettings();
    public static final JIPipeValidationReportSettings STRICT = builder().withStrict(true).build();
    private final Map<String, Boolean> additionalFlags = new HashMap<>();
    private boolean strict;

    private JIPipeValidationReportSettings() {

    }

    public static JIPipeValidationReportSettingsBuilder builder() {
        return new JIPipeValidationReportSettingsBuilder();
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean getAdditionalFlag(String key) {
        return additionalFlags.getOrDefault(key, false);
    }

    public boolean getAdditionalFlag(String key, boolean defaultValue) {
        return additionalFlags.getOrDefault(key, defaultValue);
    }

    public static final class JIPipeValidationReportSettingsBuilder {
        private final JIPipeValidationReportSettings instance = new JIPipeValidationReportSettings();

        private JIPipeValidationReportSettingsBuilder() {
        }

        public JIPipeValidationReportSettingsBuilder withStrict(boolean strict) {
            instance.strict = strict;
            return this;
        }

        public JIPipeValidationReportSettingsBuilder withFlag(String key, boolean value) {
            instance.additionalFlags.put(key, value);
            return this;
        }

        public JIPipeValidationReportSettings build() {
            return instance;
        }
    }
}
