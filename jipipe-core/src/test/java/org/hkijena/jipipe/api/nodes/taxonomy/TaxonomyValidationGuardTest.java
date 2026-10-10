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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the startup validation guard of the node taxonomy:
 * {@link TaxonomyValidationReport} and its wiring into
 * {@link JIPipeNodesServiceComponent#postprocess(JIPipeProgressInfo)}.
 * <p>
 * {@link TaxonomyValidationReport#run(FunctionRegistry, FrameworkRegistry, Map)}
 * is static and pure (no JIPipe service access), so the three core scenarios
 * are unit tests over synthetic registries:
 * <ul>
 * <li>a valid taxonomy with one unconverted node is {@code ok()}: the
 * unconverted node only appears in {@code unassignedNodeIds()} (the
 * expected phase 1a state; it must NOT fail initialization),</li>
 * <li>a broken function tree (a parent-chain cycle) produces
 * {@code errors()} and is not {@code ok():} such a taxonomy must never
 * ship a wrong browser,</li>
 * <li>a fully converted taxonomy has no errors and no unassigned nodes.</li>
 * </ul>
 * <p>
 * The fourth and fifth tests prove the component wiring: a real registry
 * error, and a dangling family step, are both appended to the service's
 * initialization report errors during {@code postprocess()} (the existing
 * init-failure mechanism), while unconverted nodes are not.
 */
class TaxonomyValidationGuardTest {

    private static final String DEPENDENCY_ID = "taxonomy-validation-guard-test";

    /** Stable id literal for the stub family used by the dangle test. */
    private static final String FAMILY_ID = "jipipe:family:guard-test";

    private final List<String> registeredNodeIds = new ArrayList<>();

    private final List<JIPipeNodeTaxonomyFunction> registeredFunctions = new ArrayList<>();

    private final List<JIPipeNodeTaxonomyFramework> registeredFrameworks = new ArrayList<>();

    /** (nodeId, familyId) pairs added to the family assembler, for teardown. */
    private final List<String[]> registeredFamilySteps = new ArrayList<>();

    // ------------------------------------------------------------------
    // Setup / teardown
    // ------------------------------------------------------------------

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @AfterEach
    void cleanup() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        // Family steps assembled into the shared FamilyAssembler must be
        // removed before the owning node is unregistered, so that no
        // dangling step is left behind by this test.
        for (String[] step : registeredFamilySteps) {
            nodes.getFamilyAssembler().unregisterStep(step[0], step[1]);
        }
        registeredFamilySteps.clear();
        for (String nodeId : registeredNodeIds) {
            if (nodes.hasNodeInfoWithId(nodeId)) {
                nodes.unregister(nodeId);
            }
        }
        registeredNodeIds.clear();
        for (JIPipeNodeTaxonomyFunction function : registeredFunctions) {
            nodes.getFunctions().unregister(function.getId());
        }
        registeredFunctions.clear();
        for (JIPipeNodeTaxonomyFramework framework : registeredFrameworks) {
            if (!(framework instanceof NoFramework)) {
                nodes.getFrameworks().unregister(framework.getId());
            }
        }
        registeredFrameworks.clear();
    }

    // ------------------------------------------------------------------
    // Scenario (a): all registries valid + one unconverted node
    // ------------------------------------------------------------------

    /**
     * A valid function/framework registry plus one node whose primary
     * function is {@code null} (not yet converted) is the expected
     * phase 1a state: {@code ok()} must be TRUE and the node must be
     * listed in {@code unassignedNodeIds()}, with no errors. The guard
     * only fails initialization on real errors, never on unconverted
     * nodes.
     */
    @Test
    void validTaxonomyWithUnconvertedNodeIsOk() {
        FunctionRegistry functions = new FunctionRegistry();
        JIPipeNodeTaxonomyFunction function = new StubFunction("guard-test:function");
        functions.register(function);

        FrameworkRegistry frameworks = new FrameworkRegistry();
        JIPipeNodeTaxonomyFramework framework = new StubFramework("guard-test:framework");
        frameworks.register(framework);

        Map<String, JIPipeNodeInfo> nodeInfos = new LinkedHashMap<>();
        nodeInfos.put("guard-test:assigned",
            new StubNodeInfo("guard-test:assigned", function, framework));
        nodeInfos.put("guard-test:unconverted",
            new StubNodeInfo("guard-test:unconverted", null, null));

        TaxonomyValidationReport report = TaxonomyValidationReport.run(
            functions, frameworks, nodeInfos);

        assertTrue(report.ok(), "unconverted nodes must not make the taxonomy invalid: " + report.errors());
        assertTrue(report.errors().isEmpty(), "no errors expected, got: " + report.errors());
        assertEquals(List.of("guard-test:unconverted"), report.unassignedNodeIds());
    }

    // ------------------------------------------------------------------
    // Scenario (b): cyclic functions ⇒ errors ⇒ not ok
    // ------------------------------------------------------------------

    /**
     * A parent-chain cycle in the function registry is a hard validation
     * error: {@code errors()} is non-empty and {@code ok()} is false.
     * A broken taxonomy must fail the startup guard even if every node
     * is converted.
     */
    @Test
    void cyclicFunctionRegistryFails() {
        FunctionRegistry functions = new FunctionRegistry();
        JIPipeNodeTaxonomyFunction f1 = new StubFunction("guard-test:cycle-a", new StubFunction("guard-test:cycle-b"));
        JIPipeNodeTaxonomyFunction f2 = new StubFunction("guard-test:cycle-b", f1);
        functions.register(f1);
        functions.register(f2);

        Map<String, JIPipeNodeInfo> nodeInfos = new LinkedHashMap<>();
        nodeInfos.put("guard-test:assigned",
            new StubNodeInfo("guard-test:assigned", f1, new NoFramework()));

        TaxonomyValidationReport report = TaxonomyValidationReport.run(
            functions, new FrameworkRegistry(), nodeInfos);

        assertFalse(report.ok(), "a cyclic function registry must be invalid");
        assertFalse(report.errors().isEmpty(), "the cycle must be reported");
        assertTrue(report.errors().stream().anyMatch(e ->
                e.contains("guard-test:cycle-a") && e.contains("guard-test:cycle-b")),
            "the cycle must name both function ids, got: " + report.errors());
        assertTrue(report.unassignedNodeIds().isEmpty());
    }

    // ------------------------------------------------------------------
    // Scenario (c): fully converted synthetic set
    // ------------------------------------------------------------------

    /**
     * When every node is converted (registered primary function and
     * framework), the report is clean: no errors, no unassigned nodes,
     * {@code ok()} true.
     */
    @Test
    void fullyConvertedSetIsClean() {
        FunctionRegistry functions = new FunctionRegistry();
        JIPipeNodeTaxonomyFunction function = new StubFunction("guard-test:function");
        functions.register(function);

        FrameworkRegistry frameworks = new FrameworkRegistry();
        JIPipeNodeTaxonomyFramework framework = new StubFramework("guard-test:framework");
        frameworks.register(framework);

        Map<String, JIPipeNodeInfo> nodeInfos = new LinkedHashMap<>();
        nodeInfos.put("guard-test:node-a", new StubNodeInfo("guard-test:node-a", function, framework));
        nodeInfos.put("guard-test:node-b", new StubNodeInfo("guard-test:node-b", function, framework));

        TaxonomyValidationReport report = TaxonomyValidationReport.run(
            functions, frameworks, nodeInfos);

        assertTrue(report.ok());
        assertTrue(report.errors().isEmpty(), "got: " + report.errors());
        assertTrue(report.unassignedNodeIds().isEmpty(),
            "got: " + report.unassignedNodeIds());
    }

    // ------------------------------------------------------------------
    // Component wiring: postprocess() appends real errors to the
    // initialization report
    // ------------------------------------------------------------------

    /**
     * A real taxonomy error (a cyclic function registered in the shared
     * registry) is appended to the service's initialization report
     * errors during {@code postprocess()} — the existing init-failure
     * mechanism — while unconverted nodes (the expected 1a state with
     * the curated tree unregistered) are not.
     */
    @Test
    void postprocessAppendsRealErrorsToInitializationReport() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();

        // Cycle in the shared function registry
        JIPipeNodeTaxonomyFunction f1 = new StubFunction("guard-test:cycle-a", new StubFunction("guard-test:cycle-b"));
        JIPipeNodeTaxonomyFunction f2 = new StubFunction("guard-test:cycle-b", f1);
        nodes.getFunctions().register(f1);
        nodes.getFunctions().register(f2);
        registeredFunctions.add(f1);
        registeredFunctions.add(f2);

        JIPipeService service = JIPipe.getInstance();
        int reportErrorsBefore = service.getInitializationReport().getErrors().size();
        Throwable appended;
        try {
            nodes.postprocess(service.getProgressInfo().resolve("taxonomy-validation-guard-test"));

            int reportErrorsAfter = service.getInitializationReport().getErrors().size();
            assertTrue(reportErrorsAfter > reportErrorsBefore,
                "postprocess() must append the taxonomy error to the initialization report");
            appended = service.getInitializationReport().getErrors().get(reportErrorsBefore);
        } finally {
            // Restore the init report: a leaked init error would fail every
            // later init-related test in the suite.
            while (service.getInitializationReport().getErrors().size() > reportErrorsBefore) {
                service.getInitializationReport().getErrors().remove(
                    service.getInitializationReport().getErrors().size() - 1);
            }
        }

        assertNotNull(appended.getMessage());
        assertTrue(appended.getMessage().contains("guard-test:cycle-a"),
            "the appended error must carry the cycle message, got: " + appended.getMessage());
        // Nothing must remain after teardown
        assertEquals(reportErrorsBefore, service.getInitializationReport().getErrors().size());
    }

    /**
     * A family step whose node is not (any longer) registered is a hard
     * validation error: it must be appended to the service's
     * initialization report errors during {@code postprocess()}. This is
     * the family-workflow half of the invariant that "a broken-but-
     * partly-converted taxonomy must never ship a wrong browser."
     */
    @Test
    void postprocessAppendsDanglingFamilyStepToInitializationReport() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        FamilyAssembler assembler = nodes.getFamilyAssembler();

        // A step in the stub family, referencing a node id that is not
        // registered (dangling).
        JIPipeNodeTaxonomyFamilyStepAnnotation annotation = stepAnnotation(StubFamily.class, "Dangling step");
        assembler.addStep("guard-test:phantom-node", FAMILY_ID, annotation);
        registeredFamilySteps.add(new String[]{"guard-test:phantom-node", FAMILY_ID});

        JIPipeService service = JIPipe.getInstance();
        int reportErrorsBefore = service.getInitializationReport().getErrors().size();
        Throwable appended;
        try {
            nodes.postprocess(service.getProgressInfo().resolve("taxonomy-validation-guard-test-dangle"));

            int reportErrorsAfter = service.getInitializationReport().getErrors().size();
            assertTrue(reportErrorsAfter > reportErrorsBefore,
                "postprocess() must append the dangling-family-step error to the initialization report");
            appended = service.getInitializationReport().getErrors().get(reportErrorsBefore);
        } finally {
            // Restore the init report: a leaked init error would fail every
            // later init-related test in the suite.
            while (service.getInitializationReport().getErrors().size() > reportErrorsBefore) {
                service.getInitializationReport().getErrors().remove(
                    service.getInitializationReport().getErrors().size() - 1);
            }
        }

        assertNotNull(appended.getMessage());
        String message = appended.getMessage();
        assertTrue(message.contains("guard-test:phantom-node"),
            "the appended error must name the dangling node id, got: " + message);
        assertTrue(message.contains(FAMILY_ID),
            "the appended error must name the family id, got: " + message);
        // Nothing must remain after teardown
        assertEquals(reportErrorsBefore, service.getInitializationReport().getErrors().size());
    }

    // ------------------------------------------------------------------
    // Stub taxonomy members
    // ------------------------------------------------------------------

    /** Builds a runtime annotation instance with the given family and label. */
    private static JIPipeNodeTaxonomyFamilyStepAnnotation stepAnnotation(
        Class<? extends JIPipeNodeTaxonomyFamily> family, String label) {
        return new JIPipeNodeTaxonomyFamilyStepAnnotation() {
            @Override
            public Class<? extends JIPipeNodeTaxonomyFamily> family() {
                return family;
            }

            @Override
            public String label() {
                return label;
            }

            @Override
            public Class<?>[] after() {
                return new Class<?>[0];
            }

            @Override
            public String purpose() {
                return "";
            }

            @Override
            public Class<? extends JIPipeNodeTaxonomyFamilyStepAnnotation> annotationType() {
                return JIPipeNodeTaxonomyFamilyStepAnnotation.class;
            }
        };
    }

    /** Stub workflow family for the dangle test. */
    private static class StubFamily implements JIPipeNodeTaxonomyFamily {
        @Override
        public String getId() {
            return FAMILY_ID;
        }

        @Override
        public String getName() {
            return "Guard Test Family";
        }

        @Override
        public String getDescription() {
            return "Test stub family";
        }
    }

    /** Minimal taxonomy function; optionally parented for cycle construction. */
    private static class StubFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;
        private final JIPipeNodeTaxonomyFunction parent;

        StubFunction(String id) {
            this(id, null);
        }

        StubFunction(String id, JIPipeNodeTaxonomyFunction parent) {
            this.id = id;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        @Nullable
        public JIPipeNodeTaxonomyFunction getParent() {
            return parent;
        }

        @Override
        @Nullable
        public Float getColorHue() {
            // A top-level function (no parent) must carry a hue; sub-branches
            // inherit from their ancestor, so null is valid there.
            return parent == null ? 0.71f : null;
        }

        @Override
        public String getDescription() {
            return "Test stub function " + id;
        }

        @Override
        @Nullable
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    /** Minimal taxonomy framework. */
    private static class StubFramework implements JIPipeNodeTaxonomyFramework {
        private final String id;

        StubFramework(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return "Test Framework " + id;
        }

        @Override
        public String getAbbreviation() {
            return "GTV";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /**
     * A self-contained, mutable node info stub for the pure
     * {@link TaxonomyValidationReport#run} tests: only the taxonomy-relevant
     * getters are meaningful, everything else returns inert defaults.
     */
    private static class StubNodeInfo implements JIPipeNodeInfo {
        private final String id;
        private final JIPipeNodeTaxonomyFunction primaryFunction;
        private final JIPipeNodeTaxonomyFramework framework;

        StubNodeInfo(String id, JIPipeNodeTaxonomyFunction primaryFunction, JIPipeNodeTaxonomyFramework framework) {
            this.id = id;
            this.primaryFunction = primaryFunction;
            this.framework = framework == null ? new NoFramework() : framework;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Class<? extends JIPipeGraphNode> getInstanceClass() {
            return StubNode.class;
        }

        @Override
        public JIPipeGraphNode newInstance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        public HTMLText getDescription() {
            return new HTMLText();
        }

        @Override
        public List<AddJIPipeInputSlot> getInputSlots() {
            return Collections.emptyList();
        }

        @Override
        public List<AddJIPipeOutputSlot> getOutputSlots() {
            return Collections.emptyList();
        }

        @Override
        public Set<Class<? extends JIPipeEnvironment>> getEnvironments() {
            return Collections.emptySet();
        }

        @Override
        public Set<JIPipeDependency> getDependencies() {
            return Collections.emptySet();
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public JIPipeNodeTaxonomyFunction getPrimaryFunction() {
            return primaryFunction;
        }

        @Override
        public Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
            return Collections.emptySet();
        }

        @Override
        public Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions() {
            return Collections.emptySet();
        }

        @Override
        public JIPipeNodeTaxonomyFramework getFramework() {
            return framework;
        }
    }

    /** A minimal node class backing {@link StubNodeInfo}. */
    @AddJIPipeInputSlot(value = JIPipeDataTable.class, name = "Table", create = true)
    private static class StubNode extends JIPipeGraphNode {
        StubNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }
}
