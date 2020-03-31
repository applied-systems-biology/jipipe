package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFileDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFileListDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFolderListDataSource;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Installs Drag&Drop features that create filesystem nodes
 */
public class ACAQAlgorithmGraphUIDragAndDrop implements DropTargetListener {

    private ACAQAlgorithmGraphCanvasUI canvasUI;

    private ACAQAlgorithmGraphUIDragAndDrop(ACAQAlgorithmGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

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
        String compartment = canvasUI.getCompartment();
        ACAQAlgorithmGraph graph = canvasUI.getAlgorithmGraph();
        if (files.size() == 1) {
            File selected = files.get(0);
            if (selected.isDirectory()) {
                ACAQFolderDataSource dataSource = ACAQAlgorithm.newInstance("import-folder");
                dataSource.setFolderPath(selected.toPath());
                graph.insertNode(dataSource, compartment);
            } else {
                ACAQFileDataSource dataSource = ACAQAlgorithm.newInstance("import-file");
                dataSource.setFileName(selected.toPath());
                graph.insertNode(dataSource, compartment);
            }
        } else {
            Map<Boolean, List<File>> groupedByType = files.stream().collect(Collectors.groupingBy(File::isDirectory));
            for (Map.Entry<Boolean, List<File>> entry : groupedByType.entrySet()) {
                if (entry.getKey()) {
                    ACAQFolderListDataSource dataSource = ACAQAlgorithm.newInstance("import-folder-list");
                    for (File file : entry.getValue()) {
                        dataSource.getFolderPaths().add(file.toPath());
                    }
                    graph.insertNode(dataSource, compartment);
                } else {
                    ACAQFileListDataSource dataSource = ACAQAlgorithm.newInstance("import-file-list");
                    for (File file : entry.getValue()) {
                        dataSource.getFileNames().add(file.toPath());
                    }
                    graph.insertNode(dataSource, compartment);
                }
            }

        }
    }

    /**
     * Installs the drag&drop into the specified {@link ACAQAlgorithmGraphCanvasUI}
     * @param canvasUI The target
     */
    public static void install(ACAQAlgorithmGraphCanvasUI canvasUI) {
        new DropTarget(canvasUI, new ACAQAlgorithmGraphUIDragAndDrop(canvasUI));
    }
}
