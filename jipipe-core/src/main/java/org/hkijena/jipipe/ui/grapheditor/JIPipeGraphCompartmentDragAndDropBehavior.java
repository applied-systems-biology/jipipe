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

package org.hkijena.jipipe.ui.grapheditor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.extensions.filesystem.datasources.FileListDataSource;
import org.hkijena.jipipe.extensions.filesystem.datasources.FolderListDataSource;
import org.hkijena.jipipe.extensions.filesystem.datasources.PathListDataSource;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphPasteNodeUIContextAction;

import javax.swing.*;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Installs Drag and Drop features that create filesystem nodes
 */
public class JIPipeGraphCompartmentDragAndDropBehavior implements JIPipeGraphDragAndDropBehavior {

    private JIPipeGraphCanvasUI canvas;

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        Point mousePosition = canvas.getMousePosition();
        if (mousePosition == null)
            return;
        Point gridLocation = canvas.getViewMode().realLocationToGrid(mousePosition, canvas.getZoom());
        Point realLocation = canvas.getViewMode().gridToRealLocation(gridLocation, canvas.getZoom());
        canvas.setGraphEditCursor(realLocation);
        canvas.repaint();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {
        if (canvas.getCurrentConnectionDragSource() != null || canvas.getCurrentConnectionDragTarget() != null) {
            dtde.rejectDrop();
            return;
        }
        try {
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            boolean accept = false;
            DataFlavor acceptedFlavor = null;
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {
                    accept = true;
                    acceptedFlavor = flavor;
                } else if (flavor.isFlavorTextType()) {
                    accept = true;
                    if (acceptedFlavor == null)
                        acceptedFlavor = flavor;
                }
            }
            if (accept) {
                dtde.acceptDrop(dtde.getDropAction());
                Object transferData = tr.getTransferData(acceptedFlavor);
                if (transferData instanceof List) {
                    List<File> files = (List<File>) transferData;
                    processDrop(files);
                    dtde.dropComplete(true);
                } else if (transferData instanceof String) {
                    String text = (String) transferData;
                    processDrop(text);
                    dtde.dropComplete(true);
                }
            }
            return;
        } catch (Throwable t) {
            t.printStackTrace();
            if (GraphEditorUISettings.getInstance().isNotifyInvalidDragAndDrop()) {
                JOptionPane.showMessageDialog(canvas, new JLabel("The dropped data is invalid. You can drop files/folders or JSON data that describes JIPipe nodes."), "Invalid drop", JOptionPane.ERROR_MESSAGE);
            }
        }
        dtde.rejectDrop();
    }

    /**
     * Processes the drop as serializable (nodes)
     *
     * @param text json
     */
    private void processDrop(String text) {
        try {
            if (text != null) {
                AlgorithmGraphPasteNodeUIContextAction.pasteNodes(canvas, text);
            }
        } catch (Exception e) {
            if (GraphEditorUISettings.getInstance().isNotifyInvalidDragAndDrop()) {
                JOptionPane.showMessageDialog(canvas.getWorkbench().getWindow(),
                        "The dropped string is no valid node/graph.",
                        "Drop nodes",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes drop to create file node(s)
     *
     * @param files the files
     */
    private void processDrop(List<File> files) {
        UUID compartment = canvas.getCompartment();
        JIPipeGraph graph = canvas.getGraph();

        boolean hasFiles = false;
        boolean hasDirectories = false;
        for (File file : files) {
            hasFiles |= file.isFile();
            hasDirectories |= file.isDirectory();
        }

        if (hasFiles && hasDirectories) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvas.getWorkbench()))
                return;
            PathListDataSource dataSource = JIPipe.createNode("import-path-list", PathListDataSource.class);
            dataSource.setPaths(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        } else if (hasFiles) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvas.getWorkbench()))
                return;
            FileListDataSource dataSource = JIPipe.createNode("import-file-list", FileListDataSource.class);
            dataSource.setFiles(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        } else if (hasDirectories) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvas.getWorkbench()))
                return;
            FolderListDataSource dataSource = JIPipe.createNode("import-folder-list", FolderListDataSource.class);
            dataSource.setFolderPaths(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        }
    }

    @Override
    public JIPipeGraphCanvasUI getCanvas() {
        return canvas;
    }

    @Override
    public void setCanvas(JIPipeGraphCanvasUI canvas) {
        this.canvas = canvas;
    }
}
