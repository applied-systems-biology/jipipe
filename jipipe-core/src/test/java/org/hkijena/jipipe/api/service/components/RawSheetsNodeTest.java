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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.settings.SettingsMigration;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The file-shape contract of the raw settings reader (Task 13 review,
 * Finding 1), tested on the exact normalisation the reader applies so it does
 * not need the JIPipe service or a profile override.
 * <p>
 * The reader is file-authoritative: a parseable file's {@code sheet:} blocks
 * are what raw readers see, and a legacy (5.x flat, unprefixed) file yields
 * <b>none</b> — i.e. before the profile migration has run, the raw readers see
 * defaults. That is why every raw reader must run after the startup
 * preparation, and why the reader must not fall through to the (pre-migration)
 * in-memory document for a legacy file.
 */
class RawSheetsNodeTest {

    private static final String GENERAL_UI = "org.hkijena.jipipe:general-ui";

    @TempDir
    Path tempProfile;

    @Test
    void newFormatDocumentExposesItsSheetBlocks() throws Exception {
        JsonNode document = JsonUtils.getObjectMapper().readTree("""
                {"$schema": 2, "$meta": {},
                 "sheet:org.hkijena.jipipe:general-ui": {"theme": "Dark"},
                 "registry:node-db": {"a": 1},
                 "state:recent-projects": ["/x.jip"]}""");
        JsonNode raw = JIPipeApplicationSettingsServiceComponent.rawSheetsOf(document);
        assertEquals(1, raw.size(), "only sheet: blocks are exposed");
        assertEquals("Dark", raw.path(GENERAL_UI).path("theme").asText());
        assertFalse(raw.has("node-db"), "registry blocks are not sheets");
    }

    /**
     * A 5.x flat file has no {@code sheet:} blocks, so the raw reader yields an
     * empty object — the documented pre-migration blindness.
     */
    @Test
    void legacyFlatFileExposesNoSheetBlocksUntilMigrated() throws Exception {
        Files.writeString(tempProfile.resolve("settings.json"),
                "{\"" + GENERAL_UI + "\":{\"theme\":\"Dark\"}}");
        assertTrue(SettingsMigration.needsMigration(tempProfile), "the fixture is a legacy profile");

        JsonNode legacy = JsonUtils.getObjectMapper().readTree(
                Files.readString(tempProfile.resolve("settings.json")));
        JsonNode rawBefore = JIPipeApplicationSettingsServiceComponent.rawSheetsOf(legacy);
        assertEquals(0, rawBefore.size(),
                "a legacy flat file exposes no sheet blocks: raw readers see defaults before the migration");

        SettingsMigration.migrateLegacyProfile(tempProfile, path -> true, Map.of(), JIPipeProgressInfo.SILENT);

        JsonNode migrated = JsonUtils.getObjectMapper().readTree(
                Files.readString(tempProfile.resolve("settings.json")));
        JsonNode rawAfter = JIPipeApplicationSettingsServiceComponent.rawSheetsOf(migrated);
        assertEquals("Dark", rawAfter.path(GENERAL_UI).path("theme").asText(),
                "after the migration the same normalisation exposes the sheet values");
        assertFalse(SettingsMigration.needsMigration(tempProfile));
    }

    /**
     * A non-object / absent file yields no sheets at all (the reader returns a
     * {@code MissingNode} instead; the normalisation of an absent document is
     * simply empty).
     */
    @Test
    void nonObjectDocumentExposesNothing() throws Exception {
        JsonNode array = JsonUtils.getObjectMapper().readTree("[1, 2, 3]");
        assertEquals(0, JIPipeApplicationSettingsServiceComponent.rawSheetsOf(array).size());
    }
}