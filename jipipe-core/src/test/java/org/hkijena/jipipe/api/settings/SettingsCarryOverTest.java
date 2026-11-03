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
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsCarryOverTest {
    @TempDir Path tempDir;

    private static JsonNode read(Path file) throws Exception {
        return JsonUtils.getObjectMapper().readTree(Files.readString(file));
    }

    @Test void recoversUnknownBlocksFromNewerProfile() throws Exception {
        Path base = tempDir.resolve("profiles");
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(current.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}}");
        Files.writeString(newer.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Light\"}," +
                "\"sheet:new-feature-x\":{\"on\":true}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.startsWith("sheet:new-feature-x"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(current.resolve("settings.json")));
        assertEquals("Dark", global.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText(),
                "a registered block must never be overridden by another version");
        assertTrue(global.get("sheet:new-feature-x").get("on").asBoolean());
    }

    @Test void noSiblingProfilesIsANoOp() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles/6.0.0"));
        Path f = base.resolve("settings.json");
        assertEquals(Set.of(), SettingsCarryOver.recoverUnknownBlocks(f, tempDir.resolve("profiles"), "6.0.0", p -> true));
    }

    @Test void newestSiblingWinsForTheSameUnknownBlock() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path older = Files.createDirectories(base.resolve("6.0.5"));
        Path newest = Files.createDirectories(base.resolve("6.2.0"));
        Files.writeString(older.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":false,\"by\":\"6.0.5\"}}");
        Files.writeString(newest.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true,\"by\":\"6.2.0\"}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        JsonNode block = read(current.resolve("settings.json")).get("sheet:new-feature-x");
        assertEquals("6.2.0", block.get("by").asText(), "the newest sibling must win");
    }

    @Test void registeredBlockAbsentLocallyIsNotPulledFromASibling() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(current.resolve("settings.json"), "{\"$schema\":2}");
        Files.writeString(newer.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Light\"}," +
                "\"sheet:new-feature-x\":{\"on\":true}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0",
                p -> p.equals("sheet:org.hkijena.jipipe:general-ui"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        JsonNode global = read(current.resolve("settings.json"));
        assertFalse(global.has("sheet:org.hkijena.jipipe:general-ui"),
                "a registered block is never recovered from a sibling");
        assertTrue(global.has("sheet:new-feature-x"),
                "the sibling's unregistered block IS recovered: the registration predicate is consulted, "
                        + "not merely the local absence");
    }

    @Test void registeredBlockPresentLocallyKeepsItsLocalValue() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(current.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}}");
        Files.writeString(newer.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Light\"}}");

        // The predicate says this block is registered, so only the local value may survive.
        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0",
                p -> p.equals("sheet:org.hkijena.jipipe:general-ui"));

        assertEquals(Set.of(), recovered, "nothing is recovered: the block is registered and present locally");
        assertEquals("Dark",
                read(current.resolve("settings.json")).get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText(),
                "the local value wins over a registered block's sibling value, independent of the registration check");
    }

    @Test void numericVersionOrderDecidesTheWinner() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path lowestButLongest = Files.createDirectories(base.resolve("6.9.0"));
        Path numericallyNewest = Files.createDirectories(base.resolve("6.10.0"));
        Files.writeString(lowestButLongest.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:new-feature-x\":{\"by\":\"6.9.0\"}}");
        Files.writeString(numericallyNewest.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:new-feature-x\":{\"by\":\"6.10.0\"}}");

        // The predicate is deliberately NOT consulted for the kind here: both dirs hold the same
        // unknown block, so only the version ordering can decide. 6.10.0 must beat 6.9.0
        // (a plain string comparison would pick "6.9.0" > "6.10.0").
        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        assertEquals("6.10.0",
                read(current.resolve("settings.json")).get("sheet:new-feature-x").get("by").asText(),
                "versions must be compared numerically: 6.10.0 is newer than 6.9.0");
    }

    @Test void corruptAndUnreadableSiblingsAreSkipped() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newest = Files.createDirectories(base.resolve("7.0.0"));
        Path older = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(newest.resolve("settings.json"), "{ this is not json");
        Files.writeString(newest.resolve("settings-dir"), "");
        Files.createDirectories(newest.resolve("not-a-profile-file"));
        Files.writeString(older.resolve("settings.json"), "{\"$schema\":2,\"state:from-older\":{\"v\":1}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("state:from-older"));

        assertEquals(Set.of("state:from-older"), recovered,
                "a corrupt newest sibling is skipped, older siblings are still scanned");
        assertFalse(Files.exists(current.resolve("settings.json.bad-0")), "no bogus backup naming");
    }

    @Test void createsTheCurrentFileWhenMissingAndStampsMeta() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(newer.resolve("settings.json"), "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        JsonNode global = read(current.resolve("settings.json"));
        assertEquals(2, global.get("$schema").asInt());
        assertEquals("6.0.0", global.get("$meta").get("written-by-version").asText());
        assertTrue(global.get("$meta").hasNonNull("written-at"));
        assertTrue(global.get("$meta").get("writer-id").asText().length() > 0);
    }

    @Test void corruptCurrentFileIsBackedUpAndStillRecovered() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(current.resolve("settings.json"), "{ truncated");
        Files.writeString(newer.resolve("settings.json"), "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true}}");

        Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x"));

        assertEquals(Set.of("sheet:new-feature-x"), recovered);
        try (var s = Files.list(current)) {
            assertTrue(s.anyMatch(f -> f.getFileName().toString().contains(".bad-")),
                    "the corrupt file must be preserved as a .bad- backup");
        }
        assertTrue(read(current.resolve("settings.json")).get("sheet:new-feature-x").get("on").asBoolean());
    }

    @Test void siblingProfilesAreNeverModified() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        String newerContent = "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true}}";
        Files.writeString(newer.resolve("settings.json"), newerContent);
        var modified = Files.getLastModifiedTime(newer.resolve("settings.json"));

        SettingsCarryOver.recoverUnknownBlocks(current.resolve("settings.json"), base, "6.0.0",
                p -> !p.equals("sheet:new-feature-x"));

        assertEquals(newerContent, Files.readString(newer.resolve("settings.json")),
                "a sibling profile file must be read-only for the carry-over");
        assertEquals(modified, Files.getLastModifiedTime(newer.resolve("settings.json")));
        try (var s = Files.list(newer)) {
            assertEquals(1, s.count(), "nothing may be created in a sibling profile directory");
        }
    }

    @Test void isIdempotent() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(base.resolve("6.0.0"));
        Path newer = Files.createDirectories(base.resolve("6.1.0"));
        Files.writeString(newer.resolve("settings.json"), "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true}}");

        assertEquals(Set.of("sheet:new-feature-x"), SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x")));
        assertEquals(Set.of(), SettingsCarryOver.recoverUnknownBlocks(
                current.resolve("settings.json"), base, "6.0.0", p -> !p.equals("sheet:new-feature-x")),
                "a recovered block is now known to the current document and is not recovered twice");
    }

    @Test void componentRecordsRecoveredBlocksByVersionOnce() throws Exception {
        Path profiles = Files.createDirectories(tempDir.resolve("profiles"));
        Path current = Files.createDirectories(profiles.resolve("6.0.0"));
        Path newer = Files.createDirectories(profiles.resolve("6.1.0"));
        Files.writeString(newer.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:new-feature-x\":{\"on\":true},\"registry:new-db\":{\"a\":1}}");

        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(current);
        try {
            assertEquals(Set.of("sheet:new-feature-x", "registry:new-db"), component.runCarryOver());
            Map<String, String> recovered = component.getRecoveredBlocks();
            assertEquals("6.1.0", recovered.get("sheet:new-feature-x"));
            assertEquals("6.1.0", recovered.get("registry:new-db"));

            // Second run: the blocks are present locally now, so nothing new.
            assertEquals(Set.of(), component.runCarryOver());
            assertEquals(2, component.getRecoveredBlocks().size());
        } finally {
            component.dispose();
        }
    }
}
