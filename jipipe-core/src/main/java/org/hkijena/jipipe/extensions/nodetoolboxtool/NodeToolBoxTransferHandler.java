package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class NodeToolBoxTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JList) {
            JIPipeGraph graph = new JIPipeGraph();
            for (JIPipeNodeDatabaseEntry obj : ((JList<JIPipeNodeDatabaseEntry>) c).getSelectedValuesList()) {
                if(obj instanceof CreateNewNodeByInfoDatabaseEntry) {
                    JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) obj).getNodeInfo();
                    graph.insertNode(info.newInstance());
                }
                else if(obj instanceof CreateNewNodeByExampleDatabaseEntry) {
                    JIPipeNodeExample example = ((CreateNewNodeByExampleDatabaseEntry) obj).getExample();
                    JIPipeNodeInfo info = example.getNodeInfo();
                    JIPipeGraphNode node = info.newInstance();
                    if (node instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) node).loadExample(example);
                    }
                    graph.insertNode(node);
                }
            }
            String json = JsonUtils.toJsonString(graph);
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{DataFlavor.stringFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return flavor.isFlavorTextType();
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    return json;
                }
            };
        }
        return null;
    }
}
