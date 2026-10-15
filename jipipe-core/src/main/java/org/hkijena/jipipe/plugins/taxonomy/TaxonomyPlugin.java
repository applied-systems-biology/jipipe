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

package org.hkijena.jipipe.plugins.taxonomy;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.AnnotationsConvertFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.AnnotationsEditFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.GraphAnnotationsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.FilamentDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.FileSystemFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.FormsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.ImageDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.MiscDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.OmeroFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.RoiDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.Scene3DDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.TableDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningInferenceFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningModelsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningTrainingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportImagesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportRoiFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportScenesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportTablesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentQuantificationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentSmoothingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphCompartmentOutputsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageArithmeticFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageComparisonFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageLabelingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageMomentsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageNeighborMeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImagePixelManipulationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageProjectionsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImagePropertiesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageStatisticsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageTransformationsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageTypeConversionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.PreprocessingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationCellBasedFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ThresholdingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotBarFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotHeatmapFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotLineFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotMiscFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotScatterFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiFiltersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiIntersectionsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMorphologyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiPropertiesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsDescriptiveFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsEstimationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsTestingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableAggregateFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableExportFileFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableFilterModifyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableJoinSplitFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableTransformFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingAnalysisFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingDetectionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingLinkingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.AnnotationsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataSourcesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataUtilitiesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ScriptingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TextMarkupFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingExpressionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingImageJMacroFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingPythonFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingRFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.MachineLearningFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ExportFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.FilamentsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.GraphFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ImagesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.MiscellaneousFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.PlottingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.RoisFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.StatisticsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TablesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TemplatesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TrackingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose2Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose3Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose4Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ClijFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CometFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CoreFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.PythonFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.RFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ2Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JFreeChartFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JIPipeDLFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OmniposeFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IlastikFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OpenCVFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.TrackMateFramework;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.library.references.IconRef;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers the curated node taxonomy into the live registries: the 84
 * functions (13 top-level branches + 71 seed leaves) and the 16 frameworks.
 * <p>
 * This is the single place that populates the {@code FunctionRegistry} and
 * {@code FrameworkRegistry}; node plugins only record their membership in
 * those registries and never add functions or frameworks themselves.
 * <p>
 * Workflow families are intentionally NOT registered here: a family only
 * appears in {@code getFamilies()} once a node carries a
 * {@code @JIPipeNodeTaxonomyFamilyStepAnnotation} (Task 13), so no
 * {@code JIPipeNodeTaxonomyFamily} is registered by this plugin.
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class TaxonomyPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY =
        new JIPipeMutableDependency("org.hkijena.jipipe:taxonomy",
            JIPipe.getJIPipeVersion(), "Node taxonomy");

    @Override
    public String getName() {
        return "Node taxonomy";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Registers the curated node taxonomy: functions and frameworks");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:taxonomy";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Set.of(CorePlugin.AS_DEPENDENCY);
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        var functions = JIPipe.getNodes().getFunctions();
        var frameworks = JIPipe.getNodes().getFrameworks();

        // 13 top-level branches first, so leaves' parents exist when the tree
        // is validated later (registration is idempotent and order-independent,
        // but this is cleaner).
        JIPipeNodeTaxonomyFunction[] topLevels = {
            new DataSourcesFunction(),
            new ImagesFunction(),
            new RoisFunction(),
            new FilamentsFunction(),
            new TablesFunction(),
            new StatisticsFunction(),
            new TrackingFunction(),
            new AnnotationsFunction(),
            new MachineLearningFunction(),
            new PlottingFunction(),
            new ExportFunction(),
            new GraphFunction(),
            new MiscellaneousFunction()
        };
        for (JIPipeNodeTaxonomyFunction function : topLevels) {
            functions.register(function);
        }

        // 48 seed leaves, grouped by sub-branch (order is irrelevant).
        JIPipeNodeTaxonomyFunction[] leaves = {
            // data
            new ImageDataFunction(),
            new TableDataFunction(),
            new RoiDataFunction(),
            new FilamentDataFunction(),
            new MiscDataFunction(),
            // data — full-registry audit leaves (curated nodes previously in
            // the "Misc. data" catch-all)
            new FileSystemFunction(),
            new OmeroFunction(),
            new FormsFunction(),
            new Scene3DDataFunction(),
            // images
            new PreprocessingFunction(),
            new FilteringFunction(),
            new ThresholdingFunction(),
            new SegmentationFunction(),
            new SegmentationCellBasedFunction(),
            new MeasurementsFunction(),
            new ResamplingFunction(),
            new MergingSplittingFunction(),
            new ColorStackFunction(),
            // images — full-registry audit leaves (auto-discovered IJ2/CLIJ2
            // nodes + curated strays that previously fell to Miscellaneous)
            new ImageArithmeticFunction(),
            new ImageComparisonFunction(),
            new ImageProjectionsFunction(),
            new ImageTypeConversionFunction(),
            new ImageLabelingFunction(),
            new ImageStatisticsFunction(),
            new ImageMomentsFunction(),
            new ImageTransformationsFunction(),
            new ImagePixelManipulationFunction(),
            new ImageNeighborMeasurementsFunction(),
            new ImagePropertiesFunction(),
            // rois
            new RoiFiltersFunction(),
            new RoiPropertiesFunction(),
            new RoiMeasurementsFunction(),
            new RoiMorphologyFunction(),
            new RoiIntersectionsFunction(),
            // filaments
            new FilamentSmoothingFunction(),
            new FilamentFilteringFunction(),
            new FilamentQuantificationFunction(),
            // tables
            new TableFilterModifyFunction(),
            new TableJoinSplitFunction(),
            new TableAggregateFunction(),
            new TableTransformFunction(),
            new TableExportFileFunction(),
            // statistics
            new StatisticsDescriptiveFunction(),
            new StatisticsTestingFunction(),
            new StatisticsEstimationFunction(),
            // tracking
            new TrackingDetectionFunction(),
            new TrackingLinkingFunction(),
            new TrackingFilteringFunction(),
            new TrackingAnalysisFunction(),
            // annotations
            new AnnotationsEditFunction(),
            new AnnotationsConvertFunction(),
            new GraphAnnotationsFunction(),
            // deeplearning
            new MachineLearningInferenceFunction(),
            new MachineLearningTrainingFunction(),
            new MachineLearningModelsFunction(),
            // plotting
            new PlotScatterFunction(),
            new PlotLineFunction(),
            new PlotBarFunction(),
            new PlotHeatmapFunction(),
            new PlotMiscFunction(),
            // export — organized by WHAT is exported
            new ExportImagesFunction(),
            new ExportTablesFunction(),
            new ExportRoiFunction(),
            new ExportScenesFunction(),
            new ExportDataFunction(),
            // graph
            new GraphCompartmentOutputsFunction(),
            new GraphWrappersFunction(),
            new GraphDebugFunction(),
            // misc — full-registry audit leaves (curated nodes previously in
            // the "Miscellaneous" catch-all)
            new DataUtilitiesFunction(),
            new TextMarkupFunction(),
            new ScriptingFunction(),
            // misc — templates catch-all (template-taxonomy integration)
            new TemplatesFunction(),
            // scripting — language sub-leaves (re-clustering audit: users
            // interested in one language get a dedicated branch; the
            // matching framework facet is the orthogonal filter)
            new ScriptingPythonFunction(),
            new ScriptingRFunction(),
            new ScriptingImageJMacroFunction(),
            new ScriptingExpressionFunction()
        };
        for (JIPipeNodeTaxonomyFunction function : leaves) {
            functions.register(function);
        }

        // 16 frameworks (flat set; order is irrelevant).
        frameworks.register(new CoreFramework());
        frameworks.register(new PythonFramework());
        frameworks.register(new RFramework());
        frameworks.register(new ImageJ1Framework());
        frameworks.register(new ImageJ2Framework());
        frameworks.register(new Cellpose2Framework());
        frameworks.register(new Cellpose3Framework());
        frameworks.register(new Cellpose4Framework());
        frameworks.register(new ClijFramework());
        frameworks.register(new CometFramework());
        frameworks.register(new JFreeChartFramework());
        frameworks.register(new JIPipeDLFramework());
        frameworks.register(new OmniposeFramework());
        frameworks.register(new IlastikFramework());
        frameworks.register(new OpenCVFramework());
        frameworks.register(new TrackMateFramework());

        // Ship the corpus-mined workflow patterns as System (plugin) node
        // templates. Runs at the END of registration: the function tree above
        // is fully populated so the icon/hue derivation below can resolve every
        // top-level branch, and the registered templates feed the
        // TemplateRelationSource which mines their graphs into the
        // Similar/Previous/Next relations index at startup.
        registerSystemTemplates();
    }

    /**
     * Registers the corpus-mined workflow patterns as bundled System node
     * templates (task 8, spec §11).
     * <p>
     * The 145 deduplicated patterns (185 research artifacts, 40 collapsed
     * shape-label duplicates + 1 kept-both fan-in/fan-out pair with distinct
     * names — see {@code utils/generate-system-template-metadata.py}) ship as
     * bundled resources under
     * {@code org/hkijena/jipipe/plugins/nodetemplate/patterns/}:
     * <ul>
     *   <li>one raw {@code JIPipeGraph}-shaped JSON per pattern
     *       ({@code {"nodes": …, "edges": …}}) — the exact shape
     *       {@link JIPipeNodeTemplate#getGraph()} deserializes;</li>
     *   <li>{@code system-templates.json} — a committed metadata sidecar
     *       mapping pattern id → {@code {name, description, functionId,
     *       file}} (no runtime markdown parsing).</li>
     * </ul>
     * For each pattern the template's {@code data} is the raw graph JSON, its
     * {@code name} and {@code description} come from the sidecar, its
     * {@code taxonomyFunction} is the sidecar's {@code functionId} (a top-level
     * branch, computed by the generator), and its icon/hue are derived from the
     * LIVE function registry at registration time (the top-level branch's
     * {@code getColorHue()} / {@code getIcon()} — never stored in the sidecar).
     * The {@code source} is left at its default: {@code addFromPlugin}
     * force-sets it to {@link JIPipeNodeTemplate#SOURCE_EXTENSION}
     * ("Extension"), which is what makes these System-scoped
     * ({@code isPluginTemplate} → {@code true}).
     * <p>
     * Registration is deferred: {@link #registerNodeTemplate(JIPipeNodeTemplate)}
     * schedules the templates (executed after all plugin registration), so a
     * failed graph would not abort the rest.
     */
    private void registerSystemTemplates() {
        JsonNode sidecar = readPatternsSidecar();
        if (sidecar == null) {
            return;
        }
        FunctionRegistry functions = JIPipe.getNodes().getFunctions();
        Map<String, IconRef> iconCache = new HashMap<>();
        int registered = 0;
        for (String id : sortedKeys(sidecar)) {
            JsonNode entry = sidecar.get(id);
            String file = entry.path("file").asText(id + ".json");
            String data = readPatternGraphResource(file);
            if (data == null) {
                JIPipe.getInstance().getProgressInfo().log(
                    "ERROR: missing bundled system template graph resource " + file + " (pattern " + id + ")");
                continue;
            }
            String functionId = entry.path("functionId").asText(null);
            if (StringUtils.isNullOrEmpty(functionId)) {
                JIPipe.getInstance().getProgressInfo().log(
                    "ERROR: bundled system template " + id + " has no functionId in its sidecar entry");
                continue;
            }
            JIPipeNodeTemplate template = new JIPipeNodeTemplate();
            template.setName(entry.path("name").asText(id));
            template.setDescription(new HTMLText(entry.path("description").asText("")));
            template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(functionId));
            template.setData(data);
            // Icon + hue from the LIVE registry (the top-level branch carries
            // both; the assigned functionId is itself top-level). Derived, not
            // stored in the sidecar.
            JIPipeNodeTaxonomyFunction function = functions.byId(functionId);
            if (function != null) {
                Float hue = function.getColorHue();
                if (hue != null) {
                    template.setColorHue(hue);
                }
                IconRef icon = iconCache.get(functionId);
                if (icon == null && function.getIcon() != null) {
                    icon = reverseMapIcon(function.getIcon());
                    if (icon != null) {
                        iconCache.put(functionId, icon);
                    }
                }
                if (icon != null) {
                    template.setIcon(icon);
                }
            }
            registerNodeTemplate(template);
            registered++;
        }
        JIPipe.getInstance().getProgressInfo().log(
            "Registered " + registered + " corpus-mined System node templates (bundled patterns)");
    }

    /**
     * Reads the committed {@code system-templates.json} metadata sidecar from
     * the bundled classpath resources.
     *
     * @return the sidecar object, or {@code null} if the resource is absent
     *         (the caller then registers nothing)
     */
    private static JsonNode readPatternsSidecar() {
        try (InputStream in = TaxonomyPlugin.class.getResourceAsStream(SYSTEM_TEMPLATE_SIDECAR)) {
            if (in == null) {
                return null;
            }
            return JsonUtils.getObjectMapper().readTree(in);
        } catch (Exception e) {
            JIPipe.getInstance().getProgressInfo().log(
                "ERROR: could not read the bundled system-template sidecar: " + e);
            return null;
        }
    }

    /** Reads a bundled raw graph JSON resource (absolute classpath path). */
    private static String readPatternGraphResource(String file) {
        String path = SYSTEM_TEMPLATE_RESOURCE_DIR + file;
        try (InputStream in = TaxonomyPlugin.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            JIPipe.getInstance().getProgressInfo().log("ERROR reading pattern graph " + path + ": " + e);
            return null;
        }
    }

    /**
     * Classpath base directory of the bundled system-template graph resources
     * (absolute, as required by {@link Class#getResourceAsStream}).
     */
    static final String SYSTEM_TEMPLATE_RESOURCE_DIR =
        "/org/hkijena/jipipe/plugins/nodetemplate/patterns/";
    /** The committed metadata sidecar (pattern id → name/description/functionId/file). */
    static final String SYSTEM_TEMPLATE_SIDECAR = SYSTEM_TEMPLATE_RESOURCE_DIR + "system-templates.json";

    /**
     * Maps a function's live {@link javax.swing.Icon} back to its
     * {@code IconRef} name. Every top-level branch builds its icon via
     * {@code JIPipe.RESOURCES.getIcon16("<name>")}, which is cached by name and
     * therefore returns the SAME instance on every call for a given name. So
     * we reverse-map by reference equality: probe each known 16px icon name and
     * return the one whose cached instance is the very icon the function
     * reported. Falls back to {@code null} (the template keeps its default
     * icon) if no match is found.
     */
    private static IconRef reverseMapIcon(javax.swing.Icon icon) {
        if (icon == null) {
            return null;
        }
        // walkInternalResourceFolder returns FULL resource paths; the IconRef
        // name is the relative path (e.g. "actions/document-import.png"), so
        // strip the light 16px base path first (mirrors getAvailableIcons()).
        String basePath = ResourceUtils.getResourcePath("icons/light/icons-16/");
        for (String fullPath : ResourceUtils.walkInternalResourceFolder("icons/light/icons-16/")) {
            if (!fullPath.startsWith(basePath) || !fullPath.endsWith(".png")) {
                continue;
            }
            String name = fullPath.substring(basePath.length());
            try {
                if (icon == JIPipe.RESOURCES.getIcon16(name)) {
                    return new IconRef(name);
                }
            } catch (Throwable ignored) {
                // a name that does not resolve to an icon — skip
            }
        }
        return null;
    }

    /** Sorted property-name list of a JSON object (deterministic iteration). */
    private static List<String> sortedKeys(JsonNode object) {
        List<String> keys = new ArrayList<>();
        object.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        return keys;
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
