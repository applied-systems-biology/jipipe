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

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.RegisterJIPipeParameterCollectionContextAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;

import java.io.IOException;

@JsonDeserialize(using = JIPipeGraphNodeEnvironmentOverridesParameter.Deserializer.class)
public class JIPipeGraphNodeEnvironmentOverridesParameter extends JIPipeDynamicParameterCollection {

    public JIPipeGraphNodeEnvironmentOverridesParameter() {
    }

    public JIPipeGraphNodeEnvironmentOverridesParameter(JIPipeGraphNodeEnvironmentOverridesParameter other) {
        super(other);
    }

    @SetJIPipeDocumentation(name = "Project", description = "Opens the connected services settings for the current project")
    @RegisterJIPipeParameterCollectionContextAction(icon = "actions/open-in-new-window.png", highlighted = true)
    public void openProjectEnvironmentConfig(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).openProjectSettings("/General/Connected services");
        }
    }

    @SetJIPipeDocumentation(name = "Global", description = "Opens the connected services settings for the whole application")
    @RegisterJIPipeParameterCollectionContextAction(icon = "actions/open-in-new-window.png")
    public void openApplicationEnvironmentConfig(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).openApplicationSettings("/Connected services/Defaults");
        }
    }

    public static class Deserializer extends JsonDeserializer<JIPipeGraphNodeEnvironmentOverridesParameter> {
        @Override
        public JIPipeGraphNodeEnvironmentOverridesParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JIPipeGraphNodeEnvironmentOverridesParameter result = new JIPipeGraphNodeEnvironmentOverridesParameter();
            result.fromJson(p.readValueAsTree());
            return result;
        }
    }
}
