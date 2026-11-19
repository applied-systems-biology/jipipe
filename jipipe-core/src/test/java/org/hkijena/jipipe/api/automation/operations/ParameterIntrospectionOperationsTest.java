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

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.multiparameters.nodes.AnnotationsToParametersAlgorithm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the documentation join of the query_node operation: without the
 * include-docs flag the parameter entries keep the normalized value shape of
 * the previous sub-project (value, type, raw?), with include-docs every entry
 * additionally carries the name, description, and important metadata of its
 * parameter access, and a parameter without a documentation annotation
 * renders the key-derived access name and an empty description (the docs
 * keys are never omitted).
 */
public class ParameterIntrospectionOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String TEXT_BOX_NODE_ID = "graph-annotation-text-box";
    private static final String DYNAMIC_PARAMS_NODE_ID = "annotations-to-parameters";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("parameter-introspection-operations-test", "1.0.0",
                    "parameter-introspection-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;
    private static boolean registeredTextBoxNodeHere;
    private static boolean registeredDynamicParamsNodeHere;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
        // The jipipe-core test JVM loads only the core plugins: the id
        // jipipe:comment is registered by the UtilitiesPlugin of the
        // jipipe-plugin-utils module, which is not on this module's test
        // classpath. Register the node (the class itself lives in jipipe-core)
        // when absent so the behavioral tests below run instead of skipping,
        // following the same approach as the UI controller tests. Where the
        // UtilitiesPlugin is loaded, its registration is kept.
        if (!JIPipe.getNodes().hasNodeInfoWithId(COMMENT_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(COMMENT_NODE_ID, JIPipeCommentNode.class), TEST_DEPENDENCY);
            registeredCommentNodeHere = true;
        }
        // Same wall as jipipe:comment: the id graph-annotation-text-box is
        // registered by the CanvasNotesPlugin, whose registration is not
        // active in this module's test JVM (the class itself lives in
        // jipipe-core). Where the plugin is loaded, its registration is kept.
        if (!JIPipe.getNodes().hasNodeInfoWithId(TEXT_BOX_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(TEXT_BOX_NODE_ID, TextBoxGraphCanvasNote.class), TEST_DEPENDENCY);
            registeredTextBoxNodeHere = true;
        }
        // The annotations-to-parameters node provides a dynamic parameter
        // collection, whose runtime-added parameters carry no documentation
        // annotation (its access name derives from the key). Register the id
        // when absent, same as above.
        if (!JIPipe.getNodes().hasNodeInfoWithId(DYNAMIC_PARAMS_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(DYNAMIC_PARAMS_NODE_ID, AnnotationsToParametersAlgorithm.class), TEST_DEPENDENCY);
            registeredDynamicParamsNodeHere = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (registeredCommentNodeHere) {
            JIPipe.getNodes().unregister(COMMENT_NODE_ID);
            registeredCommentNodeHere = false;
        }
        if (registeredTextBoxNodeHere) {
            JIPipe.getNodes().unregister(TEXT_BOX_NODE_ID);
            registeredTextBoxNodeHere = false;
        }
        if (registeredDynamicParamsNodeHere) {
            JIPipe.getNodes().unregister(DYNAMIC_PARAMS_NODE_ID);
            registeredDynamicParamsNodeHere = false;
        }
    }

    private static boolean hasNodeType(String id) {
        try {
            JIPipe.getNodes().getInfoById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static ObjectNode queryNodeParams(String nodeId, Boolean includeDocs) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
        if (includeDocs != null) {
            params.put("include-docs", includeDocs);
        }
        return params;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void smoke_requiredNodeTypesAreRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                COMMENT_NODE_ID + " is not registered: the parameter docs tests would silently skip");
        assertTrue(hasNodeType(TEXT_BOX_NODE_ID),
                TEXT_BOX_NODE_ID + " is not registered: the parameter docs tests would silently skip");
        assertTrue(hasNodeType(DYNAMIC_PARAMS_NODE_ID),
                DYNAMIC_PARAMS_NODE_ID + " is not registered: the parameter docs tests would silently skip");
    }

    @Test
    void queryNode_defaultShapeUnchanged() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode comment = info.newInstance();
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode viaOperation = new QueryOperations.QueryNode().execute(ctx, queryNodeParams(alias, null));
        JsonNode viaApi = JIPipeAutomationAPI.queryNode(ctx, alias);

        JsonNode parameters = viaOperation.get("parameters");
        assertTrue(parameters.size() > 0, "the comment node must report parameters");
        for (JsonNode entry : parameters) {
            Set<String> fields = fieldNames(entry);
            assertTrue(fields.containsAll(Set.of("value", "type")),
                    "every entry carries the normalized value and type keys: " + entry);
            assertTrue(Set.of("value", "type", "raw").containsAll(fields),
                    "entries carry exactly the normalized value keys, raw optional: " + entry);
            assertFalse(entry.has("name") || entry.has("description") || entry.has("important"),
                    "docs fields must be absent when include-docs is not requested: " + entry);
        }
        assertEquals(viaApi, viaOperation,
                "the operation without include-docs must equal the plain 2-arg queryNode result");
    }

    @Test
    void queryNode_includeDocsAddsMetadata() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        AutomationContext ctx = createContext(project);

        // The background-color parameter of the comment node carries a
        // documentation annotation with name and description
        var commentInfo = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeGraphNode comment = commentInfo.newInstance();
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());

        JsonNode commentResult = new QueryOperations.QueryNode().execute(ctx,
                queryNodeParams(project.getGraph().getAliasIdOf(comment), true));
        JsonNode commentParameters = commentResult.get("parameters");
        JsonNode backgroundColor = commentParameters.get("background-color");
        assertNotNull(backgroundColor, "the background-color entry is missing: " + commentResult);
        assertEquals("Background color", backgroundColor.get("name").asText(),
                "the name comes from the documentation annotation");
        assertFalse(backgroundColor.get("description").asText().isBlank(),
                "the description comes from the documentation annotation: " + backgroundColor);
        assertTrue(backgroundColor.get("important").isBoolean(), "important is present as a boolean");
        assertTrue(backgroundColor.has("value") && backgroundColor.has("type"),
                "the normalized value keys remain: " + backgroundColor);
        for (JsonNode entry : commentParameters) {
            assertTrue(entry.has("name") && entry.has("description") && entry.has("important"),
                    "every entry carries the docs fields when they are requested: " + entry);
            assertTrue(entry.get("important").isBoolean(), "important must be a boolean: " + entry);
        }

        // The text-title parameter of the text box is annotated as important
        var textBoxInfo = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeGraphNode textBox = textBoxInfo.newInstance();
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());

        JsonNode textBoxResult = new QueryOperations.QueryNode().execute(ctx,
                queryNodeParams(project.getGraph().getAliasIdOf(textBox), true));
        JsonNode textTitle = textBoxResult.get("parameters").get("text-title");
        assertNotNull(textTitle, "the text-title entry is missing: " + textBoxResult);
        assertEquals("Title", textTitle.get("name").asText(),
                "the name comes from the documentation annotation");
        assertTrue(textTitle.get("important").isBoolean(), "important is present as a boolean");
        assertTrue(textTitle.get("important").asBoolean(),
                "text-title is annotated as an important parameter: " + textTitle);
    }

    @Test
    void queryNode_includeDocsEmptyStrings() {
        assumeTrue(hasNodeType(DYNAMIC_PARAMS_NODE_ID),
                DYNAMIC_PARAMS_NODE_ID + " node type not registered - skipping");
        // A dynamically added parameter of the annotations-to-parameters node
        // has no documentation annotation: its access name derives from the
        // key and its description is null
        var info = JIPipe.getNodes().getInfoById(DYNAMIC_PARAMS_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        AnnotationsToParametersAlgorithm node = (AnnotationsToParametersAlgorithm) info.newInstance();
        node.getExtractedParameters().addParameter("my-undocumented-parameter", String.class);
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.QueryNode().execute(ctx,
                queryNodeParams(project.getGraph().getAliasIdOf(node), true));

        JsonNode entry = result.get("parameters").get("my-undocumented-parameter");
        assertNotNull(entry, "the dynamic parameter entry is missing: " + result);
        assertEquals("my-undocumented-parameter", entry.get("name").asText(),
                "without a documentation annotation, the name is the key-derived access name");
        assertTrue(entry.has("description"), "description must stay present, never be omitted: " + entry);
        assertEquals("", entry.get("description").asText(),
                "a parameter without a documentation annotation renders an empty description");
        assertTrue(entry.get("important").isBoolean(), "important is present as a boolean");
    }
}
