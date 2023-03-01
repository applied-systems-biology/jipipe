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

package org.hkijena.jipipe.extensions.ij3d;

import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.ij3d.compat.ROI3DImageJExporter;
import org.hkijena.jipipe.extensions.ij3d.compat.ROI3DImageJImporter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.display.AddROI3DToManagerOperation;
import org.hkijena.jipipe.extensions.ij3d.nodes.ImportROI3D;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert.Roi2DToRoi3DAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.filter.FilterRoi3DByOverlapAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.filter.FilterRoi3DListsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.filter.FilterRoi3DByStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.generate.FindParticles3DAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.generate.Roi3DFromLabelsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.measure.ExtractRoi3DRelationStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.measure.ExtractRoi3DStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.merge.MergeROI3DAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.modify.ChangeRoi3DPropertiesFromExpressionsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.modify.ROI3DCalculatorAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.split.ExplodeRoi3DListAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.split.SplitRoi3DIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DMeasurement;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurement;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class IJ3DExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/extensions/ij3d";

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(IJ3DExtension.class, "org/hkijena/jipipe/extensions/ij3d");

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-3d",
            JIPipe.getJIPipeVersion(),
            "3D ImageJ Suite integration");

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("J. Ollion, J. Cochennec, F. Loll, C. Escudé, T. Boudier. (2013) TANGO: A Generic Tool for High-throughput 3D Image Analysis for Studying Nuclear Organization. Bioinformatics 2013 Jul 15;29(14):1840-1.");
        return strings;
    }

    @Override
    public String getName() {
        return "3D ImageJ Suite integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates the 3D ImageJ Suite into JIPipe");
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.singletonList(new JIPipeImageJUpdateSiteDependency("3D ImageJ Suite", "https://sites.imagej.net/Tboudier/"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        registerEnumParameterType("ij3d-measurement", ROI3DMeasurement.class, "3D Measurement", "A 3D object measurement");
        registerParameterType("ij3d-measurement-set", ROI3DMeasurementSetParameter.class, "3D Measurements", "A selection of 3D object measurements");
        registerEnumParameterType("ij3d-relation-measurement", ROI3DRelationMeasurement.class, "3D relation measurement", "Relation between two 3D objects");
        registerParameterType("ij3d-relation-measurement-set", ROI3DRelationMeasurementSetParameter.class, "3D relation measurements", "A selection of measurements between two 3D objects");

        registerDatatype("roi-3d-list", ROI3DListData.class, RESOURCES.getIcon16URLFromResources("data-type-roi3d.png"), new AddROI3DToManagerOperation());
        registerImageJDataImporter("import-roi-3d", new ROI3DImageJImporter(), null);
        registerImageJDataExporter("export-roi-3d", new ROI3DImageJExporter(), null);
        registerNodeType("import-roi-3d", ImportROI3D.class);

        registerNodeType("ij3d-analyze-find-particles", FindParticles3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));
        registerNodeType("ij3d-labels-to-roi", Roi3DFromLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));

        registerNodeType("ij3d-measure-roi3d", ExtractRoi3DStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij3d-measure-pairwise-roi3d", ExtractRoi3DRelationStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerNodeType("ij3d-filter-roi3d-list", FilterRoi3DListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij3d-filter-roi3d-by-statistics", FilterRoi3DByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-split-explode", ExplodeRoi3DListAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij3d-roi-split-into-connected-components", SplitRoi3DIntoConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

        registerNodeType("ij3d-roi-filter-by-overlap", FilterRoi3DByOverlapAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-merge", MergeROI3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/rabbitvcs-merge.png"));

        registerNodeType("ij3d-roi-change-properties-from-expressions", ChangeRoi3DPropertiesFromExpressionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-calculator", ROI3DCalculatorAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));

        registerNodeType("ij3d-roi-convert-2d-to-3d", Roi2DToRoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-cuboid.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-3d";
    }

}
