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

package org.hkijena.jipipe.extensions.processes;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.List;

public class ProcessesExtensionSettings implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:processes";

    private final EventBus eventBus = new EventBus();

    private ProcessEnvironment.List presets = new ProcessEnvironment.List();

    public ProcessesExtensionSettings() {
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Presets", description = "List of presets stored for process environments.")
    @JIPipeParameter("presets")
    public ProcessEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(ProcessEnvironment.List presets) {
        this.presets = presets;
    }

    @Override
    public List<ExternalEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<ExternalEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (ExternalEnvironment preset : presets) {
            this.presets.add((ProcessEnvironment) preset);
        }
    }

    public static ProcessesExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ProcessesExtensionSettings.class);
    }
}
