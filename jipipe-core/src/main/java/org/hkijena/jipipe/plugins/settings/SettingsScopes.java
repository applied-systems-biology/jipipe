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

import org.hkijena.jipipe.api.settings.SettingsScope;

import java.util.Map;

/**
 * The static scope assignment of the settings blocks that are not (yet) backed
 * by typed {@link org.hkijena.jipipe.api.settings.JIPipeSettingKey}s: which
 * blocks belong to the {@link SettingsScope#RUNTIME} document.
 * <p>
 * <b>Ownership rule (spec §3.1).</b> Runtime = execution state: thread pool,
 * temp/cache directories, environment installations, hardware acceleration,
 * artifact directories. Application = everything UI and user preference.
 * Everything absent from this map is {@link SettingsScope#APPLICATION} — that
 * is the default, so application blocks are deliberately <em>not</em>
 * enumerated here.
 * <p>
 * <b>Keys first.</b> The map is only the fallback for not-yet-keyified blocks.
 * A block that has keys registered with {@link SettingsScope#RUNTIME} is
 * runtime-scoped through the keys (see
 * {@code JIPipeSettingsServiceComponent#effectiveScopeByBlock()}, which layers
 * the registered keys over this map); as sheets get keyified (Tasks 15/16),
 * the entries below can be removed without changing the routing.
 * <p>
 * <b>Stable literals.</b> The keys are the sheet/registry IDs exactly as they
 * are registered and appear in saved profiles — they must never be derived
 * from class names (a rename would silently re-route migrated settings).
 */
public final class SettingsScopes {

    /**
     * The runtime-scoped blocks: block path → {@link SettingsScope#RUNTIME}.
     * Every other block is application-scoped.
     */
    private static final Map<String, SettingsScope> RUNTIME_BLOCKS = Map.of(
            // Threads, temp directories, caches (JIPipeRuntimeApplicationSettings)
            "sheet:org.hkijena.jipipe:runtime", SettingsScope.RUNTIME,
            // Installed hardware acceleration backends (JIPipeHardwareAccelerationApplicationSettings)
            "sheet:org.hkijena.jipipe:hardware-acceleration", SettingsScope.RUNTIME,
            // Ruled (#1345 final review): JIPipeDefaultEnvironmentsApplicationSettings
            //  mixes installed environments (machine state) with the default
            //  environment of newly created projects (a default template that is
            //  still machine-side configuration). The whole block is therefore
            //  runtime-scoped. A per-key split is a future option if the
            //  project-creation defaults ever need to be per user; the routing
            //  table is keyed by block, so such a split means moving those keys
            //  into their own block, not a finer routing entry.
            "sheet:org.hkijena.jipipe:default-environments", SettingsScope.RUNTIME,
            // Artifact download directories (JIPipeArtifactApplicationSettings)
            "sheet:org.hkijena.jipipe:artifacts", SettingsScope.RUNTIME,
            // Downloader processes and their locations (JIPipeDownloadsApplicationSettings)
            "sheet:org.hkijena.jipipe:downloads", SettingsScope.RUNTIME
            // A genuinely execution-state registry block does not exist today
            //  (verified in Task 14). When one arrives — e.g. an AI
            //  remote-backend database, or any registry whose content describes
            //  the machine's execution environment rather than a user preference
            //  — it is added here as a ``registry:<id>`` entry (or as a map entry
            //  under it, for a keyed sub-table). The UI registry databases
            //  (ui-graph-editor, ui-general, ui-dock, ui-palette, node-db) hold
            //  user-interface state and stay application-scoped.
    );

    private SettingsScopes() {
    }

    /**
     * @return the runtime-scoped settings blocks (block path → {@link
     * SettingsScope#RUNTIME}). Immutable; blocks absent from it are
     * {@link SettingsScope#APPLICATION}.
     */
    public static Map<String, SettingsScope> scopeByBlock() {
        return RUNTIME_BLOCKS;
    }
}