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

package org.hkijena.jipipe.desktop.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * A UI for a parameter type
 */
public abstract class JIPipeDesktopParameterEditorUI extends JIPipeDesktopWorkbenchPanel implements Contextual, Disposable, JIPipeParameterCollection.ParameterChangedEventListener {
    public static final int CONTROL_STYLE_PANEL = 1;
    public static final int CONTROL_STYLE_LIST = 2;
    public static final int CONTROL_STYLE_CHECKBOX = 4;

    private final JIPipeParameterTree parameterTree;
    private final Object contextParent;
    private JIPipeParameterAccess parameterAccess;
    private Context context;
    private int preventReload = 0;
    private boolean reloadScheduled = false;


    public JIPipeDesktopParameterEditorUI(InitializationParameters initializationParameters) {
        super(initializationParameters.workbench);
        this.context = initializationParameters.workbench.getContext();
        this.parameterTree = initializationParameters.parameterTree;
        this.contextParent = initializationParameters.parent;
        this.parameterAccess = initializationParameters.parameterAccess;
        parameterAccess.getSource().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    /**
     * Gets the object that holds the parameter
     *
     * @return object that holds the parameter
     */
    public JIPipeParameterCollection getParameterCollection() {
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
     * Finds the graph canvas if one is available
     *
     * @return the canvas or null
     */
    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        Container ancestor = SwingUtilities.getAncestorOfClass(JIPipeDesktopGraphCanvasUI.class, this);
        if (ancestor instanceof JIPipeDesktopGraphCanvasUI) {
            return (JIPipeDesktopGraphCanvasUI) ancestor;
        }
        ancestor = SwingUtilities.getAncestorOfClass(AbstractJIPipeDesktopGraphEditorUI.class, this);
        if (ancestor instanceof AbstractJIPipeDesktopGraphEditorUI) {
            return ((AbstractJIPipeDesktopGraphEditorUI) ancestor).getCanvasUI();
        }
        return null;
    }

    public JIPipeParameterTree getParameterTree() {
        return parameterTree;
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
     * Returns the field class of the parameter
     *
     * @return the field class
     */
    public Class<?> getParameterFieldClass() {
        return getParameterAccess().getFieldClass();
    }

    /**
     * Returns the info for the current parameter type
     *
     * @return the parameter type info
     */
    public JIPipeParameterTypeInfo getParameterTypeInfo() {
        return JIPipe.getParameterTypes().getInfoByFieldClass(getParameterAccess().getFieldClass());
    }

    /**
     * If true, the {@link JIPipeDesktopParameterFormPanel} will display a label with the parameter
     * name next to this UI.
     *
     * @return if label should be shown
     */
    public abstract boolean isUILabelEnabled();

    /**
     * If true, the {@link JIPipeDesktopParameterFormPanel} will render an "important" label if the parameter is marked as important.
     *
     * @return if an automatically generated "important" label should be displayed
     */
    public boolean isUIImportantLabelEnabled() {
        return true;
    }

    /**
     * Returns the "style" of the control.
     * This is only utilized for the automated ordering within {@link JIPipeDesktopParameterFormPanel}
     * Controls with the same style are grouped together to ensure a consistent visual style (reduce clutter)
     * Please note that the grouping enforced by isUILabelEnabled() has precedence
     *
     * @return the UI control style for {@link JIPipeDesktopParameterFormPanel} (sorting only)
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

    /**
     * Gets the parent object that is added during the creation of the UI
     *
     * @return the parent object (can be null)
     */
    public Object getContextParent() {
        return contextParent;
    }

    public static class InitializationParameters {
        private JIPipeDesktopWorkbench workbench;
        private JIPipeParameterTree parameterTree;
        private Object parent;
        private JIPipeParameterAccess parameterAccess;

        public InitializationParameters() {
        }

        public InitializationParameters(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, Object parent, JIPipeParameterAccess parameterAccess) {
            this.workbench = workbench;
            this.parameterTree = parameterTree;
            this.parent = parent;
            this.parameterAccess = parameterAccess;
        }

        public InitializationParameters(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            this.workbench = workbench;
            this.parameterTree = parameterTree;
            this.parameterAccess = parameterAccess;
        }

        public JIPipeDesktopWorkbench getWorkbench() {
            return workbench;
        }

        public void setWorkbench(JIPipeDesktopWorkbench workbench) {
            this.workbench = workbench;
        }

        public JIPipeParameterTree getParameterTree() {
            return parameterTree;
        }

        public void setParameterTree(JIPipeParameterTree parameterTree) {
            this.parameterTree = parameterTree;
        }

        public Object getParent() {
            return parent;
        }

        public void setParent(Object parent) {
            this.parent = parent;
        }

        public JIPipeParameterAccess getParameterAccess() {
            return parameterAccess;
        }

        public void setParameterAccess(JIPipeParameterAccess parameterAccess) {
            this.parameterAccess = parameterAccess;
        }
    }
}
