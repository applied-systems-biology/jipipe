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

package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The recently opened projects.
 * <p>
 * Since the modern settings system (Task 13 of the 2026 settings redesign) the
 * list lives in the {@link SettingsScope#APPLICATION} settings document as the
 * {@code "state:recent-projects"} block: a JSON array of path strings,
 * most-recent first. The legacy {@code recent-projects.txt} file is only read
 * once, by {@link #migrateFromLegacy()} (normally already handled by the
 * profile migration at startup).
 * <p>
 * The public API ({@link List}{@code <Path>}, the change emitter and
 * {@link #add}/{@link #removeAll}/{@link #clear}/{@link #cleanup}/
 * {@link #migrateFromLegacy}) is unchanged. Reads go straight to the document,
 * so they are cheap and always reflect what would be saved.
 */
public final class JIPipeRecentProjectsRegistry extends JIPipeServiceComponent {
    private static final String RECENT_PROJECTS_FILE_NAME = "recent-projects.txt";
    /**
     * The block path of the recent-projects list in the application document.
     */
    private static final String BLOCK_PATH = "state:recent-projects";

    private final ChangedEventEmitter changedEventEmitter = new ChangedEventEmitter();

    public JIPipeRecentProjectsRegistry(JIPipeService service) {
        super(service);
    }

    /**
     * Registers the block with the settings service once the service is fully
     * constructed. (This component is created before the settings component
     * exists, so a constructor-time call would re-enter the initialization.)
     * <p>
     * The block is also registered for <b>union-style merging</b>: the
     * recent-projects list is additive state, so two instances that each open
     * a project must end up with both entries - the whole-block foreign-wins
     * collision rule would silently discard one of them (see
     * {@link org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent#resolveUnionBlocks}).
     */
    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        settings().registerKey(JIPipeSettingKey.object(BLOCK_PATH, "", String.class));
        settings().registerUnionMergeBlock(BLOCK_PATH);
    }

    public ChangedEventEmitter getChangedEventEmitter() {
        return changedEventEmitter;
    }

    /**
     * @return the settings component that owns the recent-projects list
     */
    private JIPipeSettingsServiceComponent settings() {
        return getService().getSettings();
    }

    /**
     * @return the live application settings document
     */
    private SettingsDocument document() {
        return settings().document(SettingsScope.APPLICATION);
    }

    /**
     * Adds a project file to the list of recent projects
     *
     * @param fileName Project file
     */
    public void add(Path fileName) {
        Objects.requireNonNull(fileName, "fileName");
        List<Path> recentProjects = read();
        int index = recentProjects.indexOf(fileName);
        if (index == 0) {
            return; // already the most recent one: nothing changed
        }
        if (index > 0) {
            recentProjects.remove(index);
        }
        recentProjects.add(0, fileName);
        write(recentProjects);
        save();
        changedEventEmitter.emit(new ChangedEvent(this));
    }

    public List<Path> getRecentProjects() {
        return Collections.unmodifiableList(read());
    }

    public void clear() {
        write(new ArrayList<>());
        save();
        changedEventEmitter.emit(new ChangedEvent(this));
    }

    public void cleanup() {
        List<Path> recentProjects = read();
        List<Path> invalidRecentProjects = recentProjects.stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidRecentProjects.isEmpty()) {
            recentProjects.removeAll(invalidRecentProjects);
            write(recentProjects);
            save();
        }
    }

    /**
     * Returns the location of the legacy text file. It is no longer written;
     * it is read only to migrate a profile whose settings were not migrated by
     * the startup profile migration.
     *
     * @return the legacy file path
     */
    public Path getPropertyFile() {
        return JIPipe.getJIPipeUserDir(false).resolve(RECENT_PROJECTS_FILE_NAME);
    }

    /**
     * Re-reads the list from the settings document. The document is
     * materialized (and loaded from disk if necessary), so a reload is cheap.
     */
    public void reload() {
        read();
    }

    /**
     * Persists the application settings (the recent-projects list is part of
     * them). The in-memory list needs no separate flush.
     */
    public void save() {
        settings().save();
    }

    /**
     * Migrates recent projects from a legacy profile.
     * <p>
     * Since the startup profile migration converts {@code recent-projects.txt}
     * into {@code state:recent-projects} (and moves the file into the backup
     * directory), this is normally a no-op. It still works if a legacy
     * {@code recent-projects.txt} — or a 5.x {@code settings.json} holding the
     * old {@code org.hkijena.jipipe:projects/recent-projects} list — exists:
     * its entries are appended, and the legacy file is only removed once its
     * content is persisted in the document.
     */
    public void migrateFromLegacy() {
        List<String> migrated = new ArrayList<>();
        Path propertyFile = getPropertyFile();
        boolean hasLegacyFile = Files.isRegularFile(propertyFile);
        if (hasLegacyFile) {
            try {
                for (String line : Files.readAllLines(propertyFile)) {
                    if (!StringUtils.isNullOrEmpty(line)) {
                        migrated.add(line.trim());
                    }
                }
            } catch (IOException e) {
                getProgressInfo().log("Could not read the legacy recent-projects file " + propertyFile + ": " + e);
                return;
            }
        }
        readLegacySheetEntries(migrated);
        migrateEntries(propertyFile, hasLegacyFile, migrated);
    }

    /**
     * Appends the {@code recent-projects} entries of a 5.x profile's flat
     * settings file (field {@code org.hkijena.jipipe:projects}) to the given
     * list. Only used when the file itself is still legacy-format, i.e. the
     * profile migration did not run.
     *
     * @param migrated receives the legacy paths
     */
    private void readLegacySheetEntries(List<String> migrated) {
        Path settingsFile = JIPipeApplicationSettingsServiceComponent.getSheetsFile(false);
        if (!Files.isRegularFile(settingsFile)) {
            return;
        }
        try {
            JsonNode properties = JsonUtils.getObjectMapper().readValue(settingsFile.toFile(), JsonNode.class);
            if (properties.has("$schema")) {
                return; // new format: the migration already carried the list over
            }
            JsonNode recentProjectsNode = properties.path(JIPipeProjectDefaultsApplicationSettings.ID).path("recent-projects");
            if (recentProjectsNode.isArray()) {
                for (JsonNode node : recentProjectsNode) {
                    migrated.add(node.asText());
                }
            }
        } catch (IOException e) {
            getProgressInfo().log("Could not read the legacy settings file " + settingsFile + ": " + e);
        }
    }

    /**
     * Merges the migrated entries into the document (existing, existing-on-disk
     * entries keep their position; new ones are appended) and, once the
     * document is saved, removes the legacy file.
     */
    private void migrateEntries(Path propertyFile, boolean hasLegacyFile, List<String> migrated) {
        List<Path> recentProjects = read();
        boolean success = false;
        for (String entry : migrated) {
            try {
                Path path = Paths.get(entry);
                if (Files.exists(path) && !recentProjects.contains(path)) {
                    recentProjects.add(path);
                    getProgressInfo().log("- Migrated recent project " + path);
                    success = true;
                }
            } catch (Exception ignored) {
                // not a valid path: skip it
            }
        }
        if (!success && !hasLegacyFile) {
            return;
        }
        getProgressInfo().log("Migrated recent projects. Saving application settings.");
        write(recentProjects);
        save();
        if (hasLegacyFile) {
            try {
                Files.deleteIfExists(propertyFile);
            } catch (IOException e) {
                getProgressInfo().log("Could not remove the migrated recent-projects file " + propertyFile + ": " + e);
            }
        }
        changedEventEmitter.emit(new ChangedEvent(this));
    }

    public void removeAll(Collection<Path> values) {
        List<Path> recentProjects = read();
        if (recentProjects.removeAll(values)) {
            write(recentProjects);
            save();
            changedEventEmitter.emit(new ChangedEvent(this));
        }
    }

    /**
     * @return the stored paths, most recent first (a mutable copy)
     */
    private List<Path> read() {
        List<Path> result = new ArrayList<>();
        JsonNode node = document().get(BLOCK_PATH, "");
        if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isTextual() && !element.asText().isEmpty()) {
                    try {
                        result.add(Paths.get(element.asText()));
                    } catch (RuntimeException ignored) {
                        // an unparsable path entry: skip it
                    }
                }
            }
        }
        return result;
    }

    /**
     * Replaces the stored list with the given paths.
     *
     * @param paths the new list, most recent first
     */
    private void write(List<Path> paths) {
        ArrayNode array = JsonUtils.getObjectMapper().createArrayNode();
        for (Path path : paths) {
            array.add(path.toString());
        }
        settings().writeQuiescently(document(), BLOCK_PATH, array);
    }

    public interface ChangedEventListener {
        void onRecentProjectsChanged(ChangedEvent event);
    }

    public static class ChangedEvent extends AbstractJIPipeEvent {
        public ChangedEvent(Object source) {
            super(source);
        }
    }

    public static class ChangedEventEmitter extends JIPipeEventEmitter<ChangedEvent, ChangedEventListener> {

        @Override
        protected void call(ChangedEventListener changedEventListener, ChangedEvent event) {
            changedEventListener.onRecentProjectsChanged(event);
        }
    }
}