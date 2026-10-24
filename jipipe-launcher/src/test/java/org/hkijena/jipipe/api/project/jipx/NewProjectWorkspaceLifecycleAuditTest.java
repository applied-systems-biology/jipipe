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

package org.hkijena.jipipe.api.project.jipx;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.PathUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SAFETY AUDIT (feature #753): the project lifecycle with respect to payload
 * files stored in the {@code jipx-workspace}, and the cleanup of temporary
 * directories across the lifecycle.
 *
 * <ul>
 *     <li><b>Payload safety (only-compress):</b> payload files
 *     ({@code data/...}) placed in the workspace survive every re-save and
 *     ride along into every container written afterwards, whatever happens
 *     to the project's work directory.</li>
 *     <li><b>Workspace binding:</b> the workspace binds on first access and
 *     is never re-bound or moved — payload content can never be lost by a
 *     directory change.</li>
 *     <li><b>Temp-base cleanup:</b> closing the project deletes EVERY
 *     temporary base it ever used. The temp base re-resolves when the work
 *     directory changes (e.g. "Save As" into another directory — the
 *     migration flow); without tracking, the earlier base (with the
 *     workspace and its payloads) would leak on disk.</li>
 * </ul>
 */
public class NewProjectWorkspaceLifecycleAuditTest {

    @BeforeAll
    public static void ensureInstance() {
        JIPipe.ensureInstance();
    }

    /**
     * The new-project → first-save lifecycle: the workspace binds AFTER the
     * save's {@code setWorkDirectory} pre-step (nothing touches it earlier),
     * a payload added between saves is preserved (only-compress), and close
     * cleans the temp base.
     */
    @Test
    public void newProjectFirstSaveKeepsPayloads(@TempDir Path dir) throws Exception {
        JIPipeProject project = new JIPipeProject();
        assertNull(project.getWorkDirectory());

        // The save pre-step: work directory set before the first workspace
        // access (temp-base memoization contract).
        Path target = dir.resolve("p.jipx");
        project.setWorkDirectory(target.getParent());
        new JIPipeXProjectWriter().write(project, target, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(target));

        // The workspace lives under the SAVE directory's temp base.
        Path workspaceDir = project.getWorkspace().getBaseDirectory();
        assertTrue(workspaceDir.startsWith(dir));

        // A payload added between saves (the future data system's
        // Files.copy story) must ride along into the next container.
        Path payload = workspaceDir.resolve("data").resolve("payload.tiff");
        PathUtils.createDirectories(payload.getParent());
        Files.write(payload, "PAYLOAD-CONTENT-1".getBytes(StandardCharsets.UTF_8));

        new JIPipeXProjectWriter().write(project, target, JIPipeProgressInfo.SILENT);
        assertTrue(zipContains(target, "data/payload.tiff"));
        assertEquals("PAYLOAD-CONTENT-1", new String(extractZipEntry(target, "data/payload.tiff"), StandardCharsets.UTF_8));

        // Closing the project cleans its temp base (default setting).
        Path tempBase = project.getTemporaryBaseDirectory();
        assertTrue(tempBase.startsWith(dir));
        project.close(JIPipeProgressInfo.SILENT);
        assertFalse(Files.exists(tempBase), "close() must delete the project's temporary base");
    }

    /**
     * The migration flow: a .jipx opened in one directory, then saved
     * ("Save As") into ANOTHER directory. The workspace (with its payloads)
     * stays where it bound — the payloads still ride along into the new
     * container — and close() must clean BOTH directories' temp trees.
     */
    @Test
    public void saveAsToOtherDirectoryKeepsPayloadsAndCleansUp(@TempDir Path dir) throws Exception {
        Path dir1 = dir.resolve("old");
        Path dir2 = dir.resolve("new");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        // Create a .jipx with a payload in dir1.
        JIPipeProject creator = new JIPipeProject();
        Path source = dir1.resolve("proj.jipx");
        creator.setWorkDirectory(dir1);
        new JIPipeXProjectWriter().write(creator, source, JIPipeProgressInfo.SILENT);
        Path payload = creator.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.tiff");
        PathUtils.createDirectories(payload.getParent());
        Files.write(payload, "PAYLOAD-CONTENT-2".getBytes(StandardCharsets.UTF_8));
        new JIPipeXProjectWriter().write(creator, source, JIPipeProgressInfo.SILENT);
        creator.close(JIPipeProgressInfo.SILENT);

        // Open it (the JIPX reader extracts the workspace + payload under
        // dir1's temp base).
        JIPipeProject opened = JIPipeProject.loadProject(source,
                new UnspecifiedValidationReportContext(),
                new JIPipeValidationReport(),
                new JIPipeNotificationInbox(),
                JIPipeProgressInfo.SILENT);
        Path openedPayload = opened.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.tiff");
        assertTrue(Files.isRegularFile(openedPayload), "the payload must be extracted into the workspace on open");

        // "Save As" into dir2: the work directory changes — the workspace
        // must NOT re-bind or lose the payload (only-compress).
        Path openedWorkspaceBefore = opened.getWorkspace().getBaseDirectory();
        opened.setWorkDirectory(dir2);
        Path target = dir2.resolve("proj.jipx");
        new JIPipeXProjectWriter().write(opened, target, JIPipeProgressInfo.SILENT);
        assertEquals(openedWorkspaceBefore, opened.getWorkspace().getBaseDirectory(),
                "the workspace must stay bound across a work-directory change (payloads never move)");
        assertTrue(zipContains(target, "data/payload.tiff"), "the payload must ride along into the new container");
        assertEquals("PAYLOAD-CONTENT-2", new String(extractZipEntry(target, "data/payload.tiff"), StandardCharsets.UTF_8));

        // Closing must clean up BOTH directories' temporary trees — the
        // temp base re-resolved to dir2 when the work directory changed,
        // but the dir1 workspace (with the payload) must not leak.
        opened.close(JIPipeProgressInfo.SILENT);
        assertTrue(listWorkspaceDirs(dir1).isEmpty(),
                "close() must also clean the old directory's temp tree (the workspace lived there)");
        assertTrue(listWorkspaceDirs(dir2).isEmpty(),
                "close() must clean the new directory's temp tree");
    }

    /**
     * A workspace accessed BEFORE the work directory is set (possible for
     * future systems that touch the workspace pre-save) binds to the GLOBAL
     * temp base. The container must still be written correctly, and close()
     * must clean the global base too.
     */
    @Test
    public void preSaveWorkspaceAccessBindsToGlobalTempAndIsCleaned(@TempDir Path dir) throws Exception {
        JIPipeProject project = new JIPipeProject();
        Path workspaceDir = project.getWorkspace().getBaseDirectory();
        assertFalse(workspaceDir.startsWith(dir), "precondition: bound to the global temp base");

        // First save after the binding: content must still be correct.
        Path target = dir.resolve("p.jipx");
        project.setWorkDirectory(target.getParent());
        new JIPipeXProjectWriter().write(project, target, JIPipeProgressInfo.SILENT);

        Path payload = workspaceDir.resolve("data").resolve("payload.tiff");
        PathUtils.createDirectories(payload.getParent());
        Files.write(payload, "PAYLOAD-CONTENT-3".getBytes(StandardCharsets.UTF_8));
        new JIPipeXProjectWriter().write(project, target, JIPipeProgressInfo.SILENT);
        assertTrue(zipContains(target, "data/payload.tiff"));

        // The workspace stays bound to the global base (payloads never
        // move) — and close() must clean it up.
        assertEquals(workspaceDir, project.getWorkspace().getBaseDirectory());
        project.close(JIPipeProgressInfo.SILENT);
        assertFalse(Files.exists(workspaceDir), "close() must clean the globally-bound workspace");
    }

    /**
     * Lists all jipx-workspace directories under a directory tree
     * (recursively) — used to detect leaked workspaces.
     */
    private static List<Path> listWorkspaceDirs(Path root) throws Exception {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.getFileName() != null
                            && "jipx-workspace".equals(p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    private static boolean zipContains(Path zip, String entryName) throws Exception {
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zip.toFile())) {
            return zf.getEntry(entryName) != null;
        }
    }

    private static byte[] extractZipEntry(Path zip, String entryName) throws Exception {
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zip.toFile())) {
            java.util.zip.ZipEntry entry = zf.getEntry(entryName);
            assertNotNull(entry);
            return zf.getInputStream(entry).readAllBytes();
        }
    }
}
