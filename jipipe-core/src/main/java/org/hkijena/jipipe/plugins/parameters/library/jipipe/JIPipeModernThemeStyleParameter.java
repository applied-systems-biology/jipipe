package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import org.hkijena.jipipe.plugins.parameters.library.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.utils.ThemeUtils;

public class JIPipeModernThemeStyleParameter extends DynamicStringEnumParameter {

    public JIPipeModernThemeStyleParameter() {
        initializeAllowedValues();
    }

    public JIPipeModernThemeStyleParameter(DynamicStringEnumParameter other) {
        super(other);
    }

    public JIPipeModernThemeStyleParameter(String value) {
        super(value);
        initializeAllowedValues();
    }

    private void initializeAllowedValues() {
        setAllowedValues(ThemeUtils.getAvailableStyleIds());
    }
}
