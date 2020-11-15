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

package org.hkijena.jipipe.extensions.utils;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.utils.algorithms.ConverterAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.SortRowsAlgorithm;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class UtilitiesExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Utilities";
    }

    @Override
    public String getDescription() {
        return "Utility algorithms";
    }

    @Override
    public void register() {
        registerNodeType("io-interface", IOInterfaceAlgorithm.class, UIUtils.getIconURLFromResources("devices/knemo-wireless-transmit-receive.png"));
        registerNodeType("node-group", NodeGroup.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerNodeType("converter", ConverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-refresh.png"));
        registerNodeType("sort-rows-by-annotation", SortRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/sort-name.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:utils";
    }

    @Override
    public String getDependencyVersion() {
        return "2020.11";
    }
}
