package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;
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

    /**
     * Creates new instance
     * @param context SciJava context
     * @param parameterAccess Parameter
     */
    public ACAQParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        this.context = context;
        this.parameterAccess = parameterAccess;
        parameterAccess.getParameterHolder().getEventBus().register(this);
    }

    /**
     * Gets the object that holds the parameter
     *
     * @return object that holds the parameter
     */
    public ACAQParameterHolder getParameterHolder() {
        return parameterAccess.getParameterHolder();
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
     * If true, the {@link org.hkijena.acaq5.ui.components.ACAQParameterAccessUI} will display a label with the parameter
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
