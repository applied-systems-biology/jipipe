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

package org.hkijena.jipipe.api.nodes.taxonomy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * THROWAWAY tool test (not a regression gate): dumps every registered node
 * info (id, name, plain-text description) as JSON for the search-tuning
 * analysis. Run explicitly via -Dtest=NodeRegistryDumpToolTest; ignored by
 * normal suite runs via the dump-path guard.
 */
class NodeRegistryDumpToolTest {

    private static final String DUMP_PATH = System.getProperty("jipipe.dump.path", "");

    @BeforeAll
    static void init() {
        JIPipe.ensureInstance();
    }

    @Test
    void dumpRegistry() throws IOException {
        if (DUMP_PATH.isEmpty()) {
            return; // no-op in normal suite runs
        }
        ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            ObjectNode node = root.putObject(info.getId());
            node.put("name", info.getName());
            HTMLText description = info.getDescription();
            node.put("description", description != null ? description.toPlainText() : "");
            node.put("framework", info.getFramework().getId());
        }
        Path target = Paths.get(DUMP_PATH);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, root.toPrettyString(), StandardCharsets.UTF_8);
        System.out.println("DUMPED " + root.size() + " node infos to " + target);
    }
}
