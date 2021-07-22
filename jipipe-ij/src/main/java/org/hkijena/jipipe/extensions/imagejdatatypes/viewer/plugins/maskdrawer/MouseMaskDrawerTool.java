package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.EDM;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Cursor;
import java.nio.file.Path;

/**
 * The standard mouse selection.
 * Allows left-click canvas dragging
 */
public class MouseMaskDrawerTool extends MaskDrawerTool {
    public MouseMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Panning",
                "Allows to drag the canvas with the left mouse",
                UIUtils.getIconFromResources("actions/hand.png"));
    }

    @Override
    public void activate() {
        getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        getViewerPanel().getCanvas().setDragWithLeftMouse(true);
    }

    @Override
    public void deactivate() {

    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        addAlgorithmButton(formPanel,
                "Export mask",
                "Exports this mask",
                UIUtils.getIconFromResources("actions/document-export.png"),
                this::exportMask);
        addAlgorithmButton(formPanel,
                "Import mask",
                "Imports this mask",
                UIUtils.getIconFromResources("actions/document-import.png"),
                this::importMask);
        addAlgorithmButton(formPanel,
                "Invert mask",
                "Inverts the current mask",
                UIUtils.getIconFromResources("actions/invertimage.png"),
                this::applyInvert);
        addAlgorithmButton(formPanel,
                "Apply watershed",
                "Applies distance transform watershed",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::applyWatershed);
        addAlgorithmButton(formPanel,
                "Apply morphological dilate",
                "Applies a morphological dilation operation (3x3 mask)",
                UIUtils.getIconFromResources("actions/morphology.png"),
                this::applyDilate);
        addAlgorithmButton(formPanel,
                "Apply morphological erode",
                "Applies a morphological erode operation (3x3 mask)",
                UIUtils.getIconFromResources("actions/morphology.png"),
                this::applyErode);
        addAlgorithmButton(formPanel,
                "Apply morphological open",
                "Applies a morphological open operation (3x3 mask)",
                UIUtils.getIconFromResources("actions/morphology.png"),
                this::applyOpen);
        addAlgorithmButton(formPanel,
                "Apply morphological close",
                "Applies a morphological close operation (3x3 mask)",
                UIUtils.getIconFromResources("actions/morphology.png"),
                this::applyClose);
    }

    private void applyInvert() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getMaskDrawerPlugin().getCurrentMaskSlice();
            processor.invert();
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyOpen() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getMaskDrawerPlugin().getCurrentMaskSlice();
            // Dilate and erode are switched for some reason
            processor.dilate(); // Erode
            processor.erode(); // Dilate
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyClose() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getMaskDrawerPlugin().getCurrentMaskSlice();
            // Dilate and erode are switched for some reason
            processor.erode(); // Dilate
            processor.dilate(); // Erode
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyDilate() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getMaskDrawerPlugin().getCurrentMaskSlice();
            processor.erode(); // Dilate and erode are switched for some reason
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyErode() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getMaskDrawerPlugin().getCurrentMaskSlice();
            processor.erode(); // Dilate and erode are switched for some reason
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void importMask() {
        Path selectedFile = FileChooserSettings.openFile(getViewerPanel(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Import mask",
                UIUtils.EXTENSION_FILTER_TIFF);
        if (selectedFile != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus image = IJ.openImage(selectedFile.toString());
                ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
                if (image.getWidth() != processor.getWidth() || image.getHeight() != processor.getHeight()) {
                    JOptionPane.showMessageDialog(getViewerPanel(),
                            "The imported mask must have a size of " + processor.getWidth() + "x" + processor.getHeight(),
                            "Import mask",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (image.getBitDepth() != 8) {
                    ImageConverter ic = new ImageConverter(image);
                    ic.convertToGray8();
                }
                processor.setRoi((Roi) null);
                processor.copyBits(image.getProcessor(), 0, 0, Blitter.COPY);
                getMaskDrawerPlugin().recalculateMaskPreview();
                postMaskChangedEvent();
            }
        }
    }

    @Override
    public boolean showGuides() {
        return false;
    }

    private void exportMask() {
        Path selectedFile = FileChooserSettings.saveFile(getViewerPanel(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Export mask",
                UIUtils.EXTENSION_FILTER_TIFF);
        if (selectedFile != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus image = new ImagePlus("Mask", getMaskDrawerPlugin().getCurrentMaskSlice());
                IJ.saveAsTiff(image, selectedFile.toString());
            }
        }
    }

    private void addAlgorithmButton(FormPanel formPanel, String name, String description, Icon icon, Runnable function) {
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setToolTipText(description);
        button.addActionListener(e -> function.run());
        formPanel.addToForm(button, new JLabel(), null);
    }

    private void applyWatershed() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            EDM edm = new EDM();
            edm.toWatershed(getMaskDrawerPlugin().getCurrentMaskSlice());
            getMaskDrawerPlugin().recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }
}
