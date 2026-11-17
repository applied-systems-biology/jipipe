package org.hkijena.jipipe.plugins.automation.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;

import javax.swing.*;

/**
 * Application settings for the shared MCP broker and the per-instance automation endpoint.
 * The permission map persists the user-chosen {@link AutomationPermissionState} per
 * operation id (state name); it is edited by the per-tool permission table
 * ({@code JIPipeDesktopMCPPermissionsEditorUI}).
 * <p>
 * The {@code enable} parameter is applied live: toggling it starts or stops the MCP
 * automation layer while the app runs (start is a no-op when the layer already runs;
 * stop uses pause semantics, so the broker stays alive for other instances), and a
 * launch with {@code enable} set starts the layer without the {@code --mcp} flag. The
 * lifecycle runs in the GUI launcher, which subscribes to this sheet's parameter change
 * events for the {@code enable} key.
 * <p>
 * The {@code requireToken} parameter toggles token authentication: when enabled, the
 * broker client generates the shared token file {@code <JIPipe user dir>/mcp/token} once
 * (secure random 32-byte base64url secret, owner-only permissions, POSIX 0600), passes it
 * to spawned brokers, and presents it as a bearer token on client-to-broker calls. The
 * file persists until manually deleted: it is never regenerated, so the token survives
 * broker restarts and is shared by all JIPipe instances of the same user directory.
 * <p>
 * The whole MCP layer is gated behind the AI feature toggle (the {@code enable-ai}
 * parameter of {@code AIApplicationSettings}, sheet {@code org.hkijena.jipipe:ai}):
 * while the AI feature is disabled, no MCP layer is started or resumed. The gate lives
 * in the GUI launcher's start paths, so it covers the {@code --mcp} flag, auto-start,
 * the {@code enable} setting, the live {@code enable} and {@code enable-ai} toggles, and
 * the AI monitor's start button. With AI off the user gets no AI and no MCP at all.
 */
public class MCPApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:mcp";

    private boolean enable = false;
    private int port = 8781;
    private boolean autoStart = false;
    private boolean requireToken = false;
    private MCPPermissionStates permissions = new MCPPermissionStates();

    public static MCPApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, MCPApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable MCP", description = "Enable the MCP automation layer (shared broker and instance endpoint). Applied live: the layer starts or stops while the app runs, and a launch with this setting enabled starts the layer without the --mcp flag. Requires the AI feature: while 'Enable AI functionality' (AI settings) is disabled, MCP stays off regardless of this setting.")
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

    @SetJIPipeDocumentation(name = "Auto-start", description = "Connect to (or spawn) the MCP broker when JIPipe launches, without the --mcp CLI flag. Launch-time switch only: the running layer is controlled by the 'Enable MCP' setting and the AI monitor.")
    @JIPipeParameter("auto-start")
    public boolean isAutoStart() {
        return autoStart;
    }

    @JIPipeParameter("auto-start")
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @SetJIPipeDocumentation(name = "Require token", description = "Require a bearer token for the MCP broker control and MCP endpoints. The shared token is generated once at '<JIPipe user dir>/mcp/token' (owner-only permissions) and persists until the file is manually deleted.")
    @JIPipeParameter("require-token")
    public boolean isRequireToken() {
        return requireToken;
    }

    @JIPipeParameter("require-token")
    public void setRequireToken(boolean requireToken) {
        this.requireToken = requireToken;
    }

    /**
     * Persisted permission states per operation id. Edited by the dedicated permission
     * table editor (registered for {@link MCPPermissionStates}); an edit writes a copy
     * through the parameter access, so the change events and the settings bridge fire.
     */
    @SetJIPipeDocumentation(name = "Permissions", description = "User-chosen permission state per automation operation (operation id to state name). Edited in the permission table below.")
    @JIPipeParameter("permissions")
    public MCPPermissionStates getPermissions() {
        return permissions;
    }

    @JIPipeParameter("permissions")
    public void setPermissions(MCPPermissionStates permissions) {
        this.permissions = permissions == null ? new MCPPermissionStates() : permissions;
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
     * Note: this bypasses the parameter access, so no parameter change event fires and
     * the settings document is not updated; the settings table writes a copy through
     * {@link #setPermissions(MCPPermissionStates)} instead.
     */
    public void setPermissionState(String operationId, AutomationPermissionState state) {
        permissions.set(operationId, state == null ? null : state.name());
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
