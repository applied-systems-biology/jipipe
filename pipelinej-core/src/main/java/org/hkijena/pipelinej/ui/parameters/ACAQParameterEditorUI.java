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

package org.hkijena.pipelinej.ui.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.events.ParameterChangedEvent;
import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.ACAQWorkbenchPanel;
import org.scijava.Context;
import org.scijava.Contextual;

import java.util.Objects;

/**
 * A UI for a parameter type
 */
public abstract class ACAQParameterEditorUI extends ACAQWorkbenchPanel implements Contextual {
    private Context context;
    private ACAQParameterAccess parameterAccess;
    private int preventReload = 0;
    private boolean reloadScheduled = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public ACAQParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench);
        this.context = workbench.getContext();
        this.parameterAccess = parameterAccess;
        parameterAccess.getSource().getEventBus().register(this);
    }

    /**
     * Gets the object that holds the parameter
     *
     * @return object that holds the parameter
     */
    public ACAQParameterCollection getParameterHolder() {
        return parameterAccess.getSource();
    }

    /**
     * Gets or creates a parameter instance.
     * Safe to be called within reload()
     *
     * @param klass parameter class
     * @param <T>   parameter class
     * @return parameter instance. never null.
     */
    public <T> T getParameter(Class<T> klass) {
        T value = (T) getParameterAccess().get(getParameterAccess().getFieldClass());
        if (value == null) {
            ++preventReload;
            value = (T) ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(getParameterAccess().getFieldClass()).newInstance();
            getParameterAccess().set(value);
            --preventReload;
        }
        return value;
    }

    /**
     * Sets the parameter value.
     * Can prevent reload()
     *
     * @param parameter the value
     * @param reload    if enabled, a reload can happen.
     * @return if the parameter was set
     */
    public boolean setParameter(Object parameter, boolean reload) {
        if (!reload)
            ++preventReload;
        reloadScheduled = reload;
        boolean success = getParameterAccess().set(parameter);
        if (!reload)
            --preventReload;
        if (!success)
            reloadScheduled = true;
        if (reloadScheduled) {
            ++preventReload;
            reload();
            --preventReload;
        }
        return success;
    }

    /**
     * Gets the parameter accessor
     *
     * @return parameter accessor
     */
    public ACAQParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    /**
     * If true, the {@link ParameterPanel} will display a label with the parameter
     * name next to this UI.
     *
     * @return if label should be shown
     */
    public abstract boolean isUILabelEnabled();

    /**
     * Reloads the value from the stored parameter
     */
    public abstract void reload();

    /**
     * Listens for changes in parameters
     *
     * @param event Generated event
     */
    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if (Objects.equals(event.getKey(), parameterAccess.getKey())) {
            if (preventReload == 0) {
                reloadScheduled = false;
                ++preventReload;
                reload();
                --preventReload;
            }
        }
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
        this.context.inject(this);
    }
}
