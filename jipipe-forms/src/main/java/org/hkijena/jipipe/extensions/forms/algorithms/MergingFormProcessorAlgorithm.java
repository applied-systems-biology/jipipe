package org.hkijena.jipipe.extensions.forms.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.ui.FormsDialog;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Form processor (merging)", description = "An algorithm that iterates through groups of data in " +
        "its 'Data' slot and shows a user interface during the runtime that allows users to modify annotations via form elements. " +
        "Groups are based on the annotations. " +
        "These forms are provided via the 'Forms' slot, where all contained form elements are shown in the user interface." +
        "After the user input, the form data objects are stored in an output slot (one set of copies per data batch).")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Forms")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = FormData.class, slotName = "Forms", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = FormData.class, slotName = "Forms", autoCreate = true)
public class MergingFormProcessorAlgorithm extends JIPipeAlgorithm implements JIPipeDataBatchAlgorithm {

    private String tabAnnotation = "Tab";
    private JIPipeMergingAlgorithmDataBatchGenerationSettings dataBatchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings();

    public MergingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergingFormProcessorAlgorithm(MergingFormProcessorAlgorithm other) {
        super(other);
        this.tabAnnotation = other.tabAnnotation;
        this.dataBatchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings(other.dataBatchGenerationSettings);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dataSlot = getInputSlot("Data");
        JIPipeDataSlot formsSlot = getInputSlot("Forms");
        JIPipeDataSlot outputDataSlot = getOutputSlot("Data");
        JIPipeDataSlot formsOutputSlot = getOutputSlot("Forms");

        if (isPassThrough() || formsSlot.isEmpty()) {
            // Just copy without changes
            outputDataSlot.addData(dataSlot, progressInfo);
        } else if (!dataSlot.isEmpty()) {
            // Generate data batches and show the user interface
            List<JIPipeMergingDataBatch> dataBatchList = generateDataBatchesDryRun(getDataInputSlots(), progressInfo);

            if (dataBatchList.isEmpty()) {
                progressInfo.log("No data batches selected (according to limit). Skipping.");
                return;
            }

            progressInfo.log("Waiting for user input ...");

            AtomicBoolean cancelled = new AtomicBoolean(true);
            AtomicBoolean windowOpened = new AtomicBoolean(true);
            Object[] uiResult = new Object[1];
            Object lock = new Object();

            synchronized (lock) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        JIPipeWorkbench workbench = JIPipeWorkbench.tryFindWorkbench(getParentGraph(), new JIPipeDummyWorkbench());
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
            for (int i = 0; i < dataBatchForms.size(); i++) {
                JIPipeDataSlot forms = dataBatchForms.get(i);
                JIPipeMergingDataBatch dataBatch = dataBatchList.get(i);
                getFirstOutputSlot().addData(dataBatch.getVirtualInputData(dataSlot).get(0),
                        new ArrayList<>(dataBatch.getMergedTextAnnotations().values()),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        new ArrayList<>(dataBatch.getMergedDataAnnotations().values()),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting);

                // Copy user-modified forms
                for (int row = 0; row < forms.getRowCount(); row++) {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>(forms.getTextAnnotations(row));
                    annotations.addAll(dataBatch.getMergedTextAnnotations().values());
                    formsOutputSlot.addData(forms.getVirtualData(row),
                            annotations,
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            forms.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting);
                }
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

    @JIPipeDocumentation(name = "Data batch generation", description = "This algorithm has one input and will iterate through groups of rows and apply the workload. " +
            "Use following settings to control which data batches are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", hidden = true)
    public JIPipeMergingAlgorithmDataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:data-batch-generation", "jipipe:adaptive-parameters")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
    }

    @Override
    public List<JIPipeInputDataSlot> getDataInputSlots() {
        return Collections.singletonList(getInputSlot("Data"));
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setNode(this);
        builder.setSlots(slots);
        builder.setApplyMerging(true);
        builder.setAnnotationMergeStrategy(dataBatchGenerationSettings.getAnnotationMergeStrategy());
        builder.setDataAnnotationMergeStrategy(dataBatchGenerationSettings.getDataAnnotationMergeStrategy());
        builder.setReferenceColumns(dataBatchGenerationSettings.getColumnMatching(),
                dataBatchGenerationSettings.getCustomColumns());
        builder.setCustomAnnotationMatching(dataBatchGenerationSettings.getCustomAnnotationMatching());
        builder.setAnnotationMatchingMethod(dataBatchGenerationSettings.getAnnotationMatchingMethod());
        List<JIPipeMergingDataBatch> dataBatches = builder.build(progressInfo);
        dataBatches.sort(Comparator.naturalOrder());
        boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, dataBatches.size(), new ExpressionVariables())) : null;
        if (withLimit) {
            List<JIPipeMergingDataBatch> limitedBatches = new ArrayList<>();
            for (int i = 0; i < dataBatches.size(); i++) {
                if (allowedIndices.contains(i)) {
                    limitedBatches.add(dataBatches.get(i));
                }
            }
            dataBatches = limitedBatches;
        }
        if (dataBatchGenerationSettings.isSkipIncompleteDataSets()) {
            dataBatches.removeIf(JIPipeMergingDataBatch::isIncomplete);
        }
        return dataBatches;
    }
}
