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

package org.hkijena.jipipe.plugins.tunnels;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1.JIPipeDataFlowTunnel1Entrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1.JIPipeDataFlowTunnel1Exit;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension containing some additional tools
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class TunnelsPlugin extends JIPipePrepackagedDefaultJavaPlugin {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Data flow tunnels";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides nodes for implementing tunnels.");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerNodeType("data-flow-tunnel-1-entrance", JIPipeDataFlowTunnel1Entrance.class, JIPipe.RESOURCES.getIcon16URL("actions/xfce-wm-unstick.png"));
        registerNodeType("data-flow-tunnel-1-exit", JIPipeDataFlowTunnel1Exit.class, JIPipe.RESOURCES.getIcon16URL("actions/file-zoom-in.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:tunnels";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

}
