package org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;

@SetJIPipeDocumentation(name = "Tunnel entrance", description = "")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Organization")
@AddJIPipeInputSlot(name = "A", value = JIPipeData.class, create = true, optional = true)
public class JIPipeDataFlowTunnel1Entrance extends JIPipeDataFlowTunnelEntrance {

    public JIPipeDataFlowTunnel1Entrance(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeDataFlowTunnel1Entrance(JIPipeDataFlowTunnel1Entrance other) {
        super(other);
    }

    @Override
    public String getTunnelKeyGroup() {
        return "tunnel-1";
    }


}
