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

package org.hkijena.jipipe.api.nodes.taxonomy.functions.rois;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.RoisFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Set;

/**
 * Seed leaf of the ROIs branch: <em>editing</em> ROI properties and
 * metadata - setting a property or an associated image on the ROI, and
 * reading/extracting ROI properties or metadata (into a table, or
 * back-and-forth with the data).
 * <p>
 * This leaf separates the "change what the ROI <em>is</em>" nodes from the
 * "select which ROIs <em>survive</em>" nodes of
 * {@link RoiFiltersFunction}. The former had no dedicated leaf (the ROI
 * branch's seed tree pre-dated the audit), so they were misfiled under
 * "ROI filters" - e.g. "Set 2D ROI properties" is not a filter, and neither
 * is "Clear 2D ROI image".
 */
public final class RoiPropertiesFunction implements JIPipeNodeTaxonomyFunction {

    /**
     * The stable registry id of this function (a string literal per the stable-ID rule).
     */
    public static final String ID = "jipipe:function:roi-properties";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "ROI properties";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new RoisFunction();
    }

    @Override
    public String getDescription() {
        return "Editing ROI properties and metadata (set/get a property or image, extract to table)";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public boolean isVisibleInPipeline() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }
}
