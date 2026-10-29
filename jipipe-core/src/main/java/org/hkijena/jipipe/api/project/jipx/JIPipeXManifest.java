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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The manifest of a JIPX project container (the {@code jipx.json} entry).
 * <p>
 * A manifest is a stable, versioned description of the container's
 * structure: which parts it contains (zip entry path → part key, see
 * {@link JIPipeProjectParts#PART_KEYS}), which graph is the main graph,
 * and — via the {@code unknown} map — which other entries the container
 * holds so that they are preserved verbatim on round-trips. The reserved
 * slots {@code data/}, {@code graphs/}, and {@code history/} appear in
 * {@code unknown} when non-empty.
 * <p>
 * The parts table of a JIPipe-authored container always uses the default
 * part file layout: {@code project/<part key>.json} with {@code ':'}
 * replaced by {@code '-'} in the file name only (e.g. the part
 * {@code jipipe:doc:parameters} lives at {@code
 * project/jipipe-doc-parameters.json}, while the manifest value keeps
 * the real part key).
 */
public class JIPipeXManifest {

    /**
     * The current version of the JIPX container manifest format.
     */
    public static final int FORMAT_VERSION = 1;

    /**
     * The zip entry path of the main project graph part.
     */
    public static final String MAIN_GRAPH = "project/graph.json";

    /**
     * The file name of the manifest within a JIPX container.
     */
    public static final String MANIFEST_FILE_NAME = "jipx.json";

    /**
     * The version of the manifest format.
     */
    @JsonProperty("format-version")
    private int formatVersion = FORMAT_VERSION;

    /**
     * The JIPipe version that authored the container.
     */
    @JsonProperty("jipipe-version")
    private String jipipeVersion;

    /**
     * The zip entry path of the main graph part.
     */
    @JsonProperty("main-graph")
    private String mainGraph = MAIN_GRAPH;

    /**
     * Container entry path → part key, in write order.
     */
    @JsonProperty("parts")
    private Map<String, String> parts = new LinkedHashMap<>();

    /**
     * Other container entry paths (not parts), preserved verbatim on
     * round-trip — including the reserved slots {@code data/},
     * {@code graphs/}, and {@code history/} when non-empty.
     */
    @JsonProperty("unknown")
    private Map<String, String> unknown = new LinkedHashMap<>();

    /**
     * Creates an empty manifest with the current format version.
     */
    public JIPipeXManifest() {
    }

    /**
     * Deserializes a manifest from a JSON node (the tree that
     * {@code jipx.json} holds).
     *
     * @param node the manifest JSON tree
     * @return the manifest
     * @throws IOException triggered by {@link ObjectMapper}
     */
    public static JIPipeXManifest fromJsonNode(JsonNode node) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(JIPipeXManifest.class).readValue(node);
    }

    /**
     * Creates the default manifest for a container authored by JIPipe:
     * all 9 default parts at {@code project/<part key>.json} (file name
     * = part key with {@code ':'} replaced by {@code '-'}; the value
     * keeps the real part key), no unknown parts.
     *
     * @param jipipeVersion the JIPipe version to record
     * @return the default manifest
     */
    public static JIPipeXManifest defaultManifest(String jipipeVersion) {
        JIPipeXManifest manifest = new JIPipeXManifest();
        manifest.setFormatVersion(FORMAT_VERSION);
        manifest.setJipipeVersion(jipipeVersion);
        manifest.setMainGraph(MAIN_GRAPH);
        for (String partKey : JIPipeProjectParts.PART_KEYS) {
            manifest.getParts().put("project/" + partKey.replace(':', '-') + ".json", partKey);
        }
        return manifest;
    }

    /**
     * Serializes this manifest to a JSON tree.
     *
     * @return the manifest JSON tree
     */
    public JsonNode toJsonNode() {
        return JsonUtils.getObjectMapper().valueToTree(this);
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getJipipeVersion() {
        return jipipeVersion;
    }

    public void setJipipeVersion(String jipipeVersion) {
        this.jipipeVersion = jipipeVersion;
    }

    public String getMainGraph() {
        return mainGraph;
    }

    public void setMainGraph(String mainGraph) {
        this.mainGraph = mainGraph;
    }

    public Map<String, String> getParts() {
        return parts;
    }

    public void setParts(Map<String, String> parts) {
        this.parts = parts;
    }

    public Map<String, String> getUnknown() {
        return unknown;
    }

    public void setUnknown(Map<String, String> unknown) {
        this.unknown = unknown;
    }
}
