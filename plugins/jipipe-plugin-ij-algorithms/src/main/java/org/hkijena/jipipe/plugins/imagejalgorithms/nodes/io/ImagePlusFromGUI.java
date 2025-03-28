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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.io;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.ImageQueryExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.util.LogicalOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Imports {@link org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData} from the GUI
 */
@SetJIPipeDocumentation(name = "Image from ImageJ", description = "Imports one or multiple active ImageJ image windows into JIPipe")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nImport")
public class ImagePlusFromGUI extends JIPipeSimpleIteratingAlgorithm {

    private boolean onlyActiveImage = true;
    private JIPipeExpressionParameter imageFilters = new JIPipeExpressionParameter("");
    private LogicalOperation imageFiltersOperation = LogicalOperation.LogicalOr;

    public ImagePlusFromGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ImagePlusFromGUI(ImagePlusFromGUI other) {
        super(other);
        this.onlyActiveImage = other.onlyActiveImage;
        this.imageFilters = new JIPipeExpressionParameter(other.imageFilters);
        this.imageFiltersOperation = other.imageFiltersOperation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> rawImages = new ArrayList<>();
        if (onlyActiveImage) {
            ImagePlus img = WindowManager.getCurrentImage();
            if (img != null) {
                rawImages.add(img);
            }
        } else {
            for (int i = 0; i < WindowManager.getImageCount(); i++) {
                int id = WindowManager.getNthImageID(i + 1);
                ImagePlus img = WindowManager.getImage(id);
                if (img != null) {
                    rawImages.add(img);
                }
            }
        }
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
        for (ImagePlus rawImage : rawImages) {
            ImageQueryExpressionVariablesInfo.buildVariablesSet(rawImage, variableSet);
            if (imageFilters.test(variableSet)) {
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(rawImage).duplicate(progressInfo), progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Only extract active image", description = "If enabled, the currently active image is extracted. There is always only one image active. If no active image is present, " +
            "the node will generate no outputs.")
    @JIPipeParameter("only-active-image")
    public boolean isOnlyActiveImage() {
        return onlyActiveImage;
    }

    @JIPipeParameter("only-active-image")
    public void setOnlyActiveImage(boolean onlyActiveImage) {
        this.onlyActiveImage = onlyActiveImage;
    }

    @SetJIPipeDocumentation(name = "Filter images", description = "Expression to filter the image(s). Each image is tested individually and added imported based on the test results. The expression should return a boolean value. Example: <pre>(title CONTAINS \"data\") AND (depth > 3). Defaults to 'TRUE'</pre>")
    @JIPipeParameter("image-filters")
    @JIPipeExpressionParameterSettings(variableSource = ImageQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getImageFilters() {
        return imageFilters;
    }

    @JIPipeParameter("image-filters")
    public void setImageFilters(JIPipeExpressionParameter imageFilters) {
        this.imageFilters = imageFilters;
    }

    @SetJIPipeDocumentation(name = "Filter images operation", description = "Determines how the 'Filter images' operations are connected.")
    @JIPipeParameter("image-filters-mode")
    public LogicalOperation getImageFiltersOperation() {
        return imageFiltersOperation;
    }

    @JIPipeParameter("image-filters-mode")
    public void setImageFiltersOperation(LogicalOperation imageFiltersOperation) {
        this.imageFiltersOperation = imageFiltersOperation;
    }
}
