package org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CoreFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;

@SetJIPipeDocumentation(name = "Tunnel exit", description = "Outputs the data sent through the matching tunnel as a collection of data items, taking no direct input and passing it unchanged.")
@ConfigureJIPipeNode(function = GraphWrappersFunction.class, framework = CoreFramework.class)
@AddJIPipeOutputSlot(name = "T1", value = JIPipeData.class, create = true)
public class JIPipeDataFlowTunnel1Exit extends JIPipeDataFlowTunnelExit {
    public JIPipeDataFlowTunnel1Exit(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeDataFlowTunnel1Exit(JIPipeDataFlowTunnel1Exit other) {
        super(other);
    }

    @Override
    public String getTunnelKeyGroup() {
        return "tunnel-1";
    }

    @Override
    public JIPipeOutputDataSlot getOutputForInput(String inputSlotName) {
        return getFirstOutputSlot();
    }
}
