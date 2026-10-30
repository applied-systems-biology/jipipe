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
 * atomic rename), so a crash never leaves a half-written profile.
 * <p>
 * The backup moves are <b>all-or-nothing</b>: if any legacy file cannot be
 * moved, every already-moved file is restored to its original name and the
 * migration aborts before writing anything, leaving the profile exactly as it
 * was found so the next start retries. If writing a new document fails after
 * the moves, the legacy files are likewise restored. A call with no legacy
 * input is a no-op and does not touch the backup directory.
 * <p>
 * After a successful write, the legacy inputs that were moved into the backup
 * directory are deleted again, while the directory itself and any
 * {@code .bad-*} corruption backups stay. This is what lets
 * {@link #recoverFromInterruptedMigration(Path, Predicate, Map, JIPipeProgressInfo)}
 * tell a crashed migration (the backup still holds the user's only copy)
 * apart from a user who deliberately deleted their settings after a
 * successful migration (the stale backup must not resurrect them).
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

        // Legacy content to read. Corrupt files are renamed to .bad-<timestamp>
        // right away and end up in `legacyFiles` as that preserved name.
        List<Path> legacyFiles = new ArrayList<>();

        Path recentProjectsFile = profileDir.resolve(RECENT_PROJECTS_FILE_NAME);
        if (Files.isRegularFile(recentProjectsFile)) {
            legacyFiles.add(recentProjectsFile);
        }
        legacyFiles.addAll(registryFiles(profileDir));

        // --- Old settings.json: top-level sheet IDs, no prefix ---
        Map<String, JsonNode> sheetBlocks = new LinkedHashMap<>();
        boolean settingsJsonMigrated = isLegacySettingsFile(settingsFile);
        if (settingsJsonMigrated) {
            legacyFiles.add(settingsFile);
        }
        if (legacyFiles.isEmpty()) {
            return;
        }
        for (Path file : legacyFiles) {
            progress.log("Migrating legacy settings file " + file.getFileName());
        }
        if (settingsJsonMigrated) {
            JsonNode parsed = parseOrPreserve(settingsFile, true, legacyFiles, progress);
            if (parsed instanceof ObjectNode) {
                parsed.fields().forEachRemaining(entry -> {
                    if (entry.getKey().startsWith("$")) {
                        progress.log("Skipping reserved field '" + entry.getKey() + "' of the legacy settings file");
                    } else {
                        sheetBlocks.put(SHEET_PREFIX + entry.getKey(), entry.getValue());
                    }
                });
            }
        }

        // --- settings-db-<key>.json: registry:<key> ---
        Map<String, JsonNode> registryBlocks = new LinkedHashMap<>();
        for (Path registryFile : registryFiles(profileDir)) {
            String key = registryKeyOf(registryFile);
            if (key == null) {
                continue;
            }
            JsonNode parsed = parseOrPreserve(registryFile, false, legacyFiles, progress);
            if (parsed != null) {
                registryBlocks.put(REGISTRY_PREFIX + key, parsed);
            }
        }

        // --- recent-projects.txt: state:recent-projects ---
        Map<String, JsonNode> routed = new LinkedHashMap<>(sheetBlocks);
        routed.putAll(registryBlocks);
        if (Files.isRegularFile(recentProjectsFile)) {
            routed.put(RECENT_PROJECTS_BLOCK, readRecentProjects(recentProjectsFile, progress));
        }

        legacyFiles.removeIf(f -> !Files.isRegularFile(f));
        if (legacyFiles.isEmpty() && !settingsJsonMigrated) {
            return;
        }

        migrateInto(profileDir, registeredBlock, scopeByBlock, progress,
                settingsFile, runtimeFile, backupDir, routed, settingsJsonMigrated, legacyFiles);
    }

    /**
     * Writes the routed blocks into the profile's documents (merging with what
     * already exists) and cleans up the backup set on success.
     *
     * @param settingsFile the application document
     * @param runtimeFile the runtime document
     * @param backupDir the backup directory
     * @param routed the migrated blocks with their <em>new</em> block paths
     * @param applicationFromScratch whether {@code settingsFile} held legacy
     *                               content (so it must be rewritten even when
     *                               {@code routed} is empty)
     * @param legacyFiles the legacy files to move into the backup; they are
     *                    restored and the whole operation is aborted if any
     *                    move fails
     */
    private static void migrateInto(Path profileDir,
                                    Predicate<String> registeredBlock,
                                    Map<String, SettingsScope> scopeByBlock,
                                    JIPipeProgressInfo progress,
                                    Path settingsFile,
                                    Path runtimeFile,
                                    Path backupDir,
                                    Map<String, JsonNode> routed,
                                    boolean applicationFromScratch,
                                    List<Path> legacyFiles) {
        // Load the documents that already exist (a partial or foreign
        // new-format profile) so the remaining legacy parts can be merged in.
        SettingsDocument application = null;
        SettingsDocument runtime = null;
        if (!applicationFromScratch && Files.isRegularFile(settingsFile)) {
            application = new SettingsFileStore(settingsFile, SettingsScope.APPLICATION, registeredBlock).load();
        }
        if (Files.isRegularFile(runtimeFile)) {
            runtime = new SettingsFileStore(runtimeFile, SettingsScope.RUNTIME, registeredBlock).load();
        }
        if (application == null) {
            application = new SettingsDocument(SettingsScope.APPLICATION, registeredBlock);
        }

        // Route every migrated block to its scope and merge it in.
        boolean applicationChanged = applicationFromScratch;
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
        // legacy bytes. The moves are all-or-nothing — a partial backup set
        // would let the write below overwrite the file that failed to move.
        List<BackupMove> backups = moveAllToBackup(legacyFiles, backupDir, progress);
        if (backups == null) {
            return;
        }

        Instant now = Instant.now();
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
            progress.log("Could not write the migrated settings (" + e + "); restoring the legacy files");
            restoreAll(backups, progress);
            throw e;
        }

        // Success: the documents hold everything, so the legacy inputs in the
        // backup must no longer look like an interrupted migration.
        // Corruption backups (.bad-*) and unrelated user files stay.
        removeMigratedBackups(backups, progress);
    }

    /**
     * Recovers from a migration that was interrupted between moving the legacy
     * files into {@code settings-migration-backup/} and writing the new
     * documents. Without this pass, such a profile has no
     * {@code settings.json}, so {@link #needsMigration(Path)} is {@code false}
     * forever and the application would run on an empty document while the
     * user's whole profile sits in the backup directory.
     * <p>
     * The pass fires only when <em>all</em> of the following hold:
     * <ul>
     *     <li>the profile directory has no legacy input left
     *     ({@link #needsMigration(Path)} is {@code false}),</li>
     *     <li>the backup directory exists and still contains legacy-format
     *     input (an old-shape {@code settings.json}, a {@code settings-db-*.json},
     *     or a {@code recent-projects.txt}), and</li>
     *     <li>a file-backed document is missing: {@code settings.json} for a
     *     full recovery, or {@code runtime-settings.json} while the backup
     *     input contains blocks that route to {@link SettingsScope#RUNTIME}
     *     for a partial recovery.</li>
     * </ul>
     * Recovery only <em>reads</em> the backup — it never moves those files
     * again, as they are the user's only copy — migrates them with the same
     * rules as {@link #migrateLegacyProfile(Path, Predicate, Map, JIPipeProgressInfo)},
     * and merges into any existing document with the existing-blocks-win rule,
     * so content written meanwhile is never clobbered. Writes go through the
     * same atomic {@link SettingsFileStore}.
     * <p>
     * Because a successful migration deletes the legacy inputs from the
     * backup, a profile whose backup only holds stale inputs (or only
     * {@code .bad-*} corruption backups) does <em>not</em> trigger a recovery.
     *
     * @param profileDir the profile directory to check
     * @param registeredBlock the registration predicate (see
     *                        {@link #migrateLegacyProfile(Path, Predicate, Map, JIPipeProgressInfo)})
     * @param scopeByBlock the scope routing map, keyed by the new block path
     * @param progress receives a loud log line when a recovery happens
     * @return whether any document was written
     */
    public static boolean recoverFromInterruptedMigration(Path profileDir,
                                                          Predicate<String> registeredBlock,
                                                          Map<String, SettingsScope> scopeByBlock,
                                                          JIPipeProgressInfo progress) {
        Objects.requireNonNull(profileDir, "profileDir");
        Objects.requireNonNull(registeredBlock, "registeredBlock");
        Objects.requireNonNull(scopeByBlock, "scopeByBlock");
        Objects.requireNonNull(progress, "progress");

        if (needsMigration(profileDir)) {
            // A normal migration is (still) pending; it handles everything.
            return false;
        }
        Path settingsFile = profileDir.resolve(SETTINGS_FILE_NAME);
        Path runtimeFile = profileDir.resolve(RUNTIME_FILE_NAME);
        Path backupDir = profileDir.resolve(BACKUP_DIRECTORY_NAME);
        if (!Files.isDirectory(backupDir)) {
            return false;
        }
        boolean applicationMissing = !Files.isRegularFile(settingsFile);
        boolean runtimeMissing = !Files.isRegularFile(runtimeFile);
        if (!applicationMissing && !runtimeMissing) {
            return false;
        }

        // Read the legacy input from the backup. Files there are never moved,
        // never renamed and never modified, so a failure to parse one simply
        // skips it (and the profile stays untouched).
        Map<String, JsonNode> routed = new LinkedHashMap<>();
        Path backupSettings = backupDir.resolve(SETTINGS_FILE_NAME);
        if (isLegacySettingsFile(backupSettings)) {
            JsonNode parsed = readTreeOrNull(backupSettings);
            if (parsed instanceof ObjectNode) {
                parsed.fields().forEachRemaining(entry -> {
                    if (!entry.getKey().startsWith("$")) {
                        routed.put(SHEET_PREFIX + entry.getKey(), entry.getValue());
                    }
                });
            }
        }
        for (Path registryFile : registryFiles(backupDir)) {
            String key = registryKeyOf(registryFile);
            JsonNode parsed = readTreeOrNull(registryFile);
            if (key != null && parsed instanceof ObjectNode) {
                routed.put(REGISTRY_PREFIX + key, parsed);
            }
        }
        Path backupRecentProjects = backupDir.resolve(RECENT_PROJECTS_FILE_NAME);
        if (Files.isRegularFile(backupRecentProjects)) {
            routed.put(RECENT_PROJECTS_BLOCK, readRecentProjects(backupRecentProjects, progress));
        }
        if (routed.isEmpty()) {
            return false;
        }

        // Decide between a full and a partial recovery.
        if (!applicationMissing) {
            boolean runtimeBlocksInBackup = routed.keySet().stream()
                    .anyMatch(blockPath -> routeTo(blockPath, scopeByBlock) == SettingsScope.RUNTIME);
            if (!runtimeMissing || !runtimeBlocksInBackup) {
                // The application document exists and either the runtime
                // document exists too or nothing would go into it, so there is
                // no interrupted migration to recover from.
                return false;
            }
        }

        progress.log("An interrupted settings migration was detected in " + profileDir
                + ": the legacy settings are in " + backupDir + " but "
                + (applicationMissing ? "settings.json" : "runtime-settings.json")
                + " is missing. Recovering the settings now.");

        SettingsDocument application = null;
        SettingsDocument runtime = null;
        boolean applicationChanged = applicationMissing;
        boolean runtimeChanged = false;
        if (!applicationMissing) {
            application = new SettingsFileStore(settingsFile, SettingsScope.APPLICATION, registeredBlock).load();
        }
        if (!runtimeMissing) {
            runtime = new SettingsFileStore(runtimeFile, SettingsScope.RUNTIME, registeredBlock).load();
        }
        for (Map.Entry<String, JsonNode> entry : routed.entrySet()) {
            String blockPath = entry.getKey();
            if (routeTo(blockPath, scopeByBlock) == SettingsScope.RUNTIME) {
                if (runtime == null) {
                    runtime = new SettingsDocument(SettingsScope.RUNTIME, registeredBlock);
                }
                mergeBlock(runtime, blockPath, entry.getValue(), progress);
                runtimeChanged = true;
            } else if (applicationMissing) {
                if (application == null) {
                    application = new SettingsDocument(SettingsScope.APPLICATION, registeredBlock);
                }
                mergeBlock(application, blockPath, entry.getValue(), progress);
            }
            // A full recovery recreates the application document; a partial one
            // leaves the existing application document untouched, because the
            // migration that wrote it already carried these blocks.
        }

        Instant now = Instant.now();
        if (applicationChanged && application != null) {
            stampMeta(application, now);
            new SettingsFileStore(settingsFile, SettingsScope.APPLICATION, registeredBlock).save(application);
            progress.log("Recovered the application settings into " + settingsFile);
        }
        if (runtimeChanged && runtime != null) {
            stampMeta(runtime, now);
            new SettingsFileStore(runtimeFile, SettingsScope.RUNTIME, registeredBlock).save(runtime);
            progress.log("Recovered the runtime settings into " + runtimeFile);
        }
        return applicationChanged || runtimeChanged;
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
     * Moves every legacy file into the backup directory, all-or-nothing: if
     * any move fails, the files moved so far are put back (in reverse order,
     * under their original names) and {@code null} is returned. The caller
     * must then abort without writing anything.
     * <p>
     * The restore target for a moved file is its original name in the profile
     * directory. A corrupt file that was preserved before the moves already
     * carries a unique {@code .bad-<timestamp>} name, so it cannot collide
     * with the legacy name of any other file.
     *
     * @return the completed moves in order, or {@code null} if a move failed
     */
    private static List<BackupMove> moveAllToBackup(List<Path> legacyFiles, Path backupDir, JIPipeProgressInfo progress) {
        List<BackupMove> moves = new ArrayList<>();
        for (Path legacyFile : legacyFiles) {
            Path target;
            try {
                Files.createDirectories(backupDir);
                target = firstFreeName(backupDir.resolve(legacyFile.getFileName().toString()));
                try {
                    Files.move(legacyFile, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(legacyFile, target);
                }
            } catch (IOException e) {
                progress.log("Could not move legacy settings file " + legacyFile.getFileName()
                        + " to the backup directory (" + e + "); aborting the migration, nothing was changed");
                restoreAll(moves, progress);
                return null;
            }
            progress.log("Moved legacy settings file " + legacyFile.getFileName() + " to " + target);
            moves.add(new BackupMove(legacyFile, target));
        }
        return moves;
    }

    /**
     * Puts moved legacy files back under their original names, most recent
     * move first. A plain move back can fail for the same reason the move
     * failed (e.g. a read-only backup directory), so a failed move falls back
     * to copying the bytes and deleting the backup copy.
     */
    private static void restoreAll(List<BackupMove> moves, JIPipeProgressInfo progress) {
        for (int i = moves.size() - 1; i >= 0; i--) {
            BackupMove move = moves.get(i);
            try {
                Files.move(move.backup(), move.original(), StandardCopyOption.REPLACE_EXISTING);
                continue;
            } catch (IOException e) {
                progress.log("Could not move " + move.original().getFileName() + " back (" + e + "); copying it instead");
            }
            try {
                Files.copy(move.backup(), move.original(), StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.deleteIfExists(move.backup());
                } catch (IOException e) {
                    progress.log("Restored " + move.original().getFileName()
                            + ", but could not remove the backup copy " + move.backup() + ": " + e);
                }
            } catch (IOException e) {
                progress.log("Could not restore " + move.original().getFileName()
                        + " from the backup; its only copy stays in " + move.backup());
            }
        }
    }

    /**
     * Deletes the legacy inputs a successful migration moved into the backup,
     * so they can no longer be mistaken for an interrupted migration (see
     * {@link #recoverFromInterruptedMigration(Path, Predicate, Map, JIPipeProgressInfo)}).
     * Corruption backups ({@code .bad-*}) and unrelated files stay, as does
     * the directory itself.
     */
    private static void removeMigratedBackups(List<BackupMove> moves, JIPipeProgressInfo progress) {
        for (BackupMove move : moves) {
            if (move.backup().getFileName().toString().contains(BAD_MARKER)) {
                continue;
            }
            try {
                Files.deleteIfExists(move.backup());
            } catch (IOException e) {
                progress.log("Could not remove the migrated legacy copy " + move.backup() + ": " + e);
            }
        }
    }

    /**
     * Reads and parses a JSON tree, returning {@code null} instead of throwing.
     * Used for the backup directory, which is read-only for recovery.
     */
    private static JsonNode readTreeOrNull(Path file) {
        try {
            return JsonUtils.getObjectMapper().readTree(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * One legacy file's trip into the backup directory.
     *
     * @param original the file's location in the profile directory
     * @param backup the file's location in the backup directory
     */
    private record BackupMove(Path original, Path backup) {
    }
}
