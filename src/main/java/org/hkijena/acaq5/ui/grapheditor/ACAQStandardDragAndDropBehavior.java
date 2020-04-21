package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.FileDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.FileListDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.FolderDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.FolderListDataSource;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Installs Drag&Drop features that create filesystem nodes
 */
public class ACAQStandardDragAndDropBehavior implements ACAQAlgorithmGraphDragAndDropBehavior {

    private ACAQAlgorithmGraphCanvasUI canvas;

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {
        try {
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            ArrayList<File> fileNames = new ArrayList<File>();
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].isFlavorJavaFileListType()) {
                    dtde.acceptDrop(dtde.getDropAction());
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) tr.getTransferData(flavors[i]);
                    processDrop(files);

                    dtde.dropComplete(true);
                }
            }
            return;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        dtde.rejectDrop();
    }

    private void processDrop(List<File> files) {
        String compartment = canvas.getCompartment();
        ACAQAlgorithmGraph graph = canvas.getAlgorithmGraph();
        if (files.size() == 1) {
            File selected = files.get(0);
            if (selected.isDirectory()) {
                FolderDataSource dataSource = ACAQAlgorithm.newInstance("import-folder");
                dataSource.setFolderPath(selected.toPath());
                graph.insertNode(dataSource, compartment);
            } else {
                FileDataSource dataSource = ACAQAlgorithm.newInstance("import-file");
                dataSource.setFileName(selected.toPath());
                graph.insertNode(dataSource, compartment);
            }
        } else {
            Map<Boolean, List<File>> groupedByType = files.stream().collect(Collectors.groupingBy(File::isDirectory));
            for (Map.Entry<Boolean, List<File>> entry : groupedByType.entrySet()) {
                if (entry.getKey()) {
                    FolderListDataSource dataSource = ACAQAlgorithm.newInstance("import-folder-list");
                    for (File file : entry.getValue()) {
                        dataSource.getFolderPaths().add(file.toPath());
                    }
                    graph.insertNode(dataSource, compartment);
                } else {
                    FileListDataSource dataSource = ACAQAlgorithm.newInstance("import-file-list");
                    for (File file : entry.getValue()) {
                        dataSource.getFileNames().add(file.toPath());
                    }
                    graph.insertNode(dataSource, compartment);
                }
            }

        }
    }

    @Override
    public ACAQAlgorithmGraphCanvasUI getCanvas() {
        return canvas;
    }

    @Override
    public void setCanvas(ACAQAlgorithmGraphCanvasUI canvas) {
        this.canvas = canvas;
    }
}
