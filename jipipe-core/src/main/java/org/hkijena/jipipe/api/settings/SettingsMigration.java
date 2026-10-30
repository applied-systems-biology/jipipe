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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * One-time migration of a 5.x profile directory into the 6.0.0 settings layout.
 * <p>
 * A 5.x profile stores settings in three places inside the profile directory:
 * <ul>
 *     <li>{@code settings.json} — a flat object whose top-level fields are
 *     settings sheet IDs (no prefix, no {@code $schema});</li>
 *     <li>{@code settings-db-<key>.json} — one file per registry database,
 *     holding a flat {@code "a/b": value} map;</li>
 *     <li>{@code recent-projects.txt} — one absolute project path per line.</li>
 * </ul>
 * The migration turns those into the new unified document(s) of the profile:
 * {@code settings.json} ({@link SettingsScope#APPLICATION}) and, when at least
 * one block is routed there, {@code runtime-settings.json}
 * ({@link SettingsScope#RUNTIME}). Sheet IDs become {@code sheet:<id>} blocks,
 * registry files become {@code registry:<key>} blocks (their flat sub-path
 * keys already are the new tree shape), and the recent-projects file becomes
 * the {@code state:recent-projects} array.
 * <p>
 * <b>Nothing is lost.</b> Every top-level field of the old files ends up in
 * exactly one of the new documents: registered and unregistered sheet IDs alike
 * are prefixed with {@code sheet:} (unknown ones become pass-through blocks so
 * a newer version can still read them), and non-object top-level values are
 * kept verbatim. A legacy file that cannot be parsed is renamed to
 * {@code <name>.bad-<timestamp>} and treated as absent, then moved into the
 * backup directory with the other legacy files — its content is never
 * destroyed and no exception escapes this class.
 * <p>
 * <b>Scope routing.</b> Each resulting block is routed to
 * {@link SettingsScope#APPLICATION} or {@link SettingsScope#RUNTIME} according
 * to {@code scopeByBlock}, keyed by the new prefixed block path (e.g.
 * {@code "sheet:org.hkijena.jipipe:runtime"}). Blocks absent from the map
 * default to {@link SettingsScope#APPLICATION} — the safe choice.
 * <p>
 * <b>Atomicity and idempotency.</b> All legacy content is read into memory
 * first. The legacy files are then moved into
 * {@code <profileDir>/settings-migration-backup/} — an existing file there is
 * never overwritten, a name clash gets a {@code -1}, {@code -2}, … suffix — and
 * the new documents are written through {@link SettingsFileStore} (temp file +
 * atomic rename), so a crash never leaves a half-written profile. If writing a
 * new document fails, the legacy files are restored from the backup. A call
 * with no legacy input is a no-op and does not touch the backup directory.
 * <p>
 * <b>Partially migrated profiles.</b> If a new-format {@code settings.json}
 * (one with {@code $schema}) already exists while legacy files are still
 * present, only the remaining legacy parts are migrated and their blocks are
 * merged into the existing documents; a block present in both keeps the
 * existing value (logged), because in that state the new-format value is the
 * more recent truth.
 */
public final class SettingsMigration {
    /**
     * The name of the backup directory inside the profile dir.
     */
    public static final String BACKUP_DIRECTORY_NAME = "settings-migration-backup";

    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String RUNTIME_FILE_NAME = "runtime-settings.json";
    private static final String RECENT_PROJECTS_FILE_NAME = "recent-projects.txt";
    private static final String REGISTRY_FILE_PREFIX = "settings-db-";
    private static final String REGISTRY_FILE_SUFFIX = ".json";
    private static final String BAD_MARKER = ".bad-";
    private static final String SCHEMA_FIELD = "$schema";
    private static final int SCHEMA_VERSION = 2;
    private static final String RECENT_PROJECTS_BLOCK = "state:recent-projects";
    private static final String SHEET_PREFIX = SettingsKind.SHEET.prefix() + ":";
    private static final String REGISTRY_PREFIX = SettingsKind.REGISTRY.prefix() + ":";

    /**
     * Filename-safe ISO instant for backups, in the same format as
     * {@link SettingsFileStore} (colons are illegal in Windows file names).
     */
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

    private SettingsMigration() {
    }

    /**
     * Reports whether the given profile directory still holds 5.x-format
     * settings that must be migrated. Cheap by design (existence checks plus at
     * most one small read), as this runs at every startup.
     *
     * @param profileDir the profile directory (e.g. {@code <base>/<version>})
     * @return {@code true} when a legacy {@code settings.json} (one without a
     * {@code $schema} field, or one that cannot be parsed), a
     * {@code settings-db-*.json} file, or a {@code recent-projects.txt} exists
     */
    public static boolean needsMigration(Path profileDir) {
        Objects.requireNonNull(profileDir, "profileDir");
        if (!Files.isDirectory(profileDir)) {
            return false;
        }
        if (isLegacySettingsFile(profileDir.resolve(SETTINGS_FILE_NAME))) {
            return true;
        }
        if (Files.isRegularFile(profileDir.resolve(RECENT_PROJECTS_FILE_NAME))) {
            return true;
        }
        return !registryFiles(profileDir).isEmpty();
    }

    /**
     * Migrates the 5.x settings of the given profile directory into the 6.0.0
     * layout. See the class documentation for the full contract: nothing is
     * lost, blocks are routed per {@code scopeByBlock} (default
     * {@link SettingsScope#APPLICATION}), the new files are written atomically,
     * and the legacy files are moved into the backup directory. The method is
     * idempotent and never throws on unparsable legacy input.
     *
     * @param profileDir the profile directory to migrate
     * @param registeredBlock a predicate that receives a canonical block path
     *                        and returns {@code true} when the block is
     *                        registered in this version; used when a
     *                        new-format document of a partially migrated
     *                        profile is read
     * @param scopeByBlock the scope of each migrated block, keyed by the
     *                     <em>new</em> block path (e.g.
     *                     {@code "sheet:org.hkijena.jipipe:runtime"});
     *                     blocks absent from the map go to
     *                     {@link SettingsScope#APPLICATION}. {@link SettingsScope#PROJECT}
     *                     is not routable here and is treated as absent.
     * @param progress receives a log line per migrated part ({@link JIPipeProgressInfo#SILENT} is accepted)
     */
    public static void migrateLegacyProfile(Path profileDir,
                                            Predicate<String> registeredBlock,
                                            Map<String, SettingsScope> scopeByBlock,
                                            JIPipeProgressInfo progress) {
        Objects.requireNonNull(profileDir, "profileDir");
        Objects.requireNonNull(registeredBlock, "registeredBlock");
        Objects.requireNonNull(scopeByBlock, "scopeByBlock");
        Objects.requireNonNull(progress, "progress");

        Path settingsFile = profileDir.resolve(SETTINGS_FILE_NAME);
        Path runtimeFile = profileDir.resolve(RUNTIME_FILE_NAME);
        Path backupDir = profileDir.resolve(BACKUP_DIRECTORY_NAME);

        // Legacy files to move into the backup (see the write order below).
        List<Path> legacyFilesToMove = new ArrayList<>();

        // --- Old settings.json: top-level sheet IDs, no prefix ---
        Map<String, JsonNode> sheetBlocks = new LinkedHashMap<>();
        boolean settingsJsonMigrated = isLegacySettingsFile(settingsFile);
        if (settingsJsonMigrated) {
            JsonNode parsed = parseOrPreserve(settingsFile, true, legacyFilesToMove, progress);
            if (parsed instanceof ObjectNode) {
                parsed.fields().forEachRemaining(entry -> {
                    if (!entry.getKey().startsWith("$")) {
                        sheetBlocks.put(SHEET_PREFIX + entry.getKey(), entry.getValue());
                    }
                });
            }
            if (Files.isRegularFile(settingsFile)) {
                legacyFilesToMove.add(settingsFile);
            }
        }

        // --- settings-db-<key>.json: registry:<key> ---
        Map<String, JsonNode> registryBlocks = new LinkedHashMap<>();
        for (Path registryFile : registryFiles(profileDir)) {
            String key = registryKeyOf(registryFile);
            if (key == null) {
                continue;
            }
            JsonNode parsed = parseOrPreserve(registryFile, false, legacyFilesToMove, progress);
            if (parsed != null) {
                registryBlocks.put(REGISTRY_PREFIX + key, parsed);
            }
            if (Files.isRegularFile(registryFile)) {
                legacyFilesToMove.add(registryFile);
            }
        }

        // --- recent-projects.txt: state:recent-projects ---
        JsonNode recentProjectsBlock = null;
        Path recentProjectsFile = profileDir.resolve(RECENT_PROJECTS_FILE_NAME);
        if (Files.isRegularFile(recentProjectsFile)) {
            recentProjectsBlock = readRecentProjects(recentProjectsFile, progress);
            legacyFilesToMove.add(recentProjectsFile);
        }

        legacyFilesToMove.removeIf(f -> !Files.isRegularFile(f));
        if (legacyFilesToMove.isEmpty() && !settingsJsonMigrated) {
            return;
        }

        // Load the documents that already exist (a partial or foreign
        // new-format profile) so the remaining legacy parts can be merged in.
        SettingsDocument application = null;
        SettingsDocument runtime = null;
        if (!settingsJsonMigrated && Files.isRegularFile(settingsFile)) {
            application = new SettingsFileStore(settingsFile, SettingsScope.APPLICATION, registeredBlock).load();
        }
        if (Files.isRegularFile(runtimeFile)) {
            runtime = new SettingsFileStore(runtimeFile, SettingsScope.RUNTIME, registeredBlock).load();
        }
        if (application == null) {
            application = new SettingsDocument(SettingsScope.APPLICATION, registeredBlock);
        }

        // Route every migrated block to its scope and merge it in.
        Map<String, JsonNode> routed = new LinkedHashMap<>(sheetBlocks);
        routed.putAll(registryBlocks);
        if (recentProjectsBlock != null) {
            routed.put(RECENT_PROJECTS_BLOCK, recentProjectsBlock);
        }
        boolean applicationChanged = settingsJsonMigrated;
        boolean runtimeChanged = false;
        for (Map.Entry<String, JsonNode> entry : routed.entrySet()) {
            String blockPath = entry.getKey();
            if (routeTo(blockPath, scopeByBlock) == SettingsScope.RUNTIME) {
                if (runtime == null) {
                    runtime = new SettingsDocument(SettingsScope.RUNTIME, registeredBlock);
                }
                mergeBlock(runtime, blockPath, entry.getValue(), progress);
                runtimeChanged = true;
            } else {
                mergeBlock(application, blockPath, entry.getValue(), progress);
                applicationChanged = true;
            }
        }

        // Every bit of legacy content is in memory by now. The legacy files are
        // moved into the backup first because the new settings.json reuses the
        // legacy file's name: it has to be moved while it still holds the
        // legacy bytes. The new documents are then written atomically; if that
        // write fails, the legacy files are restored from the backup so the
        // profile is never left worse off than before.
        Instant now = Instant.now();
        Map<Path, Path> backups = new LinkedHashMap<>();
        for (Path legacyFile : legacyFilesToMove) {
            Path backup = moveToBackup(legacyFile, backupDir, progress);
            if (backup != null) {
                backups.put(legacyFile, backup);
            }
        }
        try {
            if (applicationChanged) {
                stampMeta(application, now);
                new SettingsFileStore(settingsFile, SettingsScope.APPLICATION, registeredBlock).save(application);
                progress.log("Wrote migrated application settings to " + settingsFile);
            }
            if (runtimeChanged && runtime != null) {
                stampMeta(runtime, now);
                new SettingsFileStore(runtimeFile, SettingsScope.RUNTIME, registeredBlock).save(runtime);
                progress.log("Wrote migrated runtime settings to " + runtimeFile);
            }
        } catch (RuntimeException e) {
            restoreFromBackup(backups, progress, e);
            throw e;
        }
    }

    /**
     * Puts the legacy files back where they were after the new documents could
     * not be written, so the next start simply retries the migration.
     */
    private static void restoreFromBackup(Map<Path, Path> backups, JIPipeProgressInfo progress, RuntimeException cause) {
        progress.log("Could not write the migrated settings (" + cause + "); restoring the legacy files");
        for (Map.Entry<Path, Path> entry : backups.entrySet()) {
            try {
                Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                progress.log("Could not restore " + entry.getKey().getFileName() + " from the backup: " + e);
            }
        }
    }

    /**
     * @param blockPath the new block path
     * @param scopeByBlock the routing map
     * @return the target scope: {@link SettingsScope#RUNTIME} when the block is
     * routed there, {@link SettingsScope#APPLICATION} otherwise (including
     * absent and non-routable entries)
     */
    private static SettingsScope routeTo(String blockPath, Map<String, SettingsScope> scopeByBlock) {
        return scopeByBlock.get(blockPath) == SettingsScope.RUNTIME ? SettingsScope.RUNTIME : SettingsScope.APPLICATION;
    }

    /**
     * Merges a migrated block into the target document. An existing block wins
     * over the legacy content: in a partially migrated profile the new-format
     * value is the more recent truth.
     */
    private static void mergeBlock(SettingsDocument document, String blockPath, JsonNode value, JIPipeProgressInfo progress) {
        if (document.hasBlock(blockPath)) {
            progress.log("Keeping the existing settings block '" + blockPath + "' over its legacy content");
            return;
        }
        document.setBlock(blockPath, value.deepCopy());
    }

    private static void stampMeta(SettingsDocument document, Instant now) {
        document.setSchemaVersion(SCHEMA_VERSION);
        ObjectNode meta = document.meta();
        meta.put("written-by-version", VersionUtils.getJIPipeVersion());
        meta.put("written-at", DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(now));
        meta.put("writer-id", document.writerId());
    }

    /**
     * @param file the candidate settings file
     * @return whether the file exists and is <em>not</em> in the new format,
     * i.e. lacks a {@code $schema} field (or cannot be parsed at all)
     */
    private static boolean isLegacySettingsFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(Files.readString(file, StandardCharsets.UTF_8));
            return node == null || !node.isObject() || !node.has(SCHEMA_FIELD);
        } catch (IOException | RuntimeException e) {
            // Unparsable: needs the migration, which preserves it as a .bad- backup.
            return true;
        }
    }

    /**
     * @param profileDir the profile directory
     * @return the {@code settings-db-<key>.json} files in the directory, sorted by name
     */
    private static List<Path> registryFiles(Path profileDir) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(profileDir, REGISTRY_FILE_PREFIX + "*" + REGISTRY_FILE_SUFFIX)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && registryKeyOf(entry) != null) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            return files;
        }
        files.sort(null);
        return files;
    }

    /**
     * @param file a file named {@code settings-db-<key>.json}
     * @return the database key, or {@code null} if the name does not carry one
     */
    private static String registryKeyOf(Path file) {
        String name = file.getFileName().toString();
        if (!name.startsWith(REGISTRY_FILE_PREFIX) || !name.endsWith(REGISTRY_FILE_SUFFIX)) {
            return null;
        }
        String key = name.substring(REGISTRY_FILE_PREFIX.length(), name.length() - REGISTRY_FILE_SUFFIX.length());
        return key.isEmpty() ? null : key;
    }

    /**
     * Parses a legacy settings file. A file that cannot be parsed (invalid
     * JSON, an unexpected node type, an IO error) is preserved next to itself
     * as {@code <name>.bad-<timestamp>} and treated as absent.
     *
     * @param file the file to parse
     * @param expectObject whether a JSON object is required
     * @param legacyFiles receives the backup of an unparsable file, if any, so
     *                    it is moved into the backup directory together with
     *                    the other legacy files
     * @param progress the progress info
     * @return the parsed node, or {@code null} if the file could not be parsed
     */
    private static JsonNode parseOrPreserve(Path file,
                                            boolean expectObject,
                                            List<Path> legacyFiles,
                                            JIPipeProgressInfo progress) {
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(Files.readString(file, StandardCharsets.UTF_8));
            if (node == null || node.isMissingNode() || (expectObject && !node.isObject())) {
                throw new IOException(expectObject ? "not a JSON object" : "empty file");
            }
            return node;
        } catch (IOException | RuntimeException e) {
            Path backup = preserveAsBad(file, progress);
            if (backup != null) {
                legacyFiles.add(backup);
            }
            return null;
        }
    }

    /**
     * Preserves a legacy file that cannot be parsed, using the same
     * {@code <name>.bad-<timestamp>} convention as {@link SettingsFileStore}.
     *
     * @return the backup path, or {@code null} if even the backup failed
     */
    private static Path preserveAsBad(Path file, JIPipeProgressInfo progress) {
        Path backup = firstFreeName(file.resolveSibling(file.getFileName() + BAD_MARKER + BACKUP_TIMESTAMP.format(Instant.now())));
        try {
            try {
                Files.move(file, backup, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(file, backup);
            }
            progress.log("Settings file " + file.getFileName() + " is not valid settings JSON; preserved as " + backup.getFileName());
            return backup;
        } catch (IOException e) {
            progress.log("Settings file " + file.getFileName() + " is not valid settings JSON and could not be preserved ("
                    + e + "); it is left in place");
            return null;
        }
    }

    private static ArrayNode readRecentProjects(Path file, JIPipeProgressInfo progress) {
        ArrayNode array = JsonUtils.getObjectMapper().createArrayNode();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    array.add(trimmed);
                }
            }
        } catch (IOException | RuntimeException e) {
            progress.log("Could not read the whole recent-projects file " + file + ": " + e);
        }
        return array;
    }

    /**
     * @param candidate the preferred name
     * @return the candidate if no file uses it, otherwise the first free
     * {@code <name>-1}, {@code <name>-2}, … name (the suffix is inserted
     * before the extension)
     */
    private static Path firstFreeName(Path candidate) {
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String fileName = candidate.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; ; i++) {
            Path next = candidate.resolveSibling(base + "-" + i + extension);
            if (!Files.exists(next)) {
                return next;
            }
        }
    }

    /**
     * Moves a file into the backup directory without ever overwriting an
     * existing file there.
     *
     * @return the file's new location, or {@code null} if the move failed
     */
    private static Path moveToBackup(Path file, Path backupDir, JIPipeProgressInfo progress) {
        try {
            Files.createDirectories(backupDir);
            Path target = firstFreeName(backupDir.resolve(file.getFileName().toString()));
            try {
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(file, target);
            }
            progress.log("Moved legacy settings file " + file.getFileName() + " to " + target);
            return target;
        } catch (IOException e) {
            progress.log("Could not move legacy settings file " + file.getFileName() + " to the backup directory: " + e);
            return null;
        }
    }
}