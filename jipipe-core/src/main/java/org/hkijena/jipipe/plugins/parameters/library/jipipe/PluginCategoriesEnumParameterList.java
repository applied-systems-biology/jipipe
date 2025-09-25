package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

import java.util.Arrays;

public class PluginCategoriesEnumParameterList extends JIPipeListParameter<PluginCategoriesEnumParameter> {
    public PluginCategoriesEnumParameterList() {
        super(PluginCategoriesEnumParameter.class);
    }

    public PluginCategoriesEnumParameterList(PluginCategoriesEnumParameter... items) {
        this();
        this.addAll(Arrays.asList(items));
    }

    public PluginCategoriesEnumParameterList(String... items) {
        this();
        for (String item : items) {
            add(new PluginCategoriesEnumParameter(item));
        }
    }

    public PluginCategoriesEnumParameterList(PluginCategoriesEnumParameterList other) {
        super(PluginCategoriesEnumParameter.class);
        for (PluginCategoriesEnumParameter parameter : other) {
            add(new PluginCategoriesEnumParameter(parameter));
        }
    }
}
