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
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-module conversion gate for the node taxonomy (phase 1b, task 14;
 * Review Focus #6 — "a conversion batch must not leave a node without a
 * function, or it vanishes from the browser").
 * <p>
 * <b>Gated:</b> this test is skipped unless the system property
 * {@code jipipe.taxonomy.report.module} is set to a <b>Java package
 * prefix</b> identifying the module under conversion (one prefix per
 * invocation), e.g. {@code org.hkijena.jipipe.plugins.tables} for the
 * tables plugin (most standalone plugins live under
 * {@code org.hkijena.jipipe.plugins.<name>}) or, for jipipe-core's own
 * nodes, a prefix covering the packages they live in
 * ({@code org.hkijena.jipipe.api} for the looping/grouping/compartments
 * scaffolding nodes, {@code org.hkijena.jipipe.plugins} for the internal
 * plugins bundled into core). The filter is a
 * {@code instanceClass.getName().startsWith(prefix)} match, so any prefix
 * that is a common root of the module's node classes works.
 * When the property is unset the test is <i>skipped</i> (not failed), so the
 * normal {@code mvn test -pl jipipe-core} stays green while the conversion is
 * in progress.
 * <p>
 * <b>Running the gate for a module:</b>
 * <pre>{@code
 * mvn test -pl <module> \
 *   -Dtest=TaxonomyConversionReportTest \
 *   -Djipipe.taxonomy.report.module=<package-prefix>
 * }</pre>
 * The project's Surefire (3.5.3) forwards user {@code -D} properties into
 * the forked test JVM, so a plain {@code -D} is sufficient — no
 * {@code argLine} wiring needed.
 * <p>
 * <b>One prefix per invocation.</b> The gate filters by a single package
 * prefix, so a module whose node classes span several root packages (e.g.
 * jipipe-core's own nodes, which live under both {@code org.hkijena.jipipe.api}
 * and {@code org.hkijena.jipipe.plugins}) is checked with one invocation per
 * prefix.
 * <p>
 * <b>Module boundary — copy this test into plugin modules!</b>
 * {@code JIPipe.ensureInstance()} only initializes plugins on the classpath
 * of the running module. This test lives in jipipe-core, where it gates
 * core's own nodes. Plugin modules depend on core (not vice versa), so
 * {@code mvn test -pl jipipe-core} cannot see their nodes. For each
 * 19.x plugin conversion batch, <b>copy-paste this test file unchanged into
 * the plugin module's test tree</b> (same package, same file name — it is
 * deliberately self-contained: only jipipe-core API + JUnit 5 imports) and
 * run the gate with that module's package prefix. Do not re-derive the test
 * per batch; this class is the single source.
 * <p>
 * <b>Registration caveat:</b> the gate only sees nodes that are actually
 * <i>registered</i> in the running module's classpath. A node class with
 * {@code @ConfigureJIPipeNode} is not automatically registered — it must be
 * referenced by a plugin's {@code registerNodeType(...)} call (or a
 * registration task). E.g. in jipipe-core the comment node
 * ({@code jipipe:comment}) is registered by the <i>utils</i> plugin, and the
 * filesystem data sources register with the full application. When checking
 * a module, make sure its gate run covers the classpath where its nodes
 * register (see {@code utils/taxonomy-conversion/conversion-map.md}).
 * <p>
 * <b>Pass criterion:</b> every registered node whose instance class name
 * starts with the prefix must have a non-{@code null} primary taxonomy
 * function (per {@link TaxonomyValidationReport#unassignedNodeIds()}).
 * The test fails and enumerates the remaining unconverted node ids
 * otherwise. It also fails on real taxonomy errors (dangling function or
 * framework references) so a batch cannot "convert" into a broken registry.
 */
@EnabledIfSystemProperty(named = "jipipe.taxonomy.report.module", matches = ".+",
        disabledReason =
                "Conversion gate: enabled only when -Djipipe.taxonomy.report.module=<package-prefix> is set "
                        + "(see class Javadoc for the per-module invocation).")
class TaxonomyConversionReportTest {

    /**
     * The package prefix of the module under conversion, read from the
     * system property. Guaranteed non-null by the class-level
     * {@link EnabledIfSystemProperty} gate.
     */
    private static String modulePrefix;

    @BeforeAll
    static void init() {
        // Idempotent: reuses the JIPipe singleton if another test class in
        // the same JVM already initialized it.
        JIPipe.ensureInstance();
        modulePrefix = System.getProperty("jipipe.taxonomy.report.module");
    }

    @Test
    void moduleHasNoUnconvertedNodes() {
        TaxonomyValidationReport report = TaxonomyValidationReport.run(
                JIPipe.getNodes().getFunctions(),
                JIPipe.getNodes().getFrameworks(),
                JIPipe.getNodes().getRegisteredNodeInfos());

        // Hard errors (broken function tree, dangling framework/function
        // references) must never be shipped by a conversion batch.
        assertTrue(report.errors().isEmpty(),
                "Taxonomy validation errors while converting '" + modulePrefix + "': " + report.errors());

        // The gate itself: every node of this module must be converted
        // (have a primary function). Filter by instance-class package
        // prefix — registration path (annotation vs. programmatic) is
        // irrelevant here.
        List<String> unassignedInModule = report.unassignedNodeIds().stream()
                .filter(nodeId -> {
                    JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(nodeId);
                    return info != null
                            && info.getInstanceClass().getName().startsWith(modulePrefix);
                })
                .sorted()
                .collect(Collectors.toList());

        assertTrue(unassignedInModule.isEmpty(),
                "Module '" + modulePrefix + "' still has " + unassignedInModule.size()
                        + " node(s) without a taxonomy function:\n  "
                        + String.join("\n  ", unassignedInModule)
                        + "\nRun utils/taxonomy-conversion/convert.py with the module's map.tsv, then re-run this gate.");
    }
}
