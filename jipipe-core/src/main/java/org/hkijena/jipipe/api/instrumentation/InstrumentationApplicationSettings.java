package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;

import javax.swing.*;

/**
 * Application settings for the instrumentation server.
 */
public class InstrumentationApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:instrumentation";

    private boolean enableInstrumentation = false;
    private int port = 8780;
    private boolean autoStart = false;

    public static InstrumentationApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, InstrumentationApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable instrumentation", description = "Enable the instrumentation WebSocket server for external automation and diagnostics tools.")
    @JIPipeParameter("enable-instrumentation")
    public boolean isEnableInstrumentation() { return enableInstrumentation; }

    @JIPipeParameter("enable-instrumentation")
    public void setEnableInstrumentation(boolean enableInstrumentation) { this.enableInstrumentation = enableInstrumentation; }

    @SetJIPipeDocumentation(name = "Port", description = "WebSocket port for the instrumentation server (default: 8780)")
    @JIPipeParameter("port")
    public int getPort() { return port; }

    @JIPipeParameter("port")
    public void setPort(int port) { this.port = port; }

    @SetJIPipeDocumentation(name = "Auto-start", description = "Start the instrumentation server when JIPipe launches")
    @JIPipeParameter("auto-start")
    public boolean isAutoStart() { return autoStart; }

    @JIPipeParameter("auto-start")
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/wrench.png");
    }

    @Override
    public String getName() {
        return "Instrumentation";
    }

    @Override
    public String getDescription() {
        return "Settings for the instrumentation WebSocket server";
    }
}
