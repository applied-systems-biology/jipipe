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

package org.hkijena.jipipe;

import org.scijava.plugin.PluginInfo;

public class JIPipeJavaExtensionInitializationInfo {
    private JIPipeJavaPlugin instance;
    private PluginInfo<JIPipeJavaPlugin> pluginInfo;
    private boolean loaded;

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public JIPipeJavaPlugin getInstance() {
        return instance;
    }

    public void setInstance(JIPipeJavaPlugin instance) {
        this.instance = instance;
    }

    public PluginInfo<JIPipeJavaPlugin> getPluginInfo() {
        return pluginInfo;
    }

    public void setPluginInfo(PluginInfo<JIPipeJavaPlugin> pluginInfo) {
        this.pluginInfo = pluginInfo;
    }
}
