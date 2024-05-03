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

package org.hkijena.jipipe.plugins.pipelinerender;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension containing some additional tools
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class PipelineRenderPlugin extends JIPipePrepackagedDefaultJavaPlugin {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Render pipeline tool";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("A tool to generate a high-resolution render of the fulll pipeline.");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerMenuExtension(PipelineRenderTool.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:pipeline-render";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }

}
