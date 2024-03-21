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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.forms;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
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

@SetJIPipeDocumentation(name = "Draw/modify mask", description = "Allows users to draw or modify a mask that is drawn over a reference image." +
        " You can supply existing masks via the 'Masks' input. If a data batch has no existing mask, a new one is generated according to the " +
        "node parameters.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Forms")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", create = true)
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
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        // Generate the output first
        super.runParameterSet(runContext, progressInfo, parameterAnnotations);

        if (isPassThrough())
            return;

        // Get back the data batches
        List<JIPipeMultiIterationStep> iterationSteps;

        // No input slots -> Nothing to do
        if (getDataInputSlotCount() == 0) {
            return;
        } else if (getDataInputSlotCount() == 1) {
            iterationSteps = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled())
                    break;
                JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
                iterationStep.setInputData(getFirstInputSlot(), row);
                iterationStep.addMergedTextAnnotations(parameterAnnotations, getDataBatchGenerationSettings().getAnnotationMergeStrategy());
                iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(row), getDataBatchGenerationSettings().getAnnotationMergeStrategy());
                iterationSteps.add(iterationStep);
            }
        } else {
            iterationSteps = generateDataBatchesGenerationResult(getNonParameterInputSlots(), progressInfo).getDataBatches();
        }

        runForm(iterationSteps, progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    private void runForm(List<JIPipeMultiIterationStep> iterationSteps, JIPipeProgressInfo progressInfo) {
        if (iterationSteps.isEmpty()) {
            progressInfo.log("No data batches selected (according to limit). Skipping.");
            return;
        }

        // Make all non-generated outputs unique
        for (int row = 0; row < iterationSteps.size(); row++) {
            if (!iterationSteps.get(row).isIncomplete()) {
                ImagePlusData imagePlusData = getFirstOutputSlot().getData(row, ImagePlusData.class, progressInfo);
                imagePlusData.makeUnique();
            }
        }

        // Create the form
        JIPipeDataSlot formsSlot = JIPipeDataSlot.createSingletonSlot(new MaskDrawerFormData(iterationSteps, this), this);
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
                    FormsDialog dialog = new FormsDialog(workbench, iterationSteps, formsSlot, "Tab");
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
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Operation cancelled by user",
                    "You clicked 'Cancel'"));
        }
    }

    @Override
    protected void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot referenceSlot = getInputSlot("Reference");
        ImagePlus referenceImage = iterationStep.getInputData(referenceSlot, ImagePlusData.class, progressInfo).get(0).getImage();
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
        iterationStep.addOutputData(outputSlot, new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Overwrite number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public OptionalIntegerParameter getOverwriteSizeZ() {
        return overwriteSizeZ;
    }

    @JIPipeParameter("size-z")
    public void setOverwriteSizeZ(OptionalIntegerParameter overwriteSizeZ) {
        this.overwriteSizeZ = overwriteSizeZ;
    }

    @SetJIPipeDocumentation(name = "Overwrite number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public OptionalIntegerParameter getOverwriteSizeC() {
        return overwriteSizeC;
    }

    @JIPipeParameter("size-c")
    public void setOverwriteSizeC(OptionalIntegerParameter overwriteSizeC) {
        this.overwriteSizeC = overwriteSizeC;
    }

    @SetJIPipeDocumentation(name = "Overwrite number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public OptionalIntegerParameter getOverwriteSizeT() {
        return overwriteSizeT;
    }

    @JIPipeParameter("size-t")
    public void setOverwriteSizeT(OptionalIntegerParameter overwriteSizeT) {
        this.overwriteSizeT = overwriteSizeT;
    }
}
