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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Cross-version carry-over (recovery merge) of settings blocks: merges blocks
 * that the current version does <em>not</em> know back from a sibling version's
 * profile, so data written by a newer or development build is not silently
 * stranded when an older or another build runs (spec §3.3, reverse merge).
 * <p>
 * Settings documents live in per-version profile directories
 * ({@code <profileBaseDir>/<version>/settings.json}). Dev builds and releases
 * share a version string, and downgrades or parallel installs write into
 * sibling directories, so a profile directory of the current version may lack
 * blocks that a sibling holds.
 * <p>
 * <b>Rules.</b>
 * <ul>
 * <li><b>Never override a known block.</b> Only blocks whose canonical path is
 * <em>unknown</em> to this version are recovered: the
 * {@code registeredBlock} predicate returns {@code false} for them, or their
 * kind prefix is unknown (pass-through). A block the predicate registers —
 * and every block the current document already holds, registered or not —
 * keeps the current version's value, including when a registered block is
 * absent locally (it is then <em>not</em> pulled from a sibling).</li>
 * <li><b>Newest sibling first.</b> Sibling profile directories are scanned in
 * descending version order (via {@link StringUtils#compareVersions(String, String)});
 * a block is taken from the first sibling that contains it.</li>
 * <li><b>Siblings are read-only.</b> A sibling profile file is never
 * modified, moved, or backed up.</li>
 * <li><b>Conservative writes.</b> The recovered blocks are merged into the
 * <em>current</em> document and written atomically through
 * {@link SettingsFileStore} (temp file + rename). A missing current file is
 * created with the recovered blocks; a corrupt current file is preserved as
 * {@code <name>.bad-<timestamp>} (the {@link SettingsFileStore} convention) and
 * a fresh document is used.</li>
 * <li><b>Fail-safe.</b> A missing, unreadable, or corrupt sibling file is
 * logged and skipped; it never aborts the carry-over.</li>
 * </ul>
 * <p>
 * Nothing is ever deleted: blocks the current document already has are kept,
 * and everything else in the current document is left untouched.
 */
public final class SettingsCarryOver {
    private static final Logger logger = LoggerFactory.getLogger(SettingsCarryOver.class);

    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final int SCHEMA_VERSION = 2;
    private static final DateTimeFormatter META_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private SettingsCarryOver() {
    }

    /**
     * Recovers the blocks that the current version does not know from sibling
     * version profiles and merges them into the current settings file.
     * <p>
     * See the class documentation for the full contract. The current file is
     * only written when at least one block was recovered, so a version with no
     * unknown blocks never touches the file.
     *
     * @param currentSettingsFile the settings file of the current version (e.g.
     *                            {@code <base>/6.0.0/settings.json})
     * @param profileBaseDir the directory holding one sub-directory per
     *                       version (e.g. {@code <base>})
     * @param currentVersion the current version string; its sub-directory of
     *                       {@code profileBaseDir} is never scanned as a sibling
     * @param registeredBlock a predicate that receives a canonical block path
     *                        (e.g. {@code "sheet:org.hkijena.jipipe:general-ui"})
     *                        and returns {@code true} when the block is
     *                        registered (known) to this version
     * @return the canonical paths of the recovered blocks (see
     * {@link #recoverUnknownBlocksByVersion} for the source version)
     */
    public static Set<String> recoverUnknownBlocks(Path currentSettingsFile,
                                                   Path profileBaseDir,
                                                   String currentVersion,
                                                   Predicate<String> registeredBlock) {
        return new LinkedHashSet<>(recoverUnknownBlocksByVersion(currentSettingsFile, profileBaseDir,
                currentVersion, registeredBlock).keySet());
    }

    /**
     * Same as
     * {@link #recoverUnknownBlocks(Path, Path, String, Predicate)}, but also
     * reports which sibling version each recovered block came from, so the
     * caller can tell the user "settings recovered from version X".
     *
     * @param currentSettingsFile the settings file of the current version
     * @param profileBaseDir the directory holding one sub-directory per version
     * @param currentVersion the current version string
     * @param registeredBlock the registration predicate
     * @return the recovered block paths mapped to the version directory they
     * were recovered from (insertion order = recovery order); empty when
     * nothing was recovered
     */
    public static Map<String, String> recoverUnknownBlocksByVersion(Path currentSettingsFile,
                                                                    Path profileBaseDir,
                                                                    String currentVersion,
                                                                    Predicate<String> registeredBlock) {
        Objects.requireNonNull(currentSettingsFile, "currentSettingsFile");
        Objects.requireNonNull(profileBaseDir, "profileBaseDir");
        Objects.requireNonNull(currentVersion, "currentVersion");
        Objects.requireNonNull(registeredBlock, "registeredBlock");

        List<String> siblings = siblingVersions(profileBaseDir, currentVersion);
        if (siblings.isEmpty()) {
            return Map.of();
        }

        // Load the current document. SettingsFileStore#load() never throws: it
        // preserves a corrupt file as <name>.bad-<timestamp> and yields a fresh
        // (empty) document, which is exactly the recovery behaviour we want.
        // The store's registration predicate only decides pass-through-ness of
        // the in-memory document (nothing here depends on it), so the caller's
        // predicate is used throughout.
        SettingsFileStore store = new SettingsFileStore(currentSettingsFile, SettingsScope.APPLICATION, registeredBlock);
        SettingsDocument current = store.load();
        // Everything the current document already holds keeps its value,
        // registered or not: a block must never be replaced by a sibling's.
        Set<String> existing = current.blockPaths();

        Set<String> recovered = new LinkedHashSet<>();
        Map<String, String> recoveredByVersion = new LinkedHashMap<>();
        for (String sibling : siblings) {
            Path siblingFile = profileBaseDir.resolve(sibling).resolve(SETTINGS_FILE_NAME);
            JsonNode root = readSibling(siblingFile);
            if (root == null) {
                continue;
            }
            Iterator<Map.Entry<String, JsonNode>> blocks = root.fields();
            while (blocks.hasNext()) {
                Map.Entry<String, JsonNode> block = blocks.next();
                String blockPath = block.getKey();
                if (blockPath.startsWith("$")) {
                    continue; // reserved fields are never merged
                }
                if (existing.contains(blockPath) || recovered.contains(blockPath)) {
                    continue; // present locally, or already taken from a newer sibling
                }
                if (!isUnknownBlock(blockPath, registeredBlock)) {
                    continue; // a registered block is never taken from a sibling
                }
                current.setBlock(blockPath, block.getValue().deepCopy());
                recovered.add(blockPath);
                recoveredByVersion.put(blockPath, sibling);
            }
        }

        if (recovered.isEmpty()) {
            return Map.of();
        }
        stampMeta(current, currentVersion);
        store.save(current);
        logger.info("Recovered {} settings block(s) for version {} from sibling profiles: {}",
                recovered.size(), currentVersion, recoveredByVersion);
        return recoveredByVersion;
    }

    /**
     * @param profileBaseDir the profiles directory
     * @param currentVersion the current version
     * @return the names of the sibling profile directories (sub-directories of
     * {@code profileBaseDir} other than the current version's), sorted newest
     * version first
     */
    private static List<String> siblingVersions(Path profileBaseDir, String currentVersion) {
        if (!Files.isDirectory(profileBaseDir)) {
            return List.of();
        }
        List<String> siblings = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profileBaseDir)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }
                String name = entry.getFileName().toString();
                if (!name.equals(currentVersion)) {
                    siblings.add(name);
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not list the sibling settings profiles in {}: {}", profileBaseDir, e.toString());
            return List.of();
        }
        // Newest first; a stable descending version order with the name as the
        // tie-breaker keeps the scan deterministic for equal version strings.
        siblings.sort(Comparator.comparing((String v) -> v, SettingsCarryOver::compareVersionsDescending)
                .thenComparing(Comparator.naturalOrder()));
        return siblings;
    }

    private static int compareVersionsDescending(String a, String b) {
        int result = StringUtils.compareVersions(b, a);
        if (result == 0) {
            return 0;
        }
        return result < 0 ? -1 : 1;
    }

    /**
     * @param blockPath the canonical block path
     * @param registeredBlock the registration predicate
     * @return whether the block is unknown to this version: a pass-through
     * block (no or unknown kind prefix) or one the predicate does not
     * register
     */
    private static boolean isUnknownBlock(String blockPath, Predicate<String> registeredBlock) {
        SettingsBlockPath path = new SettingsBlockPath(blockPath);
        if (path.kind() == null) {
            return true;
        }
        try {
            return !registeredBlock.test(path.path());
        } catch (RuntimeException e) {
            // A throwing predicate must not make us override a known block.
            logger.warn("Settings block registration check failed for '{}'; treating it as known", blockPath, e);
            return false;
        }
    }

    /**
     * Reads a sibling {@code settings.json}. A missing, unreadable, or
     * non-object file is logged and skipped — sibling profiles are read-only
     * for this class, so nothing is renamed or backed up here.
     *
     * @return the parsed object, or {@code null} if it cannot be used
     */
    private static JsonNode readSibling(Path siblingFile) {
        if (!Files.isRegularFile(siblingFile)) {
            return null;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(Files.readString(siblingFile, StandardCharsets.UTF_8));
            if (node == null || !node.isObject()) {
                logger.warn("Skipping sibling settings file {}: not a settings JSON object", siblingFile);
                return null;
            }
            return node;
        } catch (IOException | RuntimeException e) {
            logger.warn("Skipping unreadable sibling settings file {}: {}", siblingFile, e.toString());
            return null;
        }
    }

    private static void stampMeta(SettingsDocument document, String currentVersion) {
        document.setSchemaVersion(SCHEMA_VERSION);
        ObjectNode meta = document.meta();
        meta.put("written-by-version", currentVersion);
        meta.put("written-at", META_TIMESTAMP.format(Instant.now()));
        meta.put("writer-id", document.writerId());
    }
}
