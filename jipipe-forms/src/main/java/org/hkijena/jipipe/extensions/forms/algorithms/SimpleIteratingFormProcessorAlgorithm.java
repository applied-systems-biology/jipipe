package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.ui.FormsDialog;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;

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

    private String tabAnnotation = "Tab";

    public SimpleIteratingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimpleIteratingFormProcessorAlgorithm(SimpleIteratingFormProcessorAlgorithm other) {
        super(other);
        this.tabAnnotation = other.tabAnnotation;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dataSlot = getInputSlot("Data");
        JIPipeDataSlot formsSlot = getInputSlot("Forms");
        JIPipeDataSlot outputDataSlot = getOutputSlot("Data");

        if (isPassThrough() || formsSlot.isEmpty()) {
            // Just copy without changes
            outputDataSlot.addData(dataSlot, progressInfo);
        } else if (!dataSlot.isEmpty()) {
            // Generate data batches and show the user interface
            List<JIPipeMergingDataBatch> dataBatchList = new ArrayList<>();
            for (int row = 0; row < dataSlot.getRowCount(); row++) {
                JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
                dataBatch.addData(dataSlot, row);
                dataBatch.addGlobalAnnotations(dataSlot.getAnnotations(row), JIPipeAnnotationMergeStrategy.Merge);
                dataBatchList.add(dataBatch);
            }

            progressInfo.log("Waiting for user input ...");

            AtomicBoolean cancelled = new AtomicBoolean(true);
            AtomicBoolean windowOpened = new AtomicBoolean(true);
            Object[] uiResult = new Object[1];
            Object lock = new Object();

            synchronized (lock) {
                SwingUtilities.invokeLater(() -> {
                    JIPipeWorkbench workbench = JIPipeWorkbench.tryFindWorkbench(getGraph(), new JIPipeDummyWorkbench());
                    FormsDialog dialog = new FormsDialog(workbench, dataBatchList, formsSlot, tabAnnotation);
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
                });

                try {
                    while (windowOpened.get()) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (cancelled.get()) {
                progressInfo.log("User input was cancelled!");
                throw new UserFriendlyRuntimeException("User input was cancelled!",
                        "User input was cancelled!",
                        "Node '" + getName() + "'",
                        "You had to provide input to allow the pipeline to continue. Instead, you cancelled the input.",
                        "");
            }

            // Apply the form workloads
            List<JIPipeDataSlot> dataBatchForms = (List<JIPipeDataSlot>) uiResult[0];
            for (int i = 0; i < dataBatchForms.size(); i++) {
                JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Processing user input", i, dataBatchForms.size());
                JIPipeDataSlot forms = dataBatchForms.get(i);
                for (int row = 0; row < forms.getRowCount(); row++) {
                    FormData form = forms.getData(row, FormData.class, batchProgress);
                    batchProgress.resolveAndLog(form.toString(), row, forms.getRowCount());
                    form.writeData(dataBatchList.get(i));
                }
            }

            // Write the output
            for (JIPipeMergingDataBatch dataBatch : dataBatchList) {
                getFirstOutputSlot().addData(dataBatch.getVirtualInputData(dataSlot).get(0),
                        new ArrayList<>(dataBatch.getAnnotations().values()),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting);
            }
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @JIPipeDocumentation(name = "Form tab annotation", description = "The annotation that is used to group form elements into tabs.")
    @JIPipeParameter("tab-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getTabAnnotation() {
        return tabAnnotation;
    }

    @JIPipeParameter("tab-annotation")
    public void setTabAnnotation(String tabAnnotation) {
        this.tabAnnotation = tabAnnotation;
    }
}
