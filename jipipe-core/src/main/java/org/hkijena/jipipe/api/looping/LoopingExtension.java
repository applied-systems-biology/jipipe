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

package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension for anything that is related to {@link GraphWrapperAlgorithm}
 */
@Plugin(type = JIPipeJavaExtension.class)
public class LoopingExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Node looping";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides nodes for controlling loops");
    }

    @Override
    public void register() {
        registerNodeType("loop:start", LoopStartNode.class, UIUtils.getIconURLFromResources("actions/run-build.png"));
        registerNodeType("loop:end", LoopEndNode.class, UIUtils.getIconURLFromResources("actions/run-build-prune.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:looping";
    }

    @Override
    public String getDependencyVersion() {
        return "1.59.0";
    }
}
