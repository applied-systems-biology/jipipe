package org.hkijena.jipipe.plugins.automation.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application settings for the shared MCP broker and the per-instance automation endpoint.
 * The permission map persists the user-chosen {@link AutomationPermissionState} per
 * operation id (state name); the permission table UI is provided by the automation UI.
 */
public class MCPApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:mcp";

    private boolean enable = false;
    private int port = 8781;
    private boolean autoStart = false;
    private boolean requireToken = false;
    private Map<String, String> permissions = new LinkedHashMap<>();

    public static MCPApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, MCPApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable MCP", description = "Enable the MCP automation layer (shared broker and instance endpoint).")
    @JIPipeParameter("enable")
    public boolean isEnable() {
        return enable;
    }

    @JIPipeParameter("enable")
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @SetJIPipeDocumentation(name = "Port", description = "Port of the shared MCP broker (default: 8781). An occupied port falls back to a free one.")
    @JIPipeParameter("port")
    public int getPort() {
        return port;
    }

    @JIPipeParameter("port")
    public void setPort(int port) {
        this.port = port;
    }

    @SetJIPipeDocumentation(name = "Auto-start", description = "Connect to (or spawn) the MCP broker when JIPipe launches, without the --mcp CLI flag.")
    @JIPipeParameter("auto-start")
    public boolean isAutoStart() {
        return autoStart;
    }

    @JIPipeParameter("auto-start")
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @SetJIPipeDocumentation(name = "Require token", description = "Require a bearer token for the MCP broker control and MCP endpoints.")
    @JIPipeParameter("require-token")
    public boolean isRequireToken() {
        return requireToken;
    }

    @JIPipeParameter("require-token")
    public void setRequireToken(boolean requireToken) {
        this.requireToken = requireToken;
    }

    /**
     * Persisted permission states per operation id (operation id to
     * {@link AutomationPermissionState} name).
     * <p>
     * Hidden from the generated settings UI: no generic editor exists for maps, and the
     * dedicated permission table UI (automation UI) reads and writes this map.
     */
    @SetJIPipeDocumentation(name = "Permissions", description = "User-chosen permission state per automation operation (operation id to state name). Edited in the automation permission table.")
    @JIPipeParameter(value = "permissions", hidden = true)
    public Map<String, String> getPermissions() {
        return permissions;
    }

    @JIPipeParameter(value = "permissions", hidden = true)
    public void setPermissions(Map<String, String> permissions) {
        this.permissions = permissions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(permissions);
    }

    /**
     * Returns the persisted permission state of one operation, or null when unset.
     */
    public AutomationPermissionState getPermissionState(String operationId) {
        String name = permissions.get(operationId);
        if (name == null) {
            return null;
        }
        try {
            return AutomationPermissionState.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Persists the permission state of one operation (null clears it).
     */
    public void setPermissionState(String operationId, AutomationPermissionState state) {
        if (state == null) {
            permissions.remove(operationId);
        } else {
            permissions.put(operationId, state.name());
        }
    }

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
        return "MCP server";
    }

    @Override
    public String getDescription() {
        return "Settings for the MCP automation layer (shared broker and instance endpoint)";
    }
}
