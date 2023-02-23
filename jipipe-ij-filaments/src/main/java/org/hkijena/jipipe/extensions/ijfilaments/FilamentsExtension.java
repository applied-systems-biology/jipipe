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

package org.hkijena.jipipe.extensions.ijfilaments;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsToRoiDataTypeConverter;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.convert.ConvertFilamentsToRoiAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.FilterFilamentEdgesByProperties;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.FilterFilamentVerticesByProperties;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.filter.RemoveDuplicateVerticesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.ImportFilamentsFromJsonAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.SkeletonToFilaments2DAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.generate.SkeletonToSimplifiedFilamentsFijiAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.measure.MeasureEdgesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.measure.MeasureVerticesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.merge.MergeFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.modify.ChangeFilamentVertexPropertiesAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.modify.SetVertexThicknessFromImageAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.FixOverlapsNonBranchingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.process.SmoothFilamentsAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.split.SplitFilamentsIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.StringsExtension;
import org.hkijena.jipipe.extensions.tables.TablesExtension;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Extension that adds filaments supports
 */
@Plugin(type = JIPipeJavaExtension.class)
public class FilamentsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-filaments",
            JIPipe.getJIPipeVersion(),
            "Filaments");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(FilamentsExtension.class, "org/hkijena/jipipe/extensions/ijfilaments");

    public FilamentsExtension() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, TablesExtension.AS_DEPENDENCY, StringsExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerNodeTypeCategory(new FilamentsNodeTypeCategory());

        registerDatatype("filaments", Filaments3DData.class, RESOURCES.getIcon16URLFromResources("data-type-filaments.png"));
        registerDatatypeConversion(new FilamentsToRoiDataTypeConverter());

        registerNodeType("filaments-from-json", ImportFilamentsFromJsonAlgorithm.class);
        registerNodeType("filaments-skeleton-to-filaments-2d3d-simplified", SkeletonToSimplifiedFilamentsFijiAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
        registerNodeType("filaments-skeleton-to-filaments-2d", SkeletonToFilaments2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));

        registerNodeType("filaments-set-vertex-properties", ChangeFilamentVertexPropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));

        registerNodeType("filaments-filter-vertices-by-properties", FilterFilamentVerticesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("filaments-filter-edges-by-properties", FilterFilamentEdgesByProperties.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("filaments-split-into-connected-components", SplitFilamentsIntoConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

        registerNodeType("filaments-merge", MergeFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));

        registerNodeType("filaments-convert-to-roi", ConvertFilamentsToRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));

        registerNodeType("filaments-set-vertex-thickness-from-image", SetVertexThicknessFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-circle-from-radius.png"));
        registerNodeType("filaments-remove-duplicate-vertices", RemoveDuplicateVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("filaments-smooth-downscale", SmoothFilamentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("filaments-fix-overlaps-non-branching", FixOverlapsNonBranchingAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-line-perpendicular.png"));

        registerNodeType("filaments-measure-vertices", MeasureVerticesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("filaments-measure-edges", MeasureEdgesAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
    }


}
