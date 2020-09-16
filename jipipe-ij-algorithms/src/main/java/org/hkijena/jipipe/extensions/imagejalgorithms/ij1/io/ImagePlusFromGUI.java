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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Imports {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData} from the GUI
 */
@JIPipeDocumentation(name = "Image from ImageJ", description = "Imports one or multiple active ImageJ image windows into JIPipe")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ImagePlusFromGUI extends JIPipeSimpleIteratingAlgorithm {

    private boolean onlyActiveImage = true;
    private StringPredicate.List imageFilters = new StringPredicate.List();
    private LogicalOperation imageFiltersOperation = LogicalOperation.LogicalOr;

    public ImagePlusFromGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ImagePlusFromGUI(ImagePlusFromGUI other) {
        super(other);
        this.onlyActiveImage = other.onlyActiveImage;
        this.imageFilters = new StringPredicate.List(other.imageFilters);
        this.imageFiltersOperation = other.imageFiltersOperation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        List<ImagePlus> rawImages = new ArrayList<>();
        if(onlyActiveImage) {
            ImagePlus img = WindowManager.getCurrentImage();
            if(img != null) {
                rawImages.add(img);
            }
        }
        else {
            for (int i = 0; i < WindowManager.getImageCount(); i++) {
                int id = WindowManager.getNthImageID(i + 1);
                ImagePlus img = WindowManager.getImage(id);
                if(img != null) {
                    rawImages.add(img);
                }
            }
        }
        for (ImagePlus rawImage : rawImages) {
            String imageString = rawImage.toString();
            if(!imageFilters.isEmpty()) {
                List<Boolean> predicateResults = new ArrayList<>();
                for (StringPredicate filter : imageFilters) {
                    predicateResults.add(filter.test(imageString));
                }
                if(!imageFiltersOperation.apply(predicateResults))
                    continue;
            }
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(rawImage).duplicate());
        }
    }

    @JIPipeDocumentation(name = "Only extract active image", description = "If enabled, the currently active image is extracted. There is always only one image active. If no active image is present, " +
            "the node will generate no outputs.")
    @JIPipeParameter("only-active-image")
    public boolean isOnlyActiveImage() {
        return onlyActiveImage;
    }

    @JIPipeParameter("only-active-image")
    public void setOnlyActiveImage(boolean onlyActiveImage) {
        this.onlyActiveImage = onlyActiveImage;
    }

    @JIPipeDocumentation(name = "Filter images", description = "Predicates to filter the list of images by their string representation. The string representation includes the title, resolution, and bit depth. If no filter is in the list, images will not be filtered.")
    @JIPipeParameter("image-filters")
    public StringPredicate.List getImageFilters() {
        return imageFilters;
    }

    @JIPipeParameter("image-filters")
    public void setImageFilters(StringPredicate.List imageFilters) {
        this.imageFilters = imageFilters;
    }

    @JIPipeDocumentation(name = "Filter images operation", description = "Determines how the 'Filter images' operations are connected.")
    @JIPipeParameter("image-filters-mode")
    public LogicalOperation getImageFiltersOperation() {
        return imageFiltersOperation;
    }

    @JIPipeParameter("image-filters-mode")
    public void setImageFiltersOperation(LogicalOperation imageFiltersOperation) {
        this.imageFiltersOperation = imageFiltersOperation;
    }
}
