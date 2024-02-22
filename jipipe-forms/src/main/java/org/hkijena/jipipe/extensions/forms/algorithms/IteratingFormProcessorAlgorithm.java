package org.hkijena.jipipe.extensions.forms.algorithms;

import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Form processor (iterating)", description = "An algorithm that iterates through groups of data in " +
        "its 'Data' slot and shows a user interface during the runtime that allows users to modify annotations via form elements. " +
        "Groups are based on the annotations. " +
        "These forms are provided via the 'Forms' slot, where all contained form elements are shown in the user interface." +
        "After the user input, the form data objects are stored in an output slot (one set of copies per data batch).")
@DefineJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Forms")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Forms", role = JIPipeDataSlotRole.Parameters)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Data")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Forms", role = JIPipeDataSlotRole.Parameters)
public class IteratingFormProcessorAlgorithm extends JIPipeAlgorithm implements JIPipeIterationStepAlgorithm {

    public static final String SLOT_FORMS = "Forms";

    private String tabAnnotation = "Tab";
    private boolean restoreAnnotations = true;
    private JIPipeMergingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings();

    public IteratingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
        updateFormsSlot();
    }

    public IteratingFormProcessorAlgorithm(IteratingFormProcessorAlgorithm other) {
        super(other);
        this.tabAnnotation = other.tabAnnotation;
        this.restoreAnnotations = other.restoreAnnotations;
        this.iterationStepGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        updateFormsSlot();
    }

    /**
     * Extracts original annotations into the annotation list
     *
     * @param iterationStep          the data batch
     * @param preFormAnnotations annotations before the form was applied
     * @param inputSlot          the input slot
     * @param row                row of the data in the input slot
     * @param annotations        target list
     */
    public static void extractRestoredAnnotations(JIPipeMultiIterationStep iterationStep, Map<String, JIPipeTextAnnotation> preFormAnnotations, JIPipeDataSlot inputSlot, int row, List<JIPipeTextAnnotation> annotations) {
        Map<String, JIPipeTextAnnotation> originalAnnotationMap = inputSlot.getTextAnnotationMap(row);
        for (JIPipeTextAnnotation formAnnotation : iterationStep.getMergedTextAnnotations().values()) {
            JIPipeTextAnnotation preFormAnnotation = preFormAnnotations.getOrDefault(formAnnotation.getName(), null);
            if (preFormAnnotation == null) {
                // Added by form
                annotations.add(formAnnotation);
            } else {
                // Was it changed by the form?
                JIPipeTextAnnotation originalAnnotation = originalAnnotationMap.getOrDefault(formAnnotation.getName(), null);
                if (originalAnnotation == null) {
                    annotations.add(formAnnotation);
                } else {
                    if (Objects.equals(preFormAnnotation.getValue(), formAnnotation.getValue())) {
                        annotations.add(originalAnnotation);
                    } else {
                        annotations.add(formAnnotation);
                    }
                }
            }
        }
    }

    private void updateFormsSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        JIPipeDataSlotInfo existing = slotConfiguration.getInputSlots().getOrDefault(SLOT_FORMS, null);
        if (existing != null && existing.getDataClass() != FormData.class) {
            slotConfiguration.removeInputSlot(SLOT_FORMS, false);
            existing = null;
        }
        if (existing == null) {
            JIPipeDataSlotInfo info = new JIPipeDataSlotInfo(FormData.class, JIPipeSlotType.Input);
            info.setRole(JIPipeDataSlotRole.Parameters);
            info.setUserModifiable(false);
            slotConfiguration.addSlot(SLOT_FORMS,
                    info,
                    false);
        }
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot formsSlot = getInputSlot("Forms");
        JIPipeDataSlot formsOutputSlot = getOutputSlot("Forms");

        if (isPassThrough() || formsSlot.isEmpty()) {
            for (String name : getInputSlotMap().keySet()) {
                JIPipeDataSlot inputSlot = getInputSlot(name);
                JIPipeDataSlot outputSlot = getOutputSlot(name);
                outputSlot.addDataFromSlot(inputSlot, progressInfo);
            }
        } else {
            // Generate data batches and show the user interface
            List<JIPipeMultiIterationStep> iterationStepList = generateDataBatchesGenerationResult(getDataInputSlots(), progressInfo).getDataBatches();

            if (iterationStepList.isEmpty()) {
                progressInfo.log("No data batches. Skipping.");
                return;
            }

            // Keep current merged annotations
            List<Map<String, JIPipeTextAnnotation>> iterationStepListPreFormAnnotations = new ArrayList<>();
            for (JIPipeMultiIterationStep iterationStep : iterationStepList) {
                iterationStepListPreFormAnnotations.add(new HashMap<>(iterationStep.getMergedTextAnnotations()));
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
                Map<String, JIPipeTextAnnotation> preFormAnnotations = iterationStepListPreFormAnnotations.get(i);
                for (String name : getInputSlotMap().keySet()) {
                    if (!name.equals("Forms")) {
                        JIPipeDataSlot inputSlot = getInputSlot(name);
                        JIPipeDataSlot outputSlot = getOutputSlot(name);
                        for (int row : iterationStep.getInputSlotRows().get(inputSlot)) {
                            List<JIPipeTextAnnotation> annotations;
                            if (restoreAnnotations) {
                                annotations = new ArrayList<>();
                                extractRestoredAnnotations(iterationStep, preFormAnnotations, inputSlot, row, annotations);
                            } else {
                                annotations = new ArrayList<>(iterationStep.getMergedTextAnnotations().values());
                            }
                            outputSlot.addData(inputSlot.getDataItemStore(row),
                                    annotations,
                                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                                    inputSlot.getDataAnnotations(row),
                                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                                    inputSlot.getDataContext(row).branch(this),
                                    progressInfo);
                        }
                    }
                }
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

    @SetJIPipeDocumentation(name = "Restore original annotations", description = "If enabled, original annotations that were not changed by the form processor will be restored in the output data. " +
            "Otherwise, merged annotation values from the data batch are used.")
    @JIPipeParameter("restore-annotations")
    public boolean isRestoreAnnotations() {
        return restoreAnnotations;
    }

    @JIPipeParameter("restore-annotations")
    public void setRestoreAnnotations(boolean restoreAnnotations) {
        this.restoreAnnotations = restoreAnnotations;
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

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm will iterate through multiple inputs at once and apply the workload. " +
            "Use following settings to control which data batches are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", hidden = true)
    public JIPipeMergingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
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
        return getInputSlots().stream().filter(slot -> !slot.getName().equals("Forms")).collect(Collectors.toList());
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMultiIterationStepGenerator builder = new JIPipeMultiIterationStepGenerator();
        builder.setNode(this);
        builder.setSlots(slots);
        builder.setApplyMerging(true);
        builder.setAnnotationMergeStrategy(iterationStepGenerationSettings.getAnnotationMergeStrategy());
        builder.setDataAnnotationMergeStrategy(iterationStepGenerationSettings.getDataAnnotationMergeStrategy());
        builder.setReferenceColumns(iterationStepGenerationSettings.getColumnMatching(),
                iterationStepGenerationSettings.getCustomColumns());
        builder.setCustomAnnotationMatching(iterationStepGenerationSettings.getCustomAnnotationMatching());
        builder.setAnnotationMatchingMethod(iterationStepGenerationSettings.getAnnotationMatchingMethod());
        builder.setForceFlowGraphSolver(iterationStepGenerationSettings.isForceFlowGraphSolver());
        List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
        iterationSteps.sort(Comparator.naturalOrder());
        boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, iterationSteps.size(), new JIPipeExpressionVariablesMap())) : null;
        if (withLimit) {
            progressInfo.log("[INFO] Applying limit to all data batches. Allowed indices are " + Ints.join(", ", allowedIndices.toArray()));
            List<JIPipeMultiIterationStep> limitedBatches = new ArrayList<>();
            for (int i = 0; i < iterationSteps.size(); i++) {
                if (allowedIndices.contains(i)) {
                    limitedBatches.add(iterationSteps.get(i));
                }
            }
            iterationSteps = limitedBatches;
        }
        List<JIPipeMultiIterationStep> incomplete = new ArrayList<>();
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            if (iterationStep.isIncomplete()) {
                incomplete.add(iterationStep);
                progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + iterationStep);
            }
        }
        if (!incomplete.isEmpty() && iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
            progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
            iterationSteps.removeAll(incomplete);
        }

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(iterationSteps);
        result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

        return result;
    }
}
