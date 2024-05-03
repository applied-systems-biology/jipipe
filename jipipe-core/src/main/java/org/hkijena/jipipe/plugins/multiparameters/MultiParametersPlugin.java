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

package org.hkijena.jipipe.plugins.multiparameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.multiparameters.converters.ParametersDataToResultsTableDataConverter;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.multiparameters.nodes.ConvertParametersToTableAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class MultiParametersPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Multi parameters data types";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Extension that provides the necessary data types for handling multiple parameters");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        // Register data types
        registerDatatype("parameters", ParametersData.class,
                ResourceUtils.getPluginResource("icons/data-types/parameters.png"));
        registerDatatypeConversion(new ParametersDataToResultsTableDataConverter());

        // Register nodes
        registerNodeType("convert-parameters-data-to-results-table-data", ConvertParametersToTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/results-table.png"));
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:multi-parameters";
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }

}
