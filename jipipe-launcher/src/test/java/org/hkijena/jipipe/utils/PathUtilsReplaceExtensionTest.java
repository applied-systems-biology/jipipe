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

package org.hkijena.jipipe.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test for {@link PathUtils#replaceExtension(Path, String, String)} —
 * the root-cause helper of the JIPX migration prompt bug (#753): the method
 * must replace the extension only when the file name actually ends with
 * {@code oldExtension} (case-insensitive), preserve the base name's original
 * casing, and be null-safe for both the path and a missing file name.
 * <p>
 * Pure path logic — no JIPipe instance required.
 */
public class PathUtilsReplaceExtensionTest {

    // ------------------------------------------------------------------
    // replacement
    // ------------------------------------------------------------------

    @Test
    public void replacesLegacyJipExtension() {
        assertEquals(Paths.get("x.jipx"),
                PathUtils.replaceExtension(Paths.get("x.jip"), ".jip", ".jipx"));
    }

    @Test
    public void replacesUppercaseExtensionLengthBased() {
        // PathUtils.replaceExtension lowercases the file name for the match
        // (PathUtils.java:652), but strips by LENGTH (PathUtils.java:656):
        // "x.JIP".length() - ".jip".length() = 1, so the last 4 chars
        // (".JIP") are removed and the new extension is appended.
        // Result: base name "x" keeps its casing, the uppercase extension
        // itself is NOT preserved — the output is fully "x.jipx".
        assertEquals(Paths.get("x.jipx"),
                PathUtils.replaceExtension(Paths.get("x.JIP"), ".jip", ".jipx"));
    }

    @Test
    public void preservesBaseNameCasingWithParent() {
        assertEquals(Paths.get("dir/sub/MyProj.jipx"),
                PathUtils.replaceExtension(Paths.get("dir/sub/MyProj.jip"), ".jip", ".jipx"));
    }

    // ------------------------------------------------------------------
    // no-op cases: the method only replaces when the old extension matches
    // ------------------------------------------------------------------

    @Test
    public void jipxIsNotTreatedAsLegacyJip() {
        // ".jipx" must not match old=".jip" (unlike ensureExtension)
        Path jipx = Paths.get("x.jipx");
        assertSame(jipx, PathUtils.replaceExtension(jipx, ".jip", ".jipx"));
    }

    @Test
    public void unrelatedExtensionIsUnchanged() {
        Path json = Paths.get("x.json");
        assertSame(json, PathUtils.replaceExtension(json, ".jip", ".jipx"));
    }

    // ------------------------------------------------------------------
    // null-safety / degenerate paths
    // ------------------------------------------------------------------

    @Test
    public void nullPathReturnsNull() {
        assertNull(PathUtils.replaceExtension(null, ".jip", ".jipx"));
    }

    @Test
    public void pathWithoutFileNameReturnsUnchanged() {
        // Root directory has no file name (PathUtils.java:648-649):
        // returned as-is, no exception.
        Path root = Paths.get("/");
        assertSame(root, PathUtils.replaceExtension(root, ".jip", ".jipx"));
    }
}
