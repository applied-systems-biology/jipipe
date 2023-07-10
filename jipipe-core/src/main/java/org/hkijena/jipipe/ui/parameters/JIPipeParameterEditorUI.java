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
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.Disposable;

import java.util.Objects;

/**
 * A UI for a parameter type
 */
public abstract class JIPipeParameterEditorUI extends JIPipeWorkbenchPanel implements Contextual, Disposable, JIPipeParameterCollection.ParameterChangedEventListener {
    public static final int CONTROL_STYLE_PANEL = 1;
    public static final int CONTROL_STYLE_LIST = 2;
    public static final int CONTROL_STYLE_CHECKBOX = 4;

    private JIPipeParameterAccess parameterAccess;
    private Context context;
    private int preventReload = 0;
    private boolean reloadScheduled = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public JIPipeParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench);
        this.context = workbench.getContext();
        this.parameterAccess = parameterAccess;
        parameterAccess.getSource().getParameterChangedEventEmitter().subscribeWeak(this);
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
     * Returns the "style" of the control.
     * This is only utilized for the automated ordering within {@link ParameterPanel}
     * Controls with the same style are grouped together to ensure a consistent visual style (reduce clutter)
     * Please note that the grouping enforced by isUILabelEnabled() has precedence
     *
     * @return the UI control style for {@link ParameterPanel} (sorting only)
     */
    public int getUIControlStyleType() {
        return CONTROL_STYLE_PANEL;
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
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (parameterAccess == null) {
            return;
        }
        if (!isDisplayable()) {
            try {
                parameterAccess.getSource().getParameterChangedEventEmitter().unsubscribe(this);
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
    public void dispose() {
        if (parameterAccess != null) {
            try {
                parameterAccess.getSource().getParameterChangedEventEmitter().unsubscribe(this);
            } catch (IllegalArgumentException e) {
            }
        }
        UIUtils.removeAllWithDispose(this);
        parameterAccess = null;
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
