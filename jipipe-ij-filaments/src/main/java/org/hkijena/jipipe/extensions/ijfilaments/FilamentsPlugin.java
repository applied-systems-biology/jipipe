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

package org.hkijena.jipipe.extensions.ijfilaments;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.core.CorePlugin;
import org.hkijena.jipipe.extensions.ij3d.IJ3DPlugin;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsToRoi3dDataTypeConverter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsToRoiDataTypeConverter;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.convert.*;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.FilterFilamentEdgesByProperties;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.FilterFilamentVerticesByProperties;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.FilterFilamentsByProperties;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.RemoveBorderFilaments;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.ImportFilamentsFromJsonAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.SkeletonToFilaments2DAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.SkeletonToFilaments3DAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.SkeletonToSimplifiedFilamentsFijiAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.measure.MeasureEdgesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.measure.MeasureFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.measure.MeasureVerticesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.merge.MergeFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.modify.*;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.FixOverlapsNonBranchingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.RemoveDuplicateVerticesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.SimplifyFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.SmoothFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.split.SplitFilamentsIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.settings.ImageViewerUIFilamentDisplaySettings;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.scene3d.Scene3DPlugin;
import org.hkijena.jipipe.extensions.strings.StringsPlugin;
import org.hkijena.jipipe.extensions.tables.TablesPlugin;
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

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(FilamentsPlugin.class, "org/hkijena/jipipe/extensions/ijfilaments");

    public FilamentsPlugin() {
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
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(RESOURCES.getResourceURL("thumbnail.png"));
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
        registerNodeTypeCategory(new FilamentsNodeTypeCategory());
        registerSettingsSheet(ImageViewerUIFilamentDisplaySettings.ID,
                "Filaments display",
                "Settings for the filaments manager component of the JIPipe image viewer",
                RESOURCES.getIconFromResources("data-type-filaments.png"),
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                new ImageViewerUIFilamentDisplaySettings());

        registerDatatype("filaments", Filaments3DData.class, RESOURCES.getIcon16URLFromResources("data-type-filaments.png"));
        registerDatatypeConversion(new FilamentsToRoiDataTypeConverter());
        registerDatatypeConversion(new FilamentsToRoi3dDataTypeConverter());

        registerNodeType("filaments-from-json", ImportFilamentsFromJsonAlgorithm.class);
        registerNodeType("filaments-skeleton-to-filaments-2d3d-simplified", SkeletonToSimplifiedFilamentsFijiAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
        registerNodeType("filaments-skeleton-to-filaments-2d", SkeletonToFilaments2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
        registerNodeType("filaments-skeleton-to-filaments-3d", SkeletonToFilaments3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));

        registerNodeType("filaments-set-vertex-properties", ChangeFilamentVertexPropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("filaments-set-edge-properties", ChangeFilamentEdgePropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));

        registerNodeType("filaments-filter-vertices-by-properties", FilterFilamentVerticesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-edges-by-properties", FilterFilamentEdgesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-components-by-properties", FilterFilamentsByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-filaments-at-border", RemoveBorderFilaments.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("filaments-split-into-connected-components", SplitFilamentsIntoConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

        registerNodeType("filaments-merge", MergeFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));

        registerNodeType("filaments-convert-to-roi", ConvertFilamentsToRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("filaments-convert-to-roi3d", ConvertFilamentsToRoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("filaments-convert-to-mask", ConvertFilamentsToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("filaments-convert-to-labels", ConvertFilamentsToLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("filaments-convert-to-rgb", ConvertFilamentsToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("filaments-convert-to-scene-3d", ConvertFilamentsTo3DMeshAlgorithm.class, UIUtils.getIconURLFromResources("actions/shape-cuboid.png"));

        registerNodeType("filaments-set-vertex-thickness-from-image", SetVertexRadiusFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-circle-from-radius.png"));
        registerNodeType("filaments-set-vertex-intensity-from-image", SetVertexIntensityFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-circle-from-radius.png"));
        registerNodeType("filaments-set-vertex-metadata-from-image", SetVertexMetadataFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-circle-from-radius.png"));
        registerNodeType("filaments-set-vertex-calibration-from-image", SetVertexPhysicalSizeFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("filaments-remove-duplicate-vertices", RemoveDuplicateVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("filaments-smooth-downscale", SmoothFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-simplify", SimplifyFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-graph-directed.png"));
        registerNodeType("filaments-fix-overlaps-non-branching", FixOverlapsNonBranchingAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-line-perpendicular.png"));

        registerNodeType("filaments-measure-vertices", MeasureVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("filaments-measure-edges", MeasureEdgesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("filaments-measure-filaments", MeasureFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerNodeType("filaments-erode-end-vertices", ErodeEndVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("filaments-grow-end-vertices", GrowEndVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-arrow.png"));
        registerNodeType("filaments-connect-vertices", ConnectVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/lines-connector.png"));
    }


}
