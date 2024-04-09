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

package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;

/**
 * Extension for anything that is related to {@link JIPipeGraphWrapperAlgorithm}
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class LoopingPlugin extends JIPipePrepackagedDefaultJavaPlugin {

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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerNodeType("loop:start", LoopStartNode.class, UIUtils.getIconURLFromResources("actions/run-build.png"));
        registerNodeType("loop:end", LoopEndNode.class, UIUtils.getIconURLFromResources("actions/run-build-prune.png"));
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:looping";
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }
}
