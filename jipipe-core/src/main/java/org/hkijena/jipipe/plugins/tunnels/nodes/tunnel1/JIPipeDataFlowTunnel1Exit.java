package org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;

@SetJIPipeDocumentation(name = "Tunnel exit", description = "")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Organization")
@AddJIPipeOutputSlot(name = "A", value = JIPipeData.class, create = true)
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
}
