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

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for {@link JIPipeLegacyProjectWriter}.
 * <p>
 * The legacy writer must reproduce the exact save semantics of the former
 * inline {@code JIPipeProject.saveProject} implementation (JIPX phase 0 is a
 * pure, behavior-preserving extraction): the Path variant pretty-prints the
 * project after merging authors from the global list (when the project's
 * metadata allows it); the {@code Writer}/{@code OutputStream} variants
 * pretty-print only - no authors merge. This asymmetry is intentional and
 * must be preserved.
 * <p>
 * <b>Scope: jipipe-launcher.</b> The fixture references the node id
 * {@code ij1-blur-gaussian2d} from the ij-algorithms plugin, so - like
 * {@code FixtureProjectsLoadTest} - this test must live in the launcher
 * (the only module with all plugin modules on its classpath). On
 * jipipe-core's classpath {@code nodesFromJson} would silently skip the
 * fixture node and the mapping assertion would pass vacuously.
 * <b>The uuid → node-info-id mapping assertion is the non-vacuity guard.</b>
 */
class LegacyProjectWriterContractTest {

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

    private static Map<UUID, String> nodeIdMapping(JIPipeGraph graph) {
        Map<UUID, String> mapping = new HashMap<>();
        for (JIPipeGraphNode n : graph.getGraphNodes()) {
            mapping.put(n.getUUIDInParentGraph(), n.getInfo().getId());
        }
        return mapping;
    }

    @Test
    void writerWritesLegacyProjectFormat() throws IOException, URISyntaxException {
        Path fixture = Path.of(LegacyProjectWriterContractTest.class.getResource(FIXTURE).toURI());
        JIPipeProject p = JIPipeProject.loadProject(fixture,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);

        // (0) Writer identity: same stable format id and extensions as the
        // legacy reader (the format pair is dispatched on these).
        JIPipeLegacyProjectWriter writer = new JIPipeLegacyProjectWriter();
        assertEquals(JIPipeLegacyProjectReader.FORMAT_ID, writer.getId());
        assertEquals(List.of("jip"), writer.getFileExtensions());

        // (1) The Path contract: write the project to a file with the
        // 3-arg write(project, target, progressInfo).
        Path written = tmp.resolve("written.jip");
        writer.write(p, written, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(written), "target file must be written");

        // (2) The file parses as JSON and carries the "project" type
        // marker (not, e.g., the "compartment" marker of an exported
        // compartment file).
        JsonNode json = JsonUtils.getObjectMapper().readValue(written.toFile(), JsonNode.class);
        assertEquals("project", json.path("jipipe:project-type").asText(),
                "Written JSON must carry the \"project\" type marker");

        // (2b) The OutputStream contract: the 2-arg
        // write(project, outputStream) pretty-prints to the given stream -
        // no authors merge (legacy asymmetry), no file side effects.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(p, out);
        out.close();
        JsonNode streamJson = JsonUtils.getObjectMapper().readTree(out.toByteArray());
        assertEquals("project", streamJson.path("jipipe:project-type").asText(),
                "Stream-written JSON must carry the \"project\" type marker");

        // (3) Round-trip: reload through the legacy reader and compare the
        // uuid → node-info-id mapping. CRITICAL (non-vacuous):
        // nodesFromJson silently skips unknown node ids, so any node lost in
        // the write would drop the mapping below the fixture's entries (the
        // pipeline node and the root compartment) and this assertion would
        // fail.
        JIPipeProject p2 = new JIPipeLegacyProjectReader().read(written,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);
        assertEquals(nodeIdMapping(p.getGraph()), nodeIdMapping(p2.getGraph()),
                "Round-tripped project does not preserve the uuid → node-info-id mapping");
    }

    @Test
    void saveProjectUpdateSavePathContract() throws IOException, URISyntaxException {
        Path fixture = Path.of(LegacyProjectWriterContractTest.class.getResource(FIXTURE).toURI());
        JIPipeProject p = JIPipeProject.loadProject(fixture,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);
        assertEquals(fixture, p.getProjectFile());

        // updateSavePath = false: the internally tracked project file stays
        // unchanged. The writer itself never touches it - only the
        // JIPipeProject.saveProject(Path, boolean) delegate applies the
        // update when requested.
        Path target = tmp.resolve("target.jip");
        p.saveProject(target, false);
        assertTrue(Files.isRegularFile(target), "target file must be written");
        assertEquals(fixture, p.getProjectFile(),
                "saveProject(target, false) must not change the tracked project file");

        // updateSavePath = true: the tracked project file becomes the new
        // target.
        Path target2 = tmp.resolve("target2.jip");
        p.saveProject(target2, true);
        assertTrue(Files.isRegularFile(target2), "target file must be written");
        assertEquals(target2, p.getProjectFile(),
                "saveProject(target2, true) must update the tracked project file");
    }
}
