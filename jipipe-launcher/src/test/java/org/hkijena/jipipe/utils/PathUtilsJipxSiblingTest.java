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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link PathUtils#findJipxSibling(Path)} — the pure file-logic
 * that decides whether a legacy {@code .jip} open should offer the
 * JIPipe 6 ({@code .jipx}) sibling instead.
 * <p>
 * The offer is purely EXISTENCE-based (no last-modified comparison):
 * mtimes are an unreliable signal because projects are frequently
 * copied between NAS shares, SFTP and USB drives.
 */
public class PathUtilsJipxSiblingTest {

    @Test
    public void returnsExistingSibling(@TempDir Path dir) throws Exception {
        Path jip = dir.resolve("proj.jip");
        Path jipx = dir.resolve("proj.jipx");
        Files.createFile(jip);
        Files.createFile(jipx);
        assertEquals(jipx, PathUtils.findJipxSibling(jip));
    }

    @Test
    public void returnsNullWhenNoSibling(@TempDir Path dir) throws Exception {
        Path jip = dir.resolve("proj.jip");
        Files.createFile(jip);
        assertNull(PathUtils.findJipxSibling(jip));
    }

    @Test
    public void returnsNullForNonJipInput(@TempDir Path dir) throws Exception {
        Path jipx = dir.resolve("proj.jipx");
        Files.createFile(jipx);
        assertNull(PathUtils.findJipxSibling(jipx));

        Path json = dir.resolve("proj.json");
        Files.createFile(json);
        assertNull(PathUtils.findJipxSibling(json));
    }

    @Test
    public void returnsNullForNullInput() {
        assertNull(PathUtils.findJipxSibling(null));
    }

    @Test
    public void matchesInputExtensionCaseInsensitively(@TempDir Path dir) throws Exception {
        // "PROJ.JIP" must be recognized as a legacy project and paired
        // with the sibling the save prompt creates for it ("PROJ.jipx").
        Path jip = dir.resolve("PROJ.JIP");
        Path jipx = dir.resolve("PROJ.jipx");
        Files.createFile(jip);
        Files.createFile(jipx);
        assertEquals(jipx, PathUtils.findJipxSibling(jip));
    }

    @Test
    public void keepsTheParentOfNestedPaths(@TempDir Path dir) throws Exception {
        Path sub = dir.resolve("deep").resolve("nested");
        Files.createDirectories(sub);
        Path jip = sub.resolve("proj.jip");
        Path jipx = sub.resolve("proj.jipx");
        Files.createFile(jip);
        Files.createFile(jipx);
        assertEquals(jipx, PathUtils.findJipxSibling(jip));
    }

    @Test
    public void returnsNullWhenJipxIsADirectory(@TempDir Path dir) throws Exception {
        Path jip = dir.resolve("proj.jip");
        Files.createFile(jip);
        Files.createDirectories(dir.resolve("proj.jipx"));
        assertNull(PathUtils.findJipxSibling(jip));
    }
}
