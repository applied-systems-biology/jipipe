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

package org.hkijena.jipipe.api.service.components.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.InputProfileProvider;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the taxonomy registry wiring of
 * {@link JIPipeNodesServiceComponent}: function/framework/family
 * registries, total input-profile derivation, and
 * {@link JIPipeNodesServiceComponent#validateTaxonomy()}.
 * <p>
 * The component under test is the shared instance obtained via
 * {@link JIPipe#getNodes()} after {@link JIPipe#ensureInstance()} (the
 * established pattern for service-component access; a bare
 * {@code new JIPipeService()} is not usable, because {@code register()}
 * requires the service to be initialized). Synthetic registrations are
 * removed again in {@link #cleanup()}.
 * <p>
 * Phase 1a leaves the curated function tree unregistered (it is populated
 * by the taxonomy plugin, Task 12), so every node that declares functions
 * currently surfaces in {@code validateTaxonomy()} as "points at an
 * unregistered function". The tests therefore check for their specific
 * synthetic error entries rather than for the global emptiness of the
 * result set.
 */
class TaxonomyResolverTest {

    private static final String FUNC_ID = "taxonomy-resolver-test:function";

    private static final String FRAMEWORK_ID = "taxonomy-resolver-test:framework";

    /** Stable id literal for the stub workflow family (see the stable-ID rule). */
    private static final String FAMILY_ID = "jipipe:family:taxonomy-resolver-test";

    private static final String TEST_DEPENDENCY_ID = "taxonomy-resolver-test";

    private static final List<InputProfileProvider> testProviders = new ArrayList<>();

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
        // Synthetic node registrations (registeredNodeInfos)
        nodes.unregister(NodesWithFunction.class.getSimpleName());
        nodes.unregister(NoFunctionNode.class.getSimpleName());
        nodes.unregister(DanglingStepNode.class.getSimpleName());
        // Curated registry entries (functions / frameworks)
        nodes.getFunctions().unregister(FUNC_ID);
        nodes.getFrameworks().unregister(FRAMEWORK_ID);
        // Family step assembled into the shared FamilyAssembler, and the
        // synthetic family itself (first-seen via DanglingStepNode's step
        // annotation). Both must be removed so that later tests observe a
        // clean family registry (no synthetic families leak across tests).
        nodes.getFamilyAssembler().unregisterStep(DanglingStepNode.class.getSimpleName(), FAMILY_ID);
        nodes.unregisterFamily(FAMILY_ID);
        // Input profile providers
        nodes.unregisterInputProfileProviders(testProviders);
        // Re-run the (idempotent) registration pipeline so that enrichment
        // recomputes over the surviving real nodes only.
        nodes.runRegistrationTasks();
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    /**
     * A node whose primary function was never registered in the
     * FunctionRegistry produces a {@code validateTaxonomy()} error that
     * names both the node id and the function id (Review Focus #1).
     */
    @Test
    void unregisteredFunctionFailsValidation() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(NodesWithFunction.class);
        // The function deliberately stays unregistered in the FunctionRegistry.

        Set<String> errors = nodes.validateTaxonomy();

        assertFalse(errors.isEmpty(), "expected at least the unregistered-function error");
        assertTrue(errors.stream().anyMatch(e -> e.contains(NodesWithFunction.class.getSimpleName()) && e.contains(FUNC_ID)),
            "error must name the node id and the function id, got: " + errors);
    }

    /**
     * With no input profile providers registered,
     * {@code inputProfileFor} returns {@code NA} and does not throw
     * (Review Focus #5: derivation is total).
     */
    @Test
    void inputProfileTotal() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();

        JIPipeNodeTaxonomyInputProfile profile = assertDoesNotThrow(
            () -> nodes.inputProfileFor(JIPipeDataTable.class));

        assertEquals(JIPipeNodeTaxonomyInputProfile.NA, profile);
    }

    /**
     * The first provider that returns a non-{@code NA} profile wins;
     * providers that return {@code NA} are skipped.
     */
    @Test
    void inputProfileFirstNonNAWins() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        InputProfileProvider naProvider = dataClass -> JIPipeNodeTaxonomyInputProfile.NA;
        InputProfileProvider volumeProvider = dataClass ->
            dataClass == JIPipeDataTable.class
                ? JIPipeNodeTaxonomyInputProfile.VOLUME_3D
                : JIPipeNodeTaxonomyInputProfile.NA;
        nodes.registerInputProfileProvider(naProvider);
        nodes.registerInputProfileProvider(volumeProvider);
        testProviders.add(naProvider);
        testProviders.add(volumeProvider);

        assertEquals(JIPipeNodeTaxonomyInputProfile.VOLUME_3D,
            nodes.inputProfileFor(JIPipeDataTable.class));
    }

    /**
     * A workflow family step whose node id is not registered surfaces as
     * a {@code validateTaxonomy()} error.
     * <p>
     * The node is registered first (so that {@code register()} picks up its
     * {@code @JIPipeNodeTaxonomyFamilyStepAnnotation} and assembles the
     * step into the family), then unregistered, leaving a dangling step.
     */
    @Test
    void familyDanglingStepSurfaces() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(DanglingStepNode.class);
        nodes.unregister(DanglingStepNode.class.getSimpleName());

        Set<String> errors = nodes.validateTaxonomy();

        assertTrue(errors.stream().anyMatch(e -> e.contains(FAMILY_ID) && e.contains(DanglingStepNode.class.getSimpleName())),
            "error must name the family id and the dangling node id, got: " + errors);
    }

    /**
     * A node with a null primary function (not yet converted to the
     * taxonomy) is NOT an error of {@code validateTaxonomy()}; it is
     * reported separately as unconverted (Task 8), not as a validation
     * error (Review Focus / Phase 1a contract).
     */
    @Test
    void unconvertedNodeIsNotAnError() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(NoFunctionNode.class);

        Set<String> errors = nodes.validateTaxonomy();

        assertFalse(errors.stream().anyMatch(e -> e.contains(NoFunctionNode.class.getSimpleName())),
            "an unconverted node must not be an error, got: " + errors);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void registerNode(Class<? extends JIPipeGraphNode> nodeClass) {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (nodes.hasNodeInfoWithId(nodeClass.getSimpleName())) {
            nodes.unregister(nodeClass.getSimpleName());
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(nodeClass.getSimpleName(), nodeClass);
        nodes.register(info, new JIPipeMutableDependency(TEST_DEPENDENCY_ID, "1.0.0", "taxonomy-resolver-test"));
    }

    // ------------------------------------------------------------------
    // Stub taxonomy members
    // ------------------------------------------------------------------

    /** Minimal taxonomy function with a top-level hue (self-validating). */
    static class StubFunction implements JIPipeNodeTaxonomyFunction {
        public StubFunction() {
        }

        @Override
        public String getId() {
            return FUNC_ID;
        }

        @Override
        public String getName() {
            return "Test Function";
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return 0.42f;
        }

        @Override
        public String getDescription() {
            return "Test stub function";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of("test-tag");
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
    static class StubFramework implements JIPipeNodeTaxonomyFramework {
        public StubFramework() {
        }

        @Override
        public String getId() {
            return FRAMEWORK_ID;
        }

        @Override
        public String getName() {
            return "Test Framework";
        }

        @Override
        public String getAbbreviation() {
            return "TF";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /** Stub workflow family. */
    static class StubFamily implements JIPipeNodeTaxonomyFamily {
        public StubFamily() {
        }

        @Override
        public String getId() {
            return FAMILY_ID;
        }

        @Override
        public String getName() {
            return "Test Family";
        }

        @Override
        public String getDescription() {
            return "Test stub family";
        }
    }

    /**
     * A node converted to the taxonomy: one primary function (instantiated
     * by the {@link ConfigureJIPipeNode} extraction) and a framework.
     * Consumes a data table so that input-profile enrichment has a target.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class)
    @AddJIPipeInputSlot(value = JIPipeDataTable.class, name = "Table", create = true)
    private static class NodesWithFunction extends JIPipeGraphNode {
        public NodesWithFunction(JIPipeNodeInfo info) {
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

    /** A node that is not yet converted: no taxonomy functions. */
    private static class NoFunctionNode extends JIPipeGraphNode {
        public NoFunctionNode(JIPipeNodeInfo info) {
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

    /**
     * A node whose family step annotation makes it a step of
     * {@link StubFamily}; the step is assembled when the node is
     * registered, and dangles once the node is unregistered.
     */
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = StubFamily.class, label = "Dangling step")
    private static class DanglingStepNode extends JIPipeGraphNode {
        public DanglingStepNode(JIPipeNodeInfo info) {
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
