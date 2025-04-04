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

package org.hkijena.jipipe.plugins.forms.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDummyWorkbench;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.forms.datatypes.FormData;
import org.hkijena.jipipe.plugins.forms.ui.FormsDialog;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SetJIPipeDocumentation(name = "Form processor (simple iterating)", description = "An algorithm that iterates through each row " +
        "of its 'Data' slot and shows a user interface during the runtime that allows users to modify annotations via form elements. " +
        "These forms are provided via the 'Forms' slot, where all contained form elements are shown in the user interface. " +
        "After the user input, the form data objects are stored in an output slot (one set of copies per iteration step).")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Forms")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeInputSlot(value = FormData.class, name = "Forms", create = true, role = JIPipeDataSlotRole.Parameters)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeOutputSlot(value = FormData.class, name = "Forms", create = true, role = JIPipeDataSlotRole.Parameters)
public class SimpleIteratingFormProcessorAlgorithm extends JIPipeAlgorithm implements JIPipeIterationStepAlgorithm {

    private String tabAnnotation = "Tab";
    private JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings();

    public SimpleIteratingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimpleIteratingFormProcessorAlgorithm(SimpleIteratingFormProcessorAlgorithm other) {
        super(other);
        this.tabAnnotation = other.tabAnnotation;
        this.iterationStepGenerationSettings = new JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot dataSlot = getInputSlot("Data");
        JIPipeDataSlot formsSlot = getInputSlot("Forms");
        JIPipeDataSlot outputDataSlot = getOutputSlot("Data");
        JIPipeDataSlot formsOutputSlot = getOutputSlot("Forms");

        if (isPassThrough() || formsSlot.isEmpty()) {
            // Just copy without changes
            outputDataSlot.addDataFromSlot(dataSlot, progressInfo);
        } else if (!dataSlot.isEmpty()) {
            // Generate iteration steps and show the user interface
            List<JIPipeMultiIterationStep> iterationStepList = new ArrayList<>();
            boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, dataSlot.getRowCount(), new JIPipeExpressionVariablesMap(this))) : null;
            for (int row = 0; row < dataSlot.getRowCount(); row++) {
                if (withLimit && !allowedIndices.contains(row))
                    continue;
                JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
                iterationStep.addInputData(dataSlot, row);
                iterationStep.addMergedTextAnnotations(dataSlot.getTextAnnotations(row), JIPipeTextAnnotationMergeMode.Merge);
                iterationStepList.add(iterationStep);
            }

            if (iterationStepList.isEmpty()) {
                progressInfo.log("No iteration steps selected (according to limit). Skipping.");
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
                        JIPipeDesktopProjectWorkbench workbench = JIPipeDesktopProjectWorkbench.tryFindProjectWorkbench(getParentGraph(), new JIPipeDummyWorkbench());
                        FormsDialog dialog = new FormsDialog(workbench, iterationStepList, formsSlot, tabAnnotation);
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
                                dialog.dispose();
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
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Operation cancelled by user",
                        "You clicked 'Cancel'"));
            }

            // Apply the form workloads
            List<JIPipeDataSlot> iterationStepForms = (List<JIPipeDataSlot>) uiResult[0];
            for (int i = 0; i < iterationStepForms.size(); i++) {
                JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Processing user input", i, iterationStepForms.size());
                JIPipeDataSlot forms = iterationStepForms.get(i);
                for (int row = 0; row < forms.getRowCount(); row++) {
                    FormData form = forms.getData(row, FormData.class, batchProgress);
                    batchProgress.resolveAndLog(form.toString(), row, forms.getRowCount());
                    form.writeData(iterationStepList.get(i));
                }
            }

            // Write the output
            for (int i = 0; i < iterationStepForms.size(); i++) {
                JIPipeDataSlot forms = iterationStepForms.get(i);
                JIPipeMultiIterationStep iterationStep = iterationStepList.get(i);
                getFirstOutputSlot().addData(iterationStep.getInputDataStore(dataSlot).get(0),
                        new ArrayList<>(iterationStep.getMergedTextAnnotations().values()),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        new ArrayList<>(iterationStep.getMergedDataAnnotations().values()),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        iterationStep.createNewContext(),
                        progressInfo);

                // Copy user-modified forms
                for (int row = 0; row < forms.getRowCount(); row++) {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>(forms.getTextAnnotations(row));
                    annotations.addAll(iterationStep.getMergedTextAnnotations().values());
                    formsOutputSlot.addData(forms.getDataItemStore(row),
                            annotations,
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            forms.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            forms.getDataContext(row).branch(this),
                            progressInfo);
                }
            }
        }
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Form tab annotation", description = "The annotation that is used to group form elements into tabs.")
    @JIPipeParameter("tab-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getTabAnnotation() {
        return tabAnnotation;
    }

    @JIPipeParameter("tab-annotation")
    public void setTabAnnotation(String tabAnnotation) {
        this.tabAnnotation = tabAnnotation;
    }

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm has one input and will iterate through each row of its input and apply the workload. " +
            "Use following settings to control which iteration steps are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", hidden = true)
    public JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
        return iterationStepGenerationSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:data-batch-generation", "jipipe:adaptive-parameters")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    @Override
    public List<JIPipeInputDataSlot> getDataInputSlots() {
        return Collections.singletonList(getInputSlot("Data"));
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        List<JIPipeMultiIterationStep> batches = new ArrayList<>();
        JIPipeDataSlot slot = slots.stream().filter(s -> "Data".equals(s.getName())).findFirst().get();
        boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, slot.getRowCount(), new JIPipeExpressionVariablesMap(this))) : null;
        for (int i = 0; i < slot.getRowCount(); i++) {
            if (withLimit && !allowedIndices.contains(i))
                continue;
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
            iterationStep.addInputData(slot, i);
            iterationStep.addMergedTextAnnotations(slot.getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
            batches.add(iterationStep);
        }

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(batches);

        return result;
    }
}
