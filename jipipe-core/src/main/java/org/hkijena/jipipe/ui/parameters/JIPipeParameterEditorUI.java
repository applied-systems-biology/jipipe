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

package org.hkijena.jipipe.ui.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.scijava.Context;
import org.scijava.Contextual;

import java.util.Objects;

/**
 * A UI for a parameter type
 */
public abstract class JIPipeParameterEditorUI extends JIPipeWorkbenchPanel implements Contextual {
    private Context context;
    private JIPipeParameterAccess parameterAccess;
    private int preventReload = 0;
    private boolean reloadScheduled = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
    public JIPipeParameterCollection getParameterHolder() {
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
            value = (T) JIPipe.getParameterTypes().getInfoByFieldClass(getParameterAccess().getFieldClass()).newInstance();
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
    public JIPipeParameterAccess getParameterAccess() {
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
     * If true, the {@link ParameterPanel} will render an "important" label if the parameter is marked as important.
     *
     * @return if an automatically generated "important" label should be displayed
     */
    public boolean isUIImportantLabelEnabled() {
        return true;
    }

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
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (!isDisplayable()) {
            try {
                parameterAccess.getSource().getEventBus().unregister(this);
            } catch (Exception e) {
            }
        }
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
