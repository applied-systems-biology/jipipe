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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class JIPipeDesktopAddNodeTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JList) {
            JIPipeGraph graph = new JIPipeGraph();
            for (JIPipeNodeDatabaseEntry obj : ((JList<JIPipeNodeDatabaseEntry>) c).getSelectedValuesList()) {
                if (obj instanceof CreateNewNodeByInfoDatabaseEntry) {
                    JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) obj).getNodeInfo();
                    graph.insertNode(info.newInstance());
                } else if (obj instanceof CreateNewNodeByInfoAliasDatabaseEntry) {
                    JIPipeNodeInfo info = ((CreateNewNodeByInfoAliasDatabaseEntry) obj).getNodeInfo();
                    JIPipeGraphNode node = info.newInstance();
                    node.setCustomName(obj.getName());
                    graph.insertNode(node);
                } else if (obj instanceof CreateNewNodeByExampleDatabaseEntry) {
                    JIPipeNodeExample example = ((CreateNewNodeByExampleDatabaseEntry) obj).getExample();
                    JIPipeNodeInfo info = example.getNodeInfo();
                    JIPipeGraphNode node = info.newInstance();
                    node.setCustomName(info.getName() + ": " + example.getNodeTemplate().getName());
                    if (node instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) node).loadExample(example);
                    }
                    graph.insertNode(node);
                } else if (obj instanceof CreateNewNodesByTemplateDatabaseEntry) {
                    graph.mergeWith(((CreateNewNodesByTemplateDatabaseEntry) obj).getTemplate().getGraph());
                } else if (obj instanceof CreateNewCompartmentNodeDatabaseEntry) {
                    graph.insertNode(JIPipe.createNode(JIPipeProjectCompartment.class));
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
