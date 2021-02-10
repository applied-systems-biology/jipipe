package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.ui.FormsDialog;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Form processor (simple iterating)", description = "An algorithm that iterates through each row " +
        "of its 'Data' slot and shows a user interface during the runtime that allows users to modify annotations via form elements. " +
        "These forms are provided via the 'Forms' slot, where all contained form elements are shown in the user interface.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Forms")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = FormData.class, slotName = "Forms", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
public class SimpleIteratingFormProcessorAlgorithm extends JIPipeAlgorithm {
    public SimpleIteratingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimpleIteratingFormProcessorAlgorithm(SimpleIteratingFormProcessorAlgorithm other) {
        super(other);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dataSlot = getInputSlot("Data");
        JIPipeDataSlot formsSlot = getInputSlot("Forms");
        JIPipeDataSlot outputDataSlot = getOutputSlot("Data");

        if(isPassThrough() ||formsSlot.isEmpty()) {
            // Just copy without changes
            outputDataSlot.addData(dataSlot, progressInfo);
        }
        else if(!dataSlot.isEmpty()) {
            // Generate data batches and show the user interface
            List<JIPipeMergingDataBatch> dataBatchList = new ArrayList<>();
            List<FormData> forms = new ArrayList<>();
            for (int row = 0; row < dataSlot.getRowCount(); row++) {
                JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
                dataBatch.addData(dataSlot, row);
                dataBatch.addGlobalAnnotations(dataSlot.getAnnotations(row), JIPipeAnnotationMergeStrategy.Merge);
                dataBatchList.add(dataBatch);
            }
            for (int row = 0; row < formsSlot.getRowCount(); row++) {
                forms.add(formsSlot.getData(row, FormData.class, progressInfo));
            }

            progressInfo.log("Waiting for user input ...");

            AtomicBoolean cancelled = new AtomicBoolean(true);
            AtomicBoolean windowOpened = new AtomicBoolean(true);
            Object lock = new Object();

            synchronized (lock) {
                SwingUtilities.invokeLater(() -> {
                    JIPipeWorkbench workbench = JIPipeWorkbench.tryFindWorkbench(getGraph(), new JIPipeDummyWorkbench());
                    FormsDialog dialog = new FormsDialog(workbench, dataBatchList, forms);
                    dialog.setTitle(getName());
                    dialog.setSize(1024, 768);
                    dialog.setLocationRelativeTo(workbench.getWindow());
                    dialog.revalidate();
                    dialog.setVisible(true);
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            cancelled.set(dialog.isCancelled());
                            windowOpened.set(false);
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
                });

                try {
                    while (windowOpened.get()) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if(cancelled.get()) {
                progressInfo.log("User input was cancelled!");
                throw new UserFriendlyRuntimeException("User input was cancelled!",
                        "User input was cancelled!",
                        "Node '" + getName() + "'",
                        "You had to provide input to allow the pipeline to continue. Instead, you cancelled the input.",
                        "");
            }
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }


}
