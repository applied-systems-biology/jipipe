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

package org.hkijena.jipipe.plugins.ijfilaments;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.ij3d.IJ3DPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.FilamentsToRoi3dDataTypeConverter;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.FilamentsToRoiDataTypeConverter;
import org.hkijena.jipipe.plugins.ijfilaments.display.FilamentsManagerPlugin2D;
import org.hkijena.jipipe.plugins.ijfilaments.environments.OptionalTSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.convert.*;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.filter.FilterFilamentEdgesByProperties;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.filter.FilterFilamentVerticesByProperties;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.filter.FilterFilamentsByProperties;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.filter.RemoveBorderFilaments;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.generate.*;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.measure.MeasureEdgesAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.measure.MeasureFilamentsAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.measure.MeasureVerticesAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.merge.MergeFilamentsAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.modify.*;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.process.FixOverlapsNonBranchingAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.process.RemoveDuplicateVerticesAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.process.SimplifyFilamentsAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.process.SmoothFilamentsAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.split.SplitFilamentsIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.split.SplitFilamentsIntoCyclesAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.CycleFinderAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.settings.FilamentsPluginProjectSettings;
import org.hkijena.jipipe.plugins.ijfilaments.settings.ImageViewerUIFilamentDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijfilaments.settings.TSOAXApplicationSettings;
import org.hkijena.jipipe.plugins.ijfilaments.viewers.Filaments3DGraphDataViewer;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.scene3d.Scene3DPlugin;
import org.hkijena.jipipe.plugins.strings.StringsPlugin;
import org.hkijena.jipipe.plugins.tables.TablesPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Extension that adds filaments supports
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class FilamentsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-filaments",
            JIPipe.getJIPipeVersion(),
            "Filaments");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(FilamentsPlugin.class, "org/hkijena/jipipe/plugins/ijfilaments");

    public FilamentsPlugin() {
    }

    public static TSOAXEnvironment getTSOAXEnvironment(JIPipeProject project, OptionalTSOAXEnvironment nodeEnvironment) {
        if (nodeEnvironment != null && nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(FilamentsPluginProjectSettings.class).getProjectDefaultTSOAXEnvironment().isEnabled()) {
            return project.getSettingsSheet(FilamentsPluginProjectSettings.class).getProjectDefaultTSOAXEnvironment().getContent();
        }
        return TSOAXApplicationSettings.getInstance().getReadOnlyDefaultEnvironment();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY,
                TablesPlugin.AS_DEPENDENCY,
                StringsPlugin.AS_DEPENDENCY,
                ImageJDataTypesPlugin.AS_DEPENDENCY,
                ImageJAlgorithmsPlugin.AS_DEPENDENCY,
                IJ3DPlugin.AS_DEPENDENCY,
                Scene3DPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-filaments";
    }

    @Override
    public String getName() {
        return "Filaments";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Introduces support for the processing of filaments.");
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        TSOAXApplicationSettings tsoaxApplicationSettings = new TSOAXApplicationSettings();
        registerEnvironment(TSOAXEnvironment.class,
                TSOAXEnvironment.List.class,
                tsoaxApplicationSettings,
                "tsoax-environment",
                "TSOAX Environment",
                "Installation of TSOAX",
                RESOURCES.getIconFromResources("tsoax.png"));
        registerParameterType("optional-tsoax-environment",
                OptionalTSOAXEnvironment.class,
                "Optional TSOAX Environment",
                "Installation of TSOAX");

        registerNodeTypeCategory(new FilamentsNodeTypeCategory());
        registerApplicationSettingsSheet(new ImageViewerUIFilamentDisplayApplicationSettings());
        registerApplicationSettingsSheet(tsoaxApplicationSettings);
        registerProjectSettingsSheet(FilamentsPluginProjectSettings.class);

        registerDatatype("filaments", Filaments3DGraphData.class, RESOURCES.getIcon16URLFromResources("data-type-filaments.png"));
        registerDatatypeConversion(new FilamentsToRoiDataTypeConverter());
        registerDatatypeConversion(new FilamentsToRoi3dDataTypeConverter());

        JIPipeDesktopLegacyImageViewer.registerDefaultPlugin(FilamentsManagerPlugin2D.class);
        registerDefaultDataTypeViewer(Filaments3DGraphData.class, Filaments3DGraphDataViewer.class);

        registerEnumParameterType("filaments-cycle-finder-algorithm", CycleFinderAlgorithm.class, "Cycle finder algorithm", "An algorithm for finding cycles");

        registerNodeType("filaments-from-json", ImportFilamentsFromJsonAlgorithm.class);
        registerNodeType("filaments-skeleton-to-filaments-2d3d-simplified", SkeletonToSimplifiedFilamentsFijiAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
        registerNodeType("filaments-skeleton-to-filaments-2d", SkeletonToFilaments2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
        registerNodeType("filaments-skeleton-to-filaments-3d", SkeletonToFilaments3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));

        registerNodeType("filaments-set-vertex-properties", ChangeFilamentVertexPropertiesExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("filaments-set-edge-properties", ChangeFilamentEdgePropertiesExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("filaments-set-vertex-properties-2", ChangeFilamentVertexPropertiesManuallyAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("filaments-set-edge-properties-2", ChangeFilamentEdgePropertiesManuallyAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("filaments-flatten", FlattenFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-flatten-z.png"));

        registerNodeType("filaments-filter-vertices-by-properties", FilterFilamentVerticesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-edges-by-properties", FilterFilamentEdgesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-components-by-properties", FilterFilamentsByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-filaments-at-border", RemoveBorderFilaments.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("filaments-split-into-connected-components", SplitFilamentsIntoConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("filaments-split-into-cycles", SplitFilamentsIntoCyclesAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

        registerNodeType("filaments-merge", MergeFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));

        registerNodeType("filaments-convert-cycles-to-roi", ConvertFilamentCyclesToROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("filaments-convert-to-roi", ConvertFilamentsToRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("filaments-convert-to-roi3d", ConvertFilamentsToRoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("filaments-convert-to-mask", ConvertFilamentsToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("filaments-convert-to-labels", ConvertFilamentsToLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("filaments-convert-to-labels-v2", ConvertFilamentsToLabels2Algorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("filaments-convert-to-rgb", ConvertFilamentsToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("filaments-convert-to-scene-3d", ConvertFilamentsTo3DMeshAlgorithm.class, UIUtils.getIconURLFromResources("actions/shape-cuboid.png"));
        registerNodeType("filaments-convert-roi2d-to-filaments", ConvertROIToFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/format-node-curve.png"));

        registerNodeType("filaments-set-vertex-thickness-from-image", SetVertexRadiusFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-circle-from-radius.png"));
        registerNodeType("filaments-set-vertex-intensity-from-image", SetVertexValueFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-set-vertex-metadata-from-image", SetVertexMetadataFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-set-vertex-calibration-from-image", SetVertexPhysicalSizeFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("filaments-remove-duplicate-vertices", RemoveDuplicateVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("filaments-smooth-downscale", SmoothFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-simplify", SimplifyFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-graph-directed.png"));
        registerNodeType("filaments-subdivide-edges", SubdivideFilamentEdges.class, UIUtils.getIconURLFromResources("actions/connector-ignore.png"));
        registerNodeType("filaments-fix-overlaps-non-branching", FixOverlapsNonBranchingAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-line-perpendicular.png"));
        registerNodeType("filaments-cycles-to-components", FilamentCyclesToComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/circle-nodes.png"));
        registerNodeType("filaments-set-metadata-to-component-index", SetFilamentMetadataToComponentIndexAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-find-linear-vertex-order", FindLinearFilamentVertexOrderAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-arrow.png"));

        registerNodeType("filaments-measure-vertices", MeasureVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("filaments-measure-edges", MeasureEdgesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("filaments-measure-filaments", MeasureFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerNodeType("filaments-erode-end-vertices", ErodeEndVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("filaments-grow-end-vertices", GrowEndVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-arrow.png"));
        registerNodeType("filaments-connect-vertices", ConnectVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/lines-connector.png"));
        registerNodeType("filaments-connect-vertices-fast", ConnectVerticesFastAlgorithm.class, UIUtils.getIconURLFromResources("actions/lines-connector.png"));

        registerNodeType("filaments-tsoax", TSOAX3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/labplot-xy-fit-curve.png"));
        registerNodeType("filaments-tsoax-2d", TSOAX2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/labplot-xy-fit-curve.png"));
        registerNodeType("tsoax-analyze-snakes", TSOAXResultAnalysisAlgorithm.class, UIUtils.getIconURLFromResources("actions/labplot-xy-fit-curve.png"));

        registerNodeType("filaments-remove-vertex-value-backups", RemoveVertexValueBackupsAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-restore-vertex-value-backups", RestoreVertexValueBackupAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }


}
