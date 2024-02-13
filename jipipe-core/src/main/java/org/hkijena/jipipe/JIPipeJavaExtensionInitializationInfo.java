package org.hkijena.jipipe;

import org.scijava.plugin.PluginInfo;

public class JIPipeJavaExtensionInitializationInfo {
    private JIPipeJavaExtension instance;
    private PluginInfo<JIPipeJavaExtension> pluginInfo;
    private boolean loaded;

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public JIPipeJavaExtension getInstance() {
        return instance;
    }

    public void setInstance(JIPipeJavaExtension instance) {
        this.instance = instance;
    }

    public PluginInfo<JIPipeJavaExtension> getPluginInfo() {
        return pluginInfo;
    }

    public void setPluginInfo(PluginInfo<JIPipeJavaExtension> pluginInfo) {
        this.pluginInfo = pluginInfo;
    }
}
