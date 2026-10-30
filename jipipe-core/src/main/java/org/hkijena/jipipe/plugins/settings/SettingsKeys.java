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

import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsScope;
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
 * <b>Lifetime of bindings.</b> Bind a key through
 * {@link org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent#bind}
 * and keep the binding for the life of the consumer that owns it — typically
 * the application: a UI component that binds a key at construction stays
 * bound until the JVM exits. The settings service tracks every binding it
 * hands out and closes them all in its {@code dispose()}, so a component
 * must <em>not</em> close its binding when one panel or dialog is destroyed:
 * the next panel re-binds the same key and gets its own live binding (binding
 * the same key twice is allowed, each instance has its own subscribers), and
 * the service closes them all at dispose. Bindings created through a
 * {@code forTesting(...)} component are closed the same way.
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