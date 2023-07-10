package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.forms;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.forms.ui.FormsDialog;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Draw/modify mask", description = "Allows users to draw or modify a mask that is drawn over a reference image." +
        " You can supply existing masks via the 'Masks' input. If a data batch has no existing mask, a new one is generated according to the " +
        "node parameters.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Forms")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
public class DrawMaskAlgorithm extends JIPipeIteratingMissingDataGeneratorAlgorithm {

    private OptionalIntegerParameter overwriteSizeZ = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeC = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeT = new OptionalIntegerParameter(false, 1);

    public DrawMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DrawMaskAlgorithm(DrawMaskAlgorithm other) {
        super(other);
        this.overwriteSizeZ = new OptionalIntegerParameter(other.overwriteSizeZ);
        this.overwriteSizeC = new OptionalIntegerParameter(other.overwriteSizeC);
        this.overwriteSizeT = new OptionalIntegerParameter(other.overwriteSizeT);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        // Generate the output first
        super.runParameterSet(progressInfo, parameterAnnotations);

        if (isPassThrough())
            return;

        // Get back the data batches
        List<JIPipeMergingDataBatch> dataBatches;

        // No input slots -> Nothing to do
        if (getDataInputSlotCount() == 0) {
            return;
        } else if (getDataInputSlotCount() == 1) {
            dataBatches = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled())
                    break;
                JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
                dataBatch.setInputData(getFirstInputSlot(), row);
                dataBatch.addMergedTextAnnotations(parameterAnnotations, getDataBatchGenerationSettings().getAnnotationMergeStrategy());
                dataBatch.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(row), getDataBatchGenerationSettings().getAnnotationMergeStrategy());
                dataBatches.add(dataBatch);
            }
        } else {
            dataBatches = generateDataBatchesDryRun(getNonParameterInputSlots(), progressInfo);
        }

        runForm(dataBatches, progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    private void runForm(List<JIPipeMergingDataBatch> dataBatches, JIPipeProgressInfo progressInfo) {
        if (dataBatches.isEmpty()) {
            progressInfo.log("No data batches selected (according to limit). Skipping.");
            return;
        }

        // Make all non-generated outputs unique
        for (int row = 0; row < dataBatches.size(); row++) {
            if (!dataBatches.get(row).isIncomplete()) {
                ImagePlusData imagePlusData = getFirstOutputSlot().getData(row, ImagePlusData.class, progressInfo);
                imagePlusData.makeUnique();
            }
        }

        // Create the form
        JIPipeDataSlot formsSlot = JIPipeDataSlot.createSingletonSlot(new MaskDrawerFormData(dataBatches, this), this);
        formsSlot.addTextAnnotationToAllData(new JIPipeTextAnnotation("Tab", "Draw mask"), true);

        // Form user input
        progressInfo.log("Waiting for user input ...");

        AtomicBoolean cancelled = new AtomicBoolean(true);
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        Object[] uiResult = new Object[1];
        Object lock = new Object();

        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                try {
                    JIPipeWorkbench workbench = JIPipeWorkbench.tryFindWorkbench(getParentGraph(), new JIPipeDummyWorkbench());
                    FormsDialog dialog = new FormsDialog(workbench, dataBatches, formsSlot, "Tab");
                    dialog.setTitle(getName());
                    dialog.setSize(1024, 768);
                    dialog.setLocationRelativeTo(workbench.getWindow());
                    dialog.revalidate();
                    dialog.setVisible(true);
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            cancelled.set(dialog.isCancelled());
                            uiResult[0] = dialog.getDataBatchForms();
                            windowOpened.set(false);
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
                    SwingUtilities.invokeLater(() -> dialog.setExtendedState(Frame.MAXIMIZED_BOTH));
                } catch (Throwable e) {
                    uiResult[0] = e;
                    windowOpened.set(false);
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            });

            try {
                while (windowOpened.get()) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (uiResult[0] instanceof Throwable) {
            throw new RuntimeException((Throwable) uiResult[0]);
        }

        if (cancelled.get()) {
            progressInfo.log("User input was cancelled!");
            throw new UserFriendlyRuntimeException("User input was cancelled!",
                    "User input was cancelled!",
                    "Node '" + getName() + "'",
                    "You had to provide input to allow the pipeline to continue. Instead, you cancelled the input.",
                    "");
        }
    }

    @Override
    protected void runGenerator(JIPipeMergingDataBatch dataBatch, JIPipeDataSlot inputSlot, JIPipeDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot referenceSlot = getInputSlot("Reference");
        ImagePlus referenceImage = dataBatch.getInputData(referenceSlot, ImagePlusData.class, progressInfo).get(0).getImage();
        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        int sizeC = referenceImage.getNChannels();
        int sizeZ = referenceImage.getNSlices();
        int sizeT = referenceImage.getNFrames();
        if (overwriteSizeC.isEnabled())
            sizeC = overwriteSizeC.getContent();
        if (overwriteSizeZ.isEnabled())
            sizeZ = overwriteSizeZ.getContent();
        if (overwriteSizeT.isEnabled())
            sizeT = overwriteSizeT.getContent();
        ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, 8);
        dataBatch.addOutputData(outputSlot, new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Overwrite number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public OptionalIntegerParameter getOverwriteSizeZ() {
        return overwriteSizeZ;
    }

    @JIPipeParameter("size-z")
    public void setOverwriteSizeZ(OptionalIntegerParameter overwriteSizeZ) {
        this.overwriteSizeZ = overwriteSizeZ;
    }

    @JIPipeDocumentation(name = "Overwrite number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public OptionalIntegerParameter getOverwriteSizeC() {
        return overwriteSizeC;
    }

    @JIPipeParameter("size-c")
    public void setOverwriteSizeC(OptionalIntegerParameter overwriteSizeC) {
        this.overwriteSizeC = overwriteSizeC;
    }

    @JIPipeDocumentation(name = "Overwrite number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public OptionalIntegerParameter getOverwriteSizeT() {
        return overwriteSizeT;
    }

    @JIPipeParameter("size-t")
    public void setOverwriteSizeT(OptionalIntegerParameter overwriteSizeT) {
        this.overwriteSizeT = overwriteSizeT;
    }
}
