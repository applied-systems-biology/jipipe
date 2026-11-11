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

package org.hkijena.jipipe.plugins.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopMapState;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * The typed keys of the settings that consumers read and write directly
 * (the <em>registry</em> constants: former {@code registry:<db>} entries of
 * the legacy settings facade).
 * <p>
 * <b>Locations are frozen.</b> Every key addresses exactly the location the
 * legacy {@code getFromRegistry}/{@code putIntoRegistry} calls used
 * ({@code registry:<databaseKey>} plus the slash-joined registry key), so
 * existing profiles keep their values. Never re-target a constant here.
 * <p>
 * <b>Lifecycle: every binding has an owner that closes it.</b> A binding
 * registers a path listener on its document, so binding per action leaks one
 * listener per user action. The rules are:
 * <ul>
 * <li>A consumer that <em>lives</em> a session (the application itself, a
 * singleton service) may keep its binding until the settings service's
 * {@code dispose()} — that is the one acceptable "closed at exit" case.</li>
 * <li>A consumer with a lifetime (an editor, a dialog, a browser panel) binds
 * once, reuses the binding, and closes it when the consumer dies (its
 * {@code dispose()}). Closing is idempotent, and the service's own close at
 * exit then is a no-op.</li>
 * <li>A <em>per-call</em> consumer (a static helper, a menu action) must not
 * bind at all: use the leak-free registry delegates of
 * {@code JIPipe.getSettings()} instead. {@code NODE_BROWSER_KINDS} is
 * documented here for a future consumer that can own a binding; today's
 * {@code NodeEntryKind.loadKinds()}/{@code saveKinds()} deliberately use the
 * delegates.</li>
 * </ul>
 * Binding a key twice (two editors of the same type) is allowed: each binding
 * has its own listeners and subscribers.
 * <p>
 * Bind only while the service exists ({@code JIPipe.isInstantiated()}):
 * {@code bind()} materializes the scope document.
 */
public final class SettingsKeys {

    // ------------------------------------------------------------------
    // Graph editor UI (registry DB "ui-graph-editor")
    // ------------------------------------------------------------------

    /**
     * Whether the graph editor dock panels show toolbar labels.
     */
    public static final JIPipeSettingKey<Boolean> SHOW_DOCK_TOOLBAR_LABELS =
            JIPipeSettingKey.bool("registry:ui-graph-editor", "any/dock/show-labels", Boolean.class)
                    .defaultValue(true)
                    .scope(SettingsScope.APPLICATION)
                    .name("Show dock toolbar labels")
                    .description("Show text labels on the graph editor's dock panel toolbars");

    /**
     * The pipeline graph editor's dock layout.
     */
    public static final JIPipeSettingKey<JIPipeDesktopDockPanel.State> PIPELINE_DOCK_STATE =
            JIPipeSettingKey.object("registry:ui-graph-editor", "pipeline/dock-state", JIPipeDesktopDockPanel.State.class)
                    .defaultValue(defaultDockState())
                    .scope(SettingsScope.APPLICATION)
                    .name("Pipeline editor dock layout")
                    .description("The dock panel layout of the pipeline graph editor");

    /**
     * The compartments graph editor's dock layout.
     */
    public static final JIPipeSettingKey<JIPipeDesktopDockPanel.State> COMPARTMENTS_DOCK_STATE =
            JIPipeSettingKey.object("registry:ui-graph-editor", "compartments/dock-state", JIPipeDesktopDockPanel.State.class)
                    .defaultValue(defaultDockState())
                    .scope(SettingsScope.APPLICATION)
                    .name("Compartments editor dock layout")
                    .description("The dock panel layout of the compartments graph editor");

    /**
     * The pipeline graph editor's floating map.
     */
    public static final JIPipeSettingKey<JIPipeDesktopMapState> PIPELINE_MAP_STATE =
            JIPipeSettingKey.object("registry:ui-graph-editor", "pipeline/map-state", JIPipeDesktopMapState.class)
                    .defaultValue(JIPipeDesktopMapState.defaultState())
                    .scope(SettingsScope.APPLICATION)
                    .name("Pipeline editor floating map")
                    .description("The floating map of the pipeline graph editor");

    /**
     * The compartments graph editor's floating map.
     */
    public static final JIPipeSettingKey<JIPipeDesktopMapState> COMPARTMENTS_MAP_STATE =
            JIPipeSettingKey.object("registry:ui-graph-editor", "compartments/map-state", JIPipeDesktopMapState.class)
                    .defaultValue(JIPipeDesktopMapState.defaultState())
                    .scope(SettingsScope.APPLICATION)
                    .name("Compartments editor floating map")
                    .description("The floating map of the compartments graph editor");

    // ------------------------------------------------------------------
    // Dock layouts (registry DB "ui-dock")
    // ------------------------------------------------------------------

    /**
     * The user's saveable dock layout templates, shared by both graph
     * editors.
     */
    public static final JIPipeSettingKey<StringAndStringPairParameterList> DOCK_LAYOUT_TEMPLATES =
            JIPipeSettingKey.object("registry:ui-dock", "layouts", StringAndStringPairParameterList.class)
                    .defaultValue(new StringAndStringPairParameterList())
                    .scope(SettingsScope.APPLICATION)
                    .name("Dock layout templates")
                    .description("Saved dock panel layouts offered by the graph editors' layout menu");

    // ------------------------------------------------------------------
    // Node database (registry DB "node-db")
    // ------------------------------------------------------------------

    /**
     * The ids of the node-database entries pinned in the node browser.
     */
    public static final JIPipeSettingKey<List<String>> PINNED_NODE_IDS =
            JIPipeSettingKey.<List<String>>object("registry:node-db", "pinned-node-ids", (Class<List<String>>) (Class<?>) List.class)
                    .defaultValue(new ArrayList<>())
                    .scope(SettingsScope.APPLICATION)
                    .name("Pinned node entries")
                    .description("Node browser entries that are pinned to the top of the list");

    /**
     * The selected {@link org.hkijena.jipipe.api.nodes.database.entries.NodeEntryKind}
     * names of the node browser / node finder kind filter (empty = show all).
     */
    public static final JIPipeSettingKey<List<String>> NODE_BROWSER_KINDS =
            JIPipeSettingKey.<List<String>>object("registry:node-db", "node-browser-kinds", (Class<List<String>>) (Class<?>) List.class)
                    .defaultValue(new ArrayList<>())
                    .scope(SettingsScope.APPLICATION)
                    .name("Node browser kinds")
                    .description("The node kinds shown by the node browser and node finder (empty = all)");
    // NOTE: NODE_BROWSER_KINDS is currently read/written by the static
    // NodeEntryKind.loadKinds()/saveKinds(), which must not bind (per-call).
    // It is declared here to document the frozen location for a consumer that
    // can own and close a binding.

    // ------------------------------------------------------------------
    // Color palette (registry DB "ui-palette")
    // ------------------------------------------------------------------

    /**
     * The user's saved palette colors for the foreground-only palette
     * (alpha selection disabled).
     * <p>
     * <b>Stored as a raw JSON node.</b> The list elements are
     * {@link org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteColor}
     * values, and a {@code JIPipeSettingKey} takes a single {@code Class}:
     * {@code List.class} would hand out {@code List<LinkedHashMap>} elements,
     * which do <em>not</em> re-serialize to the stored shape. The JSON node is
     * the exact stored value (byte-identical to the legacy
     * {@code getListFromRegistry}/{@code putIntoRegistry} shape), and the
     * consumer converts it with the typed list reader it always used.
     */
    public static final JIPipeSettingKey<JsonNode> USER_PALETTE_COLORS_FG =
            JIPipeSettingKey.object("registry:ui-palette", "user-palette-colors/fg", JsonNode.class)
                    .scope(SettingsScope.APPLICATION)
                    .name("User palette colors")
                    .description("Colors the user saved in the foreground color palette");

    /**
     * The user's saved palette colors for the foreground palette with alpha
     * selection (see {@link #USER_PALETTE_COLORS_FG}).
     */
    public static final JIPipeSettingKey<JsonNode> USER_PALETTE_COLORS_FG_A =
            JIPipeSettingKey.object("registry:ui-palette", "user-palette-colors/fg-a", JsonNode.class)
                    .scope(SettingsScope.APPLICATION)
                    .name("User palette colors (with alpha)")
                    .description("Colors the user saved in the foreground color palette with alpha selection");

    /**
     * The user's saved palette colors for the foreground/background palette
     * (see {@link #USER_PALETTE_COLORS_FG}).
     */
    public static final JIPipeSettingKey<JsonNode> USER_PALETTE_COLORS_FG_BG =
            JIPipeSettingKey.object("registry:ui-palette", "user-palette-colors/fg-bg", JsonNode.class)
                    .scope(SettingsScope.APPLICATION)
                    .name("User palette colors (foreground and background)")
                    .description("Colors the user saved in the foreground/background color palette");

    /**
     * The user's saved palette colors for the foreground/background palette
     * with alpha selection (see {@link #USER_PALETTE_COLORS_FG}).
     */
    public static final JIPipeSettingKey<JsonNode> USER_PALETTE_COLORS_FG_BG_A =
            JIPipeSettingKey.object("registry:ui-palette", "user-palette-colors/fg-bg-a", JsonNode.class)
                    .scope(SettingsScope.APPLICATION)
                    .name("User palette colors (foreground, background, alpha)")
                    .description("Colors the user saved in the foreground/background color palette with alpha selection");

    // ------------------------------------------------------------------
    // Run queue (registry DB "ui-general")
    // ------------------------------------------------------------------

    /**
     * Whether the run queue logger panel shows its log.
     */
    public static final JIPipeSettingKey<Boolean> RUN_QUEUE_SHOW_LOG =
            JIPipeSettingKey.bool("registry:ui-general", "logger-panel/show-log", Boolean.class)
                    .defaultValue(true)
                    .scope(SettingsScope.APPLICATION)
                    .name("Show run queue log")
                    .description("Whether the run queue logger panel shows its log");

    private SettingsKeys() {
    }

    /**
     * @return the default dock state the graph editors use when the user has
     * no stored layout: the right panel is always visible
     */
    private static JIPipeDesktopDockPanel.State defaultDockState() {
        JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
        state.setAlwaysShowRightPanel(true);
        return state;
    }
}