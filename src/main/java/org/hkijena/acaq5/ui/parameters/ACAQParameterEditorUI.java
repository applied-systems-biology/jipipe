package org.hkijena.acaq5.ui.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.scijava.Context;
import org.scijava.Contextual;

import javax.swing.*;
import java.util.Objects;

/**
 * A UI for a parameter type
 */
public abstract class ACAQParameterEditorUI extends JPanel implements Contextual {
    private Context context;
    private ACAQParameterAccess parameterAccess;
    private boolean preventReload = false;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public ACAQParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        this.context = context;
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
            preventReload = true;
            value = (T) ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(getParameterAccess().getFieldClass()).newInstance();
            getParameterAccess().set(value);
            preventReload = false;
        }
        return value;
    }

    /**
     * Sets the parameter value.
     * Can prevent reload()
     *
     * @param parameter the value
     * @param reload    if enabled, a reload can happen.
     */
    public void setParameter(Object parameter, boolean reload) {
        preventReload = !reload;
        getParameterAccess().set(parameter);
        preventReload = false;
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
            if (!preventReload)
                reload();
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
