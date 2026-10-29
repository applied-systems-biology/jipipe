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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Slices a project's monolithic JSON representation into per-part documents
 * and reassembles them for {@link JIPipeProject#fromJson}.
 * <p>
 * <b>Fidelity contract:</b> a part is <i>byte-for-byte</i> the monolith's
 * corresponding top-level field - an identity projection, not a
 * re-serialization. {@link #sliceParts(JIPipeProject)} serializes the whole
 * project through the global Jackson mapper
 * ({@link JsonUtils#getObjectMapper()}) with the pretty-printing writer the
 * legacy writer uses, and each returned part node <i>is</i> the top-level
 * field of that monolith tree. Parts are therefore guaranteed to be exactly
 * the serialization JIPipe has always written, and the JIPX container simply
 * separates those fields into individual documents. (Serializing the parts
 * individually would violate this contract.)
 * <p>
 * <b>Dropped fields (spec §4):</b> {@code external-environments} is never
 * sliced - it is dropped in JIPX (environments are re-derived at load time).
 * The container-level fields {@code jipipe:project-type},
 * {@code jipipe:project-format-version}, and {@code jipipe:project-jipipe-version}
 * are also never sliced - they are derived values of the JIPX container
 * format, not project parts.
 */
public final class JIPipeProjectParts {

    /**
     * The 9 part keys in write order.
     */
    public static final List<String> PART_KEYS = List.of(
            "metadata",
            "dependencies",
            "runtime-partitions",
            "run-sets",
            "settings",
            "additional-metadata",
            "graph",
            "compartments",
            "jipipe:doc:parameters"
    );

    private JIPipeProjectParts() {
    }

    /**
     * Serializes the whole project to its monolith tree (via the global
     * mapper, exactly as the legacy writer does) and returns each top-level
     * field of that tree as a part.
     * <p>
     * A part key that is absent from the monolith (bare projects may omit
     * optional fields such as {@code additional-metadata}) is absent from
     * the returned map. The keys {@code external-environments},
     * {@code jipipe:project-type}, {@code jipipe:project-format-version},
     * and {@code jipipe:project-jipipe-version} are never present, by design
     * (dropped/derived, see class Javadoc).
     *
     * @param project the project to slice
     * @return part key → the exact corresponding monolith top-level node
     * @throws IOException triggered by {@link ObjectMapper}
     */
    public static Map<String, JsonNode> sliceParts(JIPipeProject project) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        JsonNode monolith = mapper.readValue(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(project), JsonNode.class);
        Map<String, JsonNode> parts = new LinkedHashMap<>();
        for (String key : PART_KEYS) {
            if (monolith.has(key)) {
                parts.put(key, monolith.get(key));
            }
        }
        return parts;
    }

    /**
     * Builds the exact tree {@link JIPipeProject#fromJson} consumes:
     * {@code jipipe:project-type} = {@code "project"},
     * {@code jipipe:project-format-version} = {@code 1},
     * {@code jipipe:project-jipipe-version} = the passed version, plus every
     * part key mapped to its {@code parts} value. Missing part keys are
     * omitted, as monoliths legitimately omit them. No other fields.
     *
     * @param parts         part key → part node (as from {@link #sliceParts})
     * @param jipipeVersion the {@code jipipe:project-jipipe-version} value
     * @return the assembled monolith tree
     */
    public static JsonNode assembleParts(Map<String, JsonNode> parts, String jipipeVersion) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode tree = mapper.createObjectNode();
        tree.put("jipipe:project-type", "project");
        tree.put("jipipe:project-format-version", 1);
        tree.put("jipipe:project-jipipe-version", jipipeVersion);
        for (String key : PART_KEYS) {
            JsonNode part = parts.get(key);
            if (part != null) {
                tree.set(key, part);
            }
        }
        return tree;
    }
}
