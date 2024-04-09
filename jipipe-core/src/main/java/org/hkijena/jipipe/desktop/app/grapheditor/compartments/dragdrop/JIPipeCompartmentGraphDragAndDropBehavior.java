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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments.dragdrop;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphDragAndDropBehavior;
import org.hkijena.jipipe.plugins.settings.GraphEditorUISettings;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

/**
 * Installs Drag and Drop features that create filesystem nodes
 */
public class JIPipeCompartmentGraphDragAndDropBehavior implements JIPipeDesktopGraphDragAndDropBehavior {

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
//        try {
//            Transferable tr = dtde.getTransferable();
//            DataFlavor[] flavors = tr.getTransferDataFlavors();
//            for (int i = 0; i < flavors.length; i++) {
//                if (flavors[i].isFlavorJavaFileListType()) {
//                    dtde.acceptDrop(dtde.getDropAction());
//                    @SuppressWarnings("unchecked")
//                    List<File> files = (List<File>) tr.getTransferData(flavors[i]);
//                    processDrop(files);
//
//                    dtde.dropComplete(true);
//                }
//            }
//            return;
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
        if (canvas.getCurrentConnectionDragSource() == null && GraphEditorUISettings.getInstance().isNotifyInvalidDragAndDrop()) {
            String message = "<html>You probably wanted to drop some data into this graph.<br/>" +
                    "This is not possible, as the <strong>Compartment Graph</strong> only organizes your project into " +
                    "multiple sections.<br/><br/>Please double-click a node inside this graph to edit the pipeline.</html>";
            JOptionPane.showMessageDialog(canvas, new JLabel(message), "Drag & drop not supported", JOptionPane.ERROR_MESSAGE);
        }
        dtde.rejectDrop();
    }

//    private void processDrop(List<File> files) {
//        String compartment = canvas.getCompartment();
//        JIPipeGraph graph = canvas.getGraph();
//        if (files.size() == 1) {
//            File selected = files.get(0);
//            if (selected.isDirectory()) {
//                FolderDataSource dataSource = JIPipe.createNode("import-folder", FolderDataSource.class);
//                dataSource.setFolderPath(selected.toPath());
//                graph.insertNode(dataSource, compartment);
//            } else {
//                FileDataSource dataSource = JIPipe.createNode("import-file", FileDataSource.class);
//                dataSource.setFileName(selected.toPath());
//                graph.insertNode(dataSource, compartment);
//            }
//        } else {
//            Map<Boolean, List<File>> groupedByType = files.stream().collect(Collectors.groupingBy(File::isDirectory));
//            for (Map.Entry<Boolean, List<File>> entry : groupedByType.entrySet()) {
//                if (entry.getKey()) {
//                    FolderListDataSource dataSource = JIPipe.createNode("import-folder-list", FolderListDataSource.class);
//                    for (File file : entry.getValue()) {
//                        dataSource.getFolderPaths().add(file.toPath());
//                    }
//                    graph.insertNode(dataSource, compartment);
//                } else {
//                    FileListDataSource dataSource = JIPipe.createNode("import-file-list", FileListDataSource.class);
//                    for (File file : entry.getValue()) {
//                        dataSource.getFileNames().add(file.toPath());
//                    }
//                    graph.insertNode(dataSource, compartment);
//                }
//            }
//
//        }
//    }

    @Override
    public JIPipeDesktopGraphCanvasUI getCanvas() {
        return canvas;
    }

    @Override
    public void setCanvas(JIPipeDesktopGraphCanvasUI canvas) {
        this.canvas = canvas;
    }
}
