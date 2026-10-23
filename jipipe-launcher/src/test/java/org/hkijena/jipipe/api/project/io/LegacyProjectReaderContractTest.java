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

package org.hkijena.jipipe.api.project.io;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for {@link JIPipeLegacyProjectReader}.
 * <p>
 * The legacy reader must reproduce the exact load semantics of the former
 * inline {@code JIPipeProject.loadProject} implementation (JIPX phase 0 is a
 * pure, behavior-preserving extraction): the loaded project reports the
 * source file as its project file and the file's parent folder as its work
 * directory, and it loads the fixture's nodes unchanged.
 * <p>
 * <b>Scope: jipipe-launcher.</b> The fixture references the node id
 * {@code ij1-blur-gaussian2d} from the ij-algorithms plugin, so — like
 * {@code FixtureProjectsLoadTest} — this test must live in the launcher
 * (the only module with all plugin modules on its classpath). On
 * jipipe-core's classpath {@code nodesFromJson} would silently skip the
 * fixture node and the node-count assertion would pass vacuously.
 * <b>The count assertion is the non-vacuity guard.</b>
 * <p>
 * <b>.jip vs .jipipe:</b> the legacy reader only claims the {@code .jip}
 * extension in {@link JIPipeLegacyProjectReader#supports(Path)}, but its
 * {@code read(...)} deliberately does not gate on the extension — exactly
 * like the former inline {@code JIPipeProject.loadProject}, which is why the
 * {@code .jipipe} fixtures load through it. A future reader (e.g. the JIPX
 * format) is the only component allowed to use {@code supports()} for
 * dispatch.
 */
class LegacyProjectReaderContractTest {

    private static final String FIXTURE = "/projects/5.3.0-taxonomy-fixtures/basic-images.jipipe";

    @TempDir
    Path tmp;

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @Test
    void readerAppliesLegacyLoadSemantics() throws IOException, URISyntaxException {
        Path path = Path.of(LegacyProjectReaderContractTest.class.getResource(FIXTURE).toURI());
        JIPipeLegacyProjectReader reader = new JIPipeLegacyProjectReader();

        // (0) Reader identity and capability: stable string-literal id and
        // a single "jip" extension.
        assertEquals("org.hkijena.jipipe.core:jip", reader.getId());
        assertEquals(List.of("jip"), reader.getFileExtensions());

        // (0b) supports() gates on the .jip extension (case-insensitive) and
        // on regular files — NOT on the fixture's .jipipe extension.
        Path jip = tmp.resolve("probe.jip");
        Files.createFile(jip);
        Path jipUpper = tmp.resolve("PROBE.JIP");
        Files.createFile(jipUpper);
        Path jipipe = tmp.resolve("probe.jipipe");
        Files.createFile(jipipe);
        Path dir = tmp.resolve("a-dir");
        Files.createDirectory(dir);
        assertTrue(reader.supports(jip), "reader must support .jip files");
        assertTrue(reader.supports(jipUpper), "extension check must be case-insensitive");
        assertFalse(reader.supports(jipipe), "reader must not claim the .jipipe extension");
        assertFalse(reader.supports(dir), "reader must not support directories");

        // (1) The 5-arg reader call — same shape as the former 5-arg
        // JIPipeProject.loadProject: must not throw.
        JIPipeProject project = reader.read(path,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);

        // (2) The project file is recorded as the loaded path.
        assertEquals(path, project.getProjectFile(), "projectFile must be the loaded path");

        // (3) The work directory is set to the file's parent folder.
        assertEquals(path.getParent(), project.getWorkDirectory(), "workDirectory must be the file's parent folder");

        List<JIPipeGraphNode> nodes = project.getGraph().traverse();

        // (4) CRITICAL (non-vacuous): the fixture has exactly one pipeline
        // node. nodesFromJson silently skips unknown node ids, so a 0-node
        // load MUST fail.
        assertEquals(1, nodes.size(),
                "Expected 1 loaded node but got " + nodes.size()
                        + " — node silently skipped (unknown id or missing compartment?)");

        // (5) The single node is the fixture's gaussian blur node.
        assertEquals("ij1-blur-gaussian2d", nodes.get(0).getInfo().getId(),
                "Unexpected node id in loaded graph");
    }
}
