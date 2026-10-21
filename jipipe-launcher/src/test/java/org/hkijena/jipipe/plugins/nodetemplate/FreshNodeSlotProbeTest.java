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

package org.hkijena.jipipe.plugins.nodetemplate;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Dumper for the bundled System patterns' FRESH-node slot sets (Task 2, B1
 * restore). The registry dump ({@code utils/taxonomy-research/registry.json})
 * lists the {@code @AddJIPipe*Slot} ANNOTATION slots, but a fresh node loads
 * only what its constructor creates (a slot-config-less minimal-format node
 * gets no further slots from {@code fromJson}) — so the FRESH slot set can
 * differ: the cellpose inference nodes gate their extra outputs behind
 * parameters (default: Labels + ROI only), some types create slots in
 * onDeserialized, and single-class nodes derive their slot name from the
 * class (an empty annotation name).
 * <p>
 * The B1 restore pass ({@code utils/restore_pattern_slot_configs.py}) matches
 * each shipped node's required (edge-referenced) slot set against the FRESH
 * set — the world the launcher edge test sees — so this dump is the
 * generator-side oracle. It scans the BUNDLED pattern resources (via the
 * committed {@code system-templates.json} sidecar, self-contained — no
 * external input), collects the distinct node types, instantiates each fresh
 * node, and writes its input/output slot names to
 * {@code utils/fresh-node-slots.json}.
 * <p>
 * Types that cannot be instantiated in the test JVM (the version-gated
 * cLij2 kernels — dropped on load anyway, so their edges are never counted
 * by the edge test) are marked {@code uninstantiable} and the consumer falls
 * back to the registry annotation list.
 * <p>
 * Mirrors {@link RegistryDumperTest}'s bootstrap (ImageJ legacy-patcher
 * LauncherSessionListener preinit) and its determinism (sorted iteration,
 * byte-stable JSON).
 */
class FreshNodeSlotProbeTest {

    private static final String PATTERN_RESOURCE_DIR =
        "/org/hkijena/jipipe/plugins/nodetemplate/patterns/";
    private static final String SIDECAR_RESOURCE = PATTERN_RESOURCE_DIR + "system-templates.json";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe.
        JIPipe.ensureInstance();
    }

    @Test
    void dumpFreshNodeSlots() throws Exception {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();

        // The distinct node types referenced by the bundled patterns (via the
        // committed sidecar — self-contained, no external input).
        Set<String> types = new TreeSet<>();
        JsonNode sidecar = JsonUtils.getObjectMapper().readTree(readResource(SIDECAR_RESOURCE));
        for (JsonNode entry : sidecar) {
            String file = entry.get("file").asText();
            JsonNode graph = JsonUtils.getObjectMapper().readTree(readResource(PATTERN_RESOURCE_DIR + file));
            for (JsonNode node : graph.get("nodes")) {
                types.add(node.get("jipipe:node-info-id").asText());
            }
        }

        TreeMap<String, Object> out = new TreeMap<>();
        for (String id : types) {
            TreeMap<String, Object> entry = new TreeMap<>();
            try {
                JIPipeNodeInfo info = nodes.getInfoById(id);
                if (info == null) {
                    // not registered in this JVM (the version-gated cLij2
                    // kernels) — dropped on load; the consumer falls back to
                    // the registry annotation list.
                    entry.put("uninstantiable", true);
                } else {
                    JIPipeGraphNode node = info.newInstance();
                    List<String> ins = new ArrayList<>();
                    for (JIPipeDataSlot s : node.getInputSlots()) ins.add(s.getName());
                    List<String> outs = new ArrayList<>();
                    for (JIPipeDataSlot s : node.getOutputSlots()) outs.add(s.getName());
                    entry.put("inputs", ins);
                    entry.put("outputs", outs);
                }
            } catch (Throwable t) {
                // A fresh instance cannot be created in this JVM — fall back
                // to the registry annotation list on the consumer side.
                entry.put("inputs", new ArrayList<>());
                entry.put("outputs", new ArrayList<>());
                entry.put("uninstantiable", true);
            }
            out.put(id, entry);
        }

        // The test runs with working dir jipipe-launcher/; resolve to the repo
        // root and write (like RegistryDumperTest writes registry.json).
        Path dst = Path.of("..", "utils", "fresh-node-slots.json").toAbsolutePath().normalize();
        Files.createDirectories(dst.getParent());
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dst.toFile(), out);
        System.out.println("FreshNodeSlotProbe: " + out.size()
            + " bundled pattern node types -> " + dst);
    }

    /** Reads a bundled resource from the classpath as a UTF-8 string. */
    private static String readResource(String path) throws Exception {
        try (InputStream in = FreshNodeSlotProbeTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new AssertionError("resource not found on the classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
