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
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.multiparameters.nodes.AnnotationsToParametersAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
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
 * Tests the parameter introspection operations. Two seams are covered: the
 * documentation join of the query_node operation (without the include-docs
 * flag the parameter entries keep the normalized value shape of the previous
 * sub-project (value, type, raw?), with include-docs every entry additionally
 * carries the name, description, and important metadata of its parameter
 * access, and a parameter without a documentation annotation renders the
 * key-derived access name and an empty description), and the changed-vs-default
 * diff of the get_parameter_diff operation, which compares every parameter of
 * a node against a fresh reference instance (the project report's own
 * machinery) and renders both sides through the same normalized seam, and the
 * read-back of the set_parameter operation, which renders the parameter from
 * the same access after the set (the effective value after conversion, in the
 * identical shape query_node reports for that access).
 */
public class ParameterIntrospectionOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String TEXT_BOX_NODE_ID = "graph-annotation-text-box";
    private static final String DYNAMIC_PARAMS_NODE_ID = "annotations-to-parameters";
    private static final String NULL_VALUE_PARAM_NODE_ID = "parameter-introspection-test:null-value-parameter";
    private static final String ENUM_PARAM_NODE_ID = "parameter-introspection-test:enum-parameter";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("parameter-introspection-operations-test", "1.0.0",
                    "parameter-introspection-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;
    private static boolean registeredTextBoxNodeHere;
    private static boolean registeredDynamicParamsNodeHere;
    private static boolean registeredNullValueParamNodeHere;
    private static boolean registeredEnumParamNodeHere;

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
        // The null-value fixture node is a test-only type: its never-set-value
        // parameter is null on a fresh instance, which is the present-but-null
        // side of the defaultValue contract of get_parameter_diff (Review
        // Focus 3 of the plan: such a key renders the string "null", while
        // JSON null is reserved for keys the reference tree does not have).
        // It extends the comment node so the fixture inherits valid node
        // plumbing; the id cannot collide with a real registration.
        if (!JIPipe.getNodes().hasNodeInfoWithId(NULL_VALUE_PARAM_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(NULL_VALUE_PARAM_NODE_ID, NullValueParameterNode.class), TEST_DEPENDENCY);
            registeredNullValueParamNodeHere = true;
        }
        // The enum-parameter fixture node is a test-only type: its
        // render-mode parameter is an enum whose normalized value rendering
        // (the text description) differs from its raw rendering (toString()),
        // which pins the raw rule of the set_parameter read-back. It extends
        // the comment node so the fixture inherits valid node plumbing; the
        // id cannot collide with a real registration.
        if (!JIPipe.getNodes().hasNodeInfoWithId(ENUM_PARAM_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(ENUM_PARAM_NODE_ID, EnumParameterNode.class), TEST_DEPENDENCY);
            registeredEnumParamNodeHere = true;
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
        if (registeredNullValueParamNodeHere) {
            JIPipe.getNodes().unregister(NULL_VALUE_PARAM_NODE_ID);
            registeredNullValueParamNodeHere = false;
        }
        if (registeredEnumParamNodeHere) {
            JIPipe.getNodes().unregister(ENUM_PARAM_NODE_ID);
            registeredEnumParamNodeHere = false;
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

    private static ObjectNode parameterDiffParams(String nodeId, Boolean includeUnchanged) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
        if (includeUnchanged != null) {
            params.put("include-unchanged", includeUnchanged);
        }
        return params;
    }

    private static JsonNode findParameterEntry(JsonNode result, String key) {
        for (JsonNode candidate : result.get("parameters")) {
            if (key.equals(candidate.get("key").asText())) {
                return candidate;
            }
        }
        return null;
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
        assertTrue(hasNodeType(NULL_VALUE_PARAM_NODE_ID),
                NULL_VALUE_PARAM_NODE_ID + " is not registered: the parameter diff tests would silently skip");
        assertTrue(hasNodeType(ENUM_PARAM_NODE_ID),
                ENUM_PARAM_NODE_ID + " is not registered: the set_parameter read-back tests would silently skip");
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

    @Test
    void getParameterDiff_unchangedNodeYieldsEmptyChangedList() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode comment = info.newInstance();
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode changedOnly = new QueryOperations.GetParameterDiff().execute(ctx, parameterDiffParams(alias, null));
        assertEquals(0, changedOnly.get("changedCount").asInt(),
                "a fresh node has no parameter that differs from a fresh instance: " + changedOnly);
        assertEquals(0, changedOnly.get("parameters").size(),
                "the changed-only list of a fresh node is empty: " + changedOnly);
        assertEquals(changedOnly, JIPipeAutomationAPI.getParameterDiff(ctx, alias, false),
                "the operation without include-unchanged must equal the 3-arg getParameterDiff with false");

        JsonNode fullInventory = new QueryOperations.GetParameterDiff().execute(ctx, parameterDiffParams(alias, true));
        assertEquals(fullInventory, JIPipeAutomationAPI.getParameterDiff(ctx, alias, true),
                "the operation with include-unchanged must equal the 3-arg getParameterDiff with true");
        assertEquals(0, fullInventory.get("changedCount").asInt(),
                "changedCount counts changed entries only, also in the full inventory: " + fullInventory);
        Set<String> responseKeys = new LinkedHashSet<>();
        for (JsonNode entry : fullInventory.get("parameters")) {
            responseKeys.add(entry.get("key").asText());
            assertEquals(Set.of("key", "value", "type", "defaultValue", "changed"), fieldNames(entry),
                    "every entry carries exactly the diff keys: " + entry);
            assertFalse(entry.get("changed").asBoolean(),
                    "no parameter of a fresh node is changed: " + entry);
            assertTrue(entry.get("defaultValue").isTextual(),
                    "a fresh node has every key in the reference tree, so defaultValue is a rendered string, never JSON null: " + entry);
        }
        Set<String> expectedKeys = new LinkedHashSet<>(JIPipeParameterTree.getParameters(comment).keySet());
        expectedKeys.remove("jipipe:node:name");
        expectedKeys.remove("jipipe:node:description");
        assertEquals(expectedKeys, responseKeys,
                "include-unchanged lists the full current-tree inventory minus the report exclusions");
    }

    @Test
    void getParameterDiff_modifiedParameterIsChanged() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode textBox = info.newInstance();
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(textBox);

        ObjectNode setParams = mapper.createObjectNode();
        setParams.put("node-id", alias);
        setParams.put("key", "text-title");
        setParams.put("value", "Hello diff");
        JsonNode setResult = new ModificationOperations.SetParameter().execute(ctx, setParams);
        assertTrue(setResult.get("success").asBoolean(), "the parameter must be set before the diff: " + setResult);

        JsonNode result = new QueryOperations.GetParameterDiff().execute(ctx, parameterDiffParams(alias, null));
        assertEquals(1, result.get("changedCount").asInt(),
                "exactly the modified parameter is changed: " + result);
        assertEquals(1, result.get("parameters").size(),
                "the changed-only list carries exactly the modified parameter: " + result);
        JsonNode entry = result.get("parameters").get(0);
        assertEquals("text-title", entry.get("key").asText(), "the modified key is listed: " + result);
        assertEquals("Hello diff", entry.get("value").asText(),
                "value is the new rendering: " + entry);
        assertEquals("Text box", entry.get("defaultValue").asText(),
                "defaultValue is the old rendering from the fresh reference instance: " + entry);
        assertTrue(entry.get("changed").asBoolean(), "the entry is marked changed: " + entry);
        assertEquals("String", entry.get("type").asText(),
                "type describes the live access field class, same as query_node: " + entry);
    }

    @Test
    void getParameterDiff_optionalParameterNeverSetRendersNullString() {
        assumeTrue(hasNodeType(NULL_VALUE_PARAM_NODE_ID),
                NULL_VALUE_PARAM_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(NULL_VALUE_PARAM_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.GetParameterDiff().execute(ctx,
                parameterDiffParams(project.getGraph().getAliasIdOf(node), true));
        JsonNode entry = findParameterEntry(result, "never-set-value");
        assertNotNull(entry, "the never-set-value entry is missing: " + result);
        assertFalse(entry.get("changed").asBoolean(),
                "the parameter is untouched since instantiation, so both sides render the same: " + entry);
        assertTrue(entry.get("defaultValue").isTextual() && "null".equals(entry.get("defaultValue").asText()),
                "a present-but-null parameter renders the string \"null\" as defaultValue: " + entry);
        assertFalse(entry.get("defaultValue").isNull(),
                "defaultValue must be the string \"null\", never JSON null (JSON null is reserved for keys the reference tree does not have): " + entry);
        assertEquals("null", entry.get("value").asText(),
                "the live side renders the same string \"null\": " + entry);
    }

    @Test
    void getParameterDiff_keyMissingFromReferenceGetsJsonNullDefaultValue() {
        assumeTrue(hasNodeType(DYNAMIC_PARAMS_NODE_ID),
                DYNAMIC_PARAMS_NODE_ID + " node type not registered - skipping");
        // A runtime-added dynamic parameter exists in the current tree only:
        // the fresh reference instance has an empty dynamic collection
        var info = JIPipe.getNodes().getInfoById(DYNAMIC_PARAMS_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        AnnotationsToParametersAlgorithm node = (AnnotationsToParametersAlgorithm) info.newInstance();
        node.getExtractedParameters().addParameter("my-runtime-parameter", String.class);
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.GetParameterDiff().execute(ctx,
                parameterDiffParams(project.getGraph().getAliasIdOf(node), null));
        // The diff iterates the parameter tree, so the entry key is the tree
        // path of the dynamic parameter (collection prefix + access key)
        JsonNode entry = findParameterEntry(result, "extracted-parameters/my-runtime-parameter");
        assertNotNull(entry, "the current-only key must be listed as changed: " + result);
        assertTrue(entry.get("changed").asBoolean(),
                "a key the reference tree does not have is changed: " + entry);
        assertTrue(entry.get("defaultValue").isNull(),
                "defaultValue is JSON null when the reference tree has no such key: " + entry);
        assertEquals("", entry.get("value").asText(),
                "value is the rendering of the live default instance: " + entry);
        assertEquals(1, result.get("changedCount").asInt(),
                "exactly the current-only key counts as changed: " + result);
    }

    @Test
    void getParameterDiff_excludesNodeNameAndDescriptionKeys() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode textBox = info.newInstance();
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(textBox);

        // Make both report-excluded keys differ from the fresh reference, plus
        // one non-excluded parameter so the changed list is not empty
        ObjectNode setNameParams = mapper.createObjectNode();
        setNameParams.put("node-id", alias);
        setNameParams.put("key", "jipipe:node:name");
        setNameParams.put("value", "Renamed text box");
        JsonNode setNameResult = new ModificationOperations.SetParameter().execute(ctx, setNameParams);
        assertTrue(setNameResult.get("success").asBoolean(), "the node name must be set before the diff: " + setNameResult);
        ObjectNode setTitleParams = mapper.createObjectNode();
        setTitleParams.put("node-id", alias);
        setTitleParams.put("key", "text-title");
        setTitleParams.put("value", "Hello diff");
        JsonNode setTitleResult = new ModificationOperations.SetParameter().execute(ctx, setTitleParams);
        assertTrue(setTitleResult.get("success").asBoolean(), "the text title must be set before the diff: " + setTitleResult);
        textBox.setCustomDescription(new HTMLText("Custom note"));

        JsonNode result = new QueryOperations.GetParameterDiff().execute(ctx, parameterDiffParams(alias, null));
        for (JsonNode entry : result.get("parameters")) {
            String key = entry.get("key").asText();
            assertFalse(key.equals("jipipe:node:name") || key.equals("jipipe:node:description"),
                    "the report-excluded keys must never appear in the response: " + entry);
            assertFalse(key.startsWith("jipipe:node:"),
                    "no jipipe:node key is part of the diff: " + entry);
        }
        JsonNode textTitle = findParameterEntry(result, "text-title");
        assertNotNull(textTitle,
                "the changed non-excluded parameter must still be listed (the response must not be vacuously empty): " + result);
        assertTrue(textTitle.get("changed").asBoolean(), "text-title is the changed parameter: " + textTitle);
        assertEquals(1, result.get("changedCount").asInt(),
                "only text-title counts: name and description are excluded before the comparison: " + result);
    }

    @Test
    void getParameterDiff_unknownNodeErrors() {
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);
        JsonNode result = new QueryOperations.GetParameterDiff().execute(ctx, parameterDiffParams("nope", null));
        assertEquals("Node not found: nope", result.get("error").asText(),
                "an unknown node yields the house error envelope: " + result);
    }

    @Test
    void setParameter_responseCarriesEffectiveValue() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode textBox = info.newInstance();
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(textBox);

        ObjectNode setParams = mapper.createObjectNode();
        setParams.put("node-id", alias);
        setParams.put("key", "text-title");
        setParams.put("value", "Hello readback");
        JsonNode result = new ModificationOperations.SetParameter().execute(ctx, setParams);

        assertTrue(result.get("success").asBoolean(), "the set must succeed: " + result);
        JsonNode parameter = result.get("parameter");
        assertNotNull(parameter, "the response must carry the parameter read-back: " + result);
        assertEquals("Hello readback", parameter.get("value").asText(),
                "the response carries the effective value after conversion: " + result);
        assertEquals("String", parameter.get("type").asText(),
                "type is the field class simple name, same as query_node: " + result);
        assertEquals(Set.of("value", "type"), fieldNames(parameter),
                "a string set renders value and type without the raw key (the renderings agree): " + result);
    }

    @Test
    void setParameter_responseNormalizedLikeQueryNode() {
        assumeTrue(hasNodeType(ENUM_PARAM_NODE_ID), ENUM_PARAM_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(ENUM_PARAM_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        EnumParameterNode node = (EnumParameterNode) info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(node);

        ObjectNode setParams = mapper.createObjectNode();
        setParams.put("node-id", alias);
        setParams.put("key", "render-mode");
        setParams.put("value", "fancy");
        JsonNode result = new ModificationOperations.SetParameter().execute(ctx, setParams);

        assertTrue(result.get("success").asBoolean(), "the set must succeed: " + result);
        JsonNode parameter = result.get("parameter");
        assertNotNull(parameter, "the response must carry the parameter read-back: " + result);
        assertEquals("fancy", parameter.get("value").asText(),
                "the enum parameter set by name renders its normalized value: " + result);
        assertEquals("FANCY", parameter.get("raw").asText(),
                "raw appears exactly when the renderings differ and carries the plain toString() form: " + result);
        assertEquals(Set.of("value", "type", "raw"), fieldNames(parameter),
                "the entry carries exactly the normalized value keys: " + result);

        JsonNode viaQueryNode = new QueryOperations.QueryNode().execute(ctx, queryNodeParams(alias, null));
        assertEquals(viaQueryNode.get("parameters").get("render-mode"), parameter,
                "the read-back is identical to what query_node returns for the same access: " + result);
    }

    /**
     * Test-only node type whose render-mode parameter is an enum that
     * renders its normalized value differently from its raw form: setting by
     * name stores the FANCY constant, whose normalized value rendering is
     * "fancy" (the text description) while the raw rendering is "FANCY" (the
     * plain toString()), which pins the raw rule of the read-back (raw
     * appears exactly when the renderings differ, the same rule query_node
     * applies). It extends the comment node so the fixture inherits valid
     * node plumbing and needs no additional setup.
     */
    @ConfigureJIPipeNode(function = GraphDebugFunction.class)
    public static class EnumParameterNode extends JIPipeCommentNode {

        private RenderMode renderMode = RenderMode.PLAIN;

        public EnumParameterNode(JIPipeNodeInfo info) {
            super(info);
        }

        public EnumParameterNode(EnumParameterNode other) {
            super(other);
            this.renderMode = other.renderMode;
        }

        @JIPipeParameter("render-mode")
        public RenderMode getRenderMode() {
            return renderMode;
        }

        @JIPipeParameter("render-mode")
        public void setRenderMode(RenderMode renderMode) {
            this.renderMode = renderMode;
        }

        /**
         * Test-only enum whose normalized value rendering (the text
         * description, "fancy") differs from the raw rendering (the plain
         * toString(), "FANCY"), which lets the read-back test pin the raw
         * rule.
         */
        private enum RenderMode implements JIPipeCustomTextDescriptionParameter {
            PLAIN, FANCY;

            @Override
            public String getTextDescription() {
                return name().toLowerCase();
            }
        }
    }

    /**
     * Test-only node type whose never-set-value parameter is null on a fresh
     * instance: renderParameterValue renders the string "null" for it, which
     * pins the present-but-null side of the defaultValue contract (Review
     * Focus 3 of the plan). It extends the comment node so the fixture
     * inherits valid node plumbing and needs no additional setup.
     */
    @ConfigureJIPipeNode(function = GraphDebugFunction.class)
    public static class NullValueParameterNode extends JIPipeCommentNode {

        private String neverSetValue;

        public NullValueParameterNode(JIPipeNodeInfo info) {
            super(info);
        }

        public NullValueParameterNode(NullValueParameterNode other) {
            super(other);
            this.neverSetValue = other.neverSetValue;
        }

        @JIPipeParameter("never-set-value")
        public String getNeverSetValue() {
            return neverSetValue;
        }

        @JIPipeParameter("never-set-value")
        public void setNeverSetValue(String neverSetValue) {
            this.neverSetValue = neverSetValue;
        }
    }
}
