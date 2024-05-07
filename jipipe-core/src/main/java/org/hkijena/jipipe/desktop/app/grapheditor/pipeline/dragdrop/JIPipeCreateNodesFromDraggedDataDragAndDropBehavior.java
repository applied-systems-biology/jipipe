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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.dragdrop;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphDragAndDropBehavior;
import org.hkijena.jipipe.plugins.filesystem.datasources.FileListDataSource;
import org.hkijena.jipipe.plugins.filesystem.datasources.FolderListDataSource;
import org.hkijena.jipipe.plugins.filesystem.datasources.PathListDataSource;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;

import javax.swing.*;
import java.awt.*;
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
public class JIPipeCreateNodesFromDraggedDataDragAndDropBehavior implements JIPipeDesktopGraphDragAndDropBehavior {

    private JIPipeDesktopGraphCanvasUI canvas;

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        Point mousePosition = dtde.getLocation();
        if (mousePosition == null)
            return;
        Point gridLocation = canvas.getViewMode().realLocationToGrid(mousePosition, canvas.getZoom());
        Point realLocation = canvas.getViewMode().gridToRealLocation(gridLocation, canvas.getZoom());
        canvas.setGraphEditCursor(realLocation);
        canvas.repaintLowLag();
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
            if (JIPipeGraphEditorUIApplicationSettings.getInstance().isNotifyInvalidDragAndDrop()) {
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
                canvas.pasteNodes(text);
            }
        } catch (Exception e) {
            if (JIPipeGraphEditorUIApplicationSettings.getInstance().isNotifyInvalidDragAndDrop()) {
                JOptionPane.showMessageDialog(canvas.getDesktopWorkbench().getWindow(),
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
            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvas.getDesktopWorkbench()))
                return;
            PathListDataSource dataSource = JIPipe.createNode("import-path-list");
            dataSource.setPaths(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        } else if (hasFiles) {
            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvas.getDesktopWorkbench()))
                return;
            FileListDataSource dataSource = JIPipe.createNode("import-file-list");
            dataSource.setFiles(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        } else if (hasDirectories) {
            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvas.getDesktopWorkbench()))
                return;
            FolderListDataSource dataSource = JIPipe.createNode("import-folder-list");
            dataSource.setFolderPaths(new PathList(files.stream().map(File::toPath).collect(Collectors.toList())));
            graph.insertNode(dataSource, compartment);
        }
    }

    @Override
    public JIPipeDesktopGraphCanvasUI getCanvas() {
        return canvas;
    }

    @Override
    public void setCanvas(JIPipeDesktopGraphCanvasUI canvas) {
        this.canvas = canvas;
    }
}
