package org.hkijena.jipipe.extensions.cellpose;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.environments.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.environments.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

public class CellPoseSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:cellpose";

    private final EventBus eventBus = new EventBus();

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();

    public CellPoseSettings() {
        overridePythonEnvironment.setEnabled(true);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static CellPoseSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, CellPoseSettings.class);
    }

    @JIPipeDocumentation(name = "Cellpose Python environment", description = "If enabled, a separate Python environment is used for CellPose. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that CellPose is installed. " +
            "You can also install CellPose via the Select/Install button (CPU and GPU supported).")
    @JIPipeParameter("python-environment")
    public OptionalPythonEnvironment getOverridePythonEnvironment() {
        return overridePythonEnvironment;
    }

    @JIPipeParameter("python-environment")
    public void setOverridePythonEnvironment(OptionalPythonEnvironment overridePythonEnvironment) {
        this.overridePythonEnvironment = overridePythonEnvironment;
    }

    public PythonEnvironment getPythonEnvironment() {
        if(overridePythonEnvironment.isEnabled()) {
            return overridePythonEnvironment.getContent();
        }
        else {
            return PythonExtensionSettings.getInstance().getPythonEnvironment();
        }
    }
}
