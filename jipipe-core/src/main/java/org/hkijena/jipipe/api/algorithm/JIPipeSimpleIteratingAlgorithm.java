/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.algorithm;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link JIPipeAlgorithm} that iterates through each data row.
 * This is a simplified version of {@link JIPipeIteratingAlgorithm} that assumes that there is only one or zero input slots.
 * An error is thrown if there are more than one input slots.
 */
public abstract class JIPipeSimpleIteratingAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm {

    private boolean parallelizationEnabled = true;

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeSimpleIteratingAlgorithm(JIPipeAlgorithmDeclaration declaration, JIPipeSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public JIPipeSimpleIteratingAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeSimpleIteratingAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
        this.parallelizationEnabled = other.parallelizationEnabled;
    }

    @Override
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        if (getEffectiveInputSlotCount() > 1)
            throw new UserFriendlyRuntimeException("Too many input slots for JIPipeSimpleIteratingAlgorithm!",
                    "Error in source code detected!",
                    "Algorithm '" + getName() + "'",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getDeclaration().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead.");
        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }

        if (getInputSlots().isEmpty()) {
            final int row = 0;
            JIPipeRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (row + 1) + " / " + 1);
            algorithmProgress.accept(slotProgress);
            JIPipeDataBatch dataInterface = new JIPipeDataBatch(this);
            dataInterface.addGlobalAnnotations(parameterAnnotations, true);
            runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
        } else {
            if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1) {
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (isCancelled.get())
                        return;
                    JIPipeRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (i + 1) + " / " + getFirstInputSlot().getRowCount());
                    algorithmProgress.accept(slotProgress);
                    JIPipeDataBatch dataInterface = new JIPipeDataBatch(this);
                    dataInterface.setData(getFirstInputSlot(), i);
                    dataInterface.addGlobalAnnotations(getFirstInputSlot().getAnnotations(i), true);
                    dataInterface.addGlobalAnnotations(parameterAnnotations, true);
                    runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
                }
            } else {
                List<Runnable> tasks = new ArrayList<>();
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    int rowIndex = i;
                    tasks.add(() -> {
                        if (isCancelled.get())
                            return;
                        JIPipeRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (rowIndex + 1) + " / " + getFirstInputSlot().getRowCount());
                        algorithmProgress.accept(slotProgress);
                        JIPipeDataBatch dataInterface = new JIPipeDataBatch(this);
                        dataInterface.setData(getFirstInputSlot(), rowIndex);
                        dataInterface.addGlobalAnnotations(getFirstInputSlot().getAnnotations(rowIndex), true);
                        dataInterface.addGlobalAnnotations(parameterAnnotations, true);
                        runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
                    });
                }
                algorithmProgress.accept(subProgress.resolve(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d", tasks.size(), getParallelizationBatchSize(), getThreadPool().getMaxThreads())));
                for (Future<Exception> batch : getThreadPool().scheduleBatches(tasks, getParallelizationBatchSize())) {
                    try {
                        Exception exception = batch.get();
                        if (exception != null)
                            throw new RuntimeException(exception);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (getInputSlots().size() > 1) {
            report.forCategory("Internals").reportIsInvalid(
                    "Error in source code detected!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getDeclaration().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead.",
                    this);
        }
    }

    /**
     * Runs code on one data row
     *
     * @param dataInterface     The data interface
     * @param subProgress       The current sub-progress this algorithm is scheduled in
     * @param algorithmProgress Consumer to publish a new sub-progress
     * @param isCancelled       Supplier that informs if the current task was canceled
     */
    protected abstract void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @JIPipeDocumentation(name = "Enable parallelization", description = "If enabled, the workload can be calculated across multiple threads to for speedup. " +
            "Please note that the actual usage of multiple threads depend on the runtime settings and the algorithm implementation. " +
            "We recommend to use the runtime parameters to control parallelization in most cases.")
    @JIPipeParameter(value = "jipipe:parallelization:enabled", visibility = JIPipeParameterVisibility.Visible)
    @Override
    public boolean isParallelizationEnabled() {
        return parallelizationEnabled;
    }

    @Override
    @JIPipeParameter("jipipe:parallelization:enabled")
    public void setParallelizationEnabled(boolean parallelizationEnabled) {
        this.parallelizationEnabled = parallelizationEnabled;
    }
}
