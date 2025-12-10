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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.forms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.forms.datatypes.FormData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.LUTManagerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.maskdrawer.MaskDrawerPlugin2D;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Structural {@link FormData} for drawing masks
 */
public class MaskDrawerFormData extends FormData {

    private final List<JIPipeMultiIterationStep> iterationSteps;
    private final InteractiveDrawMaskAlgorithm drawMaskAlgorithm;
    private JPanel imageViewerPanelContainer;
    private JIPipeDesktopLegacyImageViewer imageViewerPanel;
    private MaskDrawerPlugin2D maskDrawerPlugin;
    private ImagePlus lazyLoadedImage;
    private ImagePlus lazyLoadedMask;

    public MaskDrawerFormData(List<JIPipeMultiIterationStep> iterationSteps, InteractiveDrawMaskAlgorithm drawMaskAlgorithm) {
        this.iterationSteps = iterationSteps;
        this.drawMaskAlgorithm = drawMaskAlgorithm;
    }

    private void initializeImageViewer() {
        imageViewerPanelContainer = new JPanel(new BorderLayout(8,8));
        JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
        JIPipeDesktopRibbon ribbon = new  JIPipeDesktopRibbon();
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        imageViewerPanelContainer.add(dockPanel, BorderLayout.CENTER);
        imageViewerPanelContainer.add(ribbon, BorderLayout.NORTH);
        imageViewerPanelContainer.add(toolBar, BorderLayout.SOUTH);

        imageViewerPanel = new JIPipeDesktopLegacyImageViewer(new JIPipeDesktopDummyWorkbench(),
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class,
                        LUTManagerPlugin2D.class,
                        MaskDrawerPlugin2D.class),
                null,
                Collections.emptyMap());
        dockPanel.setMainComponent(imageViewerPanel);

        imageViewerPanel.buildDock(dockPanel);
        imageViewerPanel.buildStatusBar(toolBar);
        imageViewerPanel.buildRibbon(ribbon);

        maskDrawerPlugin = imageViewerPanel.getPlugin(MaskDrawerPlugin2D.class);
        if (lazyLoadedImage != null) {
            imageViewerPanel.setImageData(new ImagePlusData(lazyLoadedImage));
            maskDrawerPlugin.setMask(lazyLoadedMask);
            lazyLoadedImage = null;
            lazyLoadedMask = null;
        }

        ribbon.rebuildRibbon();
        ribbon.selectTask("Mask");
    }

    public JIPipeDesktopLegacyImageViewer getImageViewerPanel() {
        if (imageViewerPanel == null || maskDrawerPlugin == null) {
            initializeImageViewer();
        }
        return imageViewerPanel;
    }

    public JPanel getImageViewerPanelContainer() {
        if (imageViewerPanel == null || maskDrawerPlugin == null) {
            initializeImageViewer();
        }
        return imageViewerPanelContainer;
    }

    @Override
    public boolean isUsingCustomCopy() {
        return true;
    }

    @Override
    public void customCopy(FormData source, CustomValidationReportContext context, JIPipeValidationReport report) {
        // Initialize the viewer
        getImageViewerPanel();

        MaskDrawerFormData sourceData = (MaskDrawerFormData) source;
        ImagePlus sourceMask = sourceData.maskDrawerPlugin.getMask();
        ImagePlus targetMask = maskDrawerPlugin.getMask();

        if (!ImageJUtils.imagesHaveSameSize(sourceMask, targetMask)) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                    "Could not copy mask due to different sizes!",
                    "The source mask is " + sourceMask + " and cannot be copied into the target " + targetMask,
                    "Ensure that the masks have the same size"));
            return;
        }

        ImageJUtils.copyPixelsBetweenImages(sourceMask, targetMask, new JIPipeProgressInfo());
        maskDrawerPlugin.recalculateMaskPreview();
    }

    @Override
    public boolean isUsingCustomReset() {
        return true;
    }

    @Override
    public void customReset() {
        // Initialize the viewer
        getImageViewerPanel();

        ImagePlus targetMask = maskDrawerPlugin.getMask();
        ImageJIterationUtils.forEachIndexedZCTSlice(targetMask, (targetProcessor, index) -> {
            targetProcessor.setValue(0);
            targetProcessor.fillRect(0, 0, targetProcessor.getWidth(), targetProcessor.getHeight());
        }, new JIPipeProgressInfo());
        maskDrawerPlugin.recalculateMaskPreview();
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        // Initialize the viewer
        getImageViewerPanel();
        return new MaskDrawerFormData(iterationSteps, drawMaskAlgorithm);
    }

    @Override
    public Component getEditor(JIPipeDesktopWorkbench workbench) {
        JPanel panel = getImageViewerPanelContainer();
        maskDrawerPlugin.recalculateMaskPreview();
        return panel;
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        int row = iterationSteps.indexOf(iterationStep);
        ImagePlus referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, new JIPipeProgressInfo()).getFirst().getImage();
        ImagePlus maskImage = drawMaskAlgorithm.getOutputSlot("Mask").getData(row, ImagePlusGreyscaleMaskData.class, new JIPipeProgressInfo()).getImage();

        if (imageViewerPanel != null) {
            imageViewerPanel.setImagePlus(referenceImage);
            maskDrawerPlugin.setMask(maskImage);
        } else {
            lazyLoadedImage = referenceImage;
            lazyLoadedMask = maskImage;
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {

    }
}
