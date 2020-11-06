/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.grouping;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.registries.JIPipeDefaultNodeRegistrationTask;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Registers a {@link JsonNodeInfo}
 */
public class JsonNodeRegistrationTask extends JIPipeDefaultNodeRegistrationTask {

    private final JsonNode jsonNode;
    private final JIPipeJsonExtension source;
    private boolean alreadyRegistered = false;

    /**
     * @param jsonNode The JSON serialized graph wrapper algorithm
     * @param source   dependency that registers the algorithm
     */
    public JsonNodeRegistrationTask(JsonNode jsonNode, JIPipeJsonExtension source) {
        this.jsonNode = jsonNode;
        this.source = source;
        findDependencyAlgorithms();
    }

    private void findDependencyAlgorithms() {
        JsonNode graphNodesNode = jsonNode.get("graph").get("nodes");
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(graphNodesNode.fields())) {
            JsonNode infoIdNode = entry.getValue().get("jipipe:node-info-id");
            getDependencyAlgorithmIds().add(infoIdNode.asText());
        }
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        try {
            JsonNodeInfo info = JsonUtils.getObjectMapper().readerFor(JsonNodeInfo.class).readValue(jsonNode);
            if (info == null)
                throw new NullPointerException("Algorithm is null!");
            JIPipe.getNodes().register(info, source);
            if (info.getIcon().getIconName() != null) {
                JIPipe.getNodes().registerIcon(info,
                        ResourceUtils.getPluginResource("icons/" + info.getIcon().getIconName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

