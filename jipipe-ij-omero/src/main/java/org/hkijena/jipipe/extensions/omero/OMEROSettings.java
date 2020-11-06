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

package org.hkijena.jipipe.extensions.omero;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.PasswordParameter;

public class OMEROSettings implements JIPipeParameterCollection {
    public static final String ID = "org.hkijena.jipipe:omero";
    private final EventBus eventBus = new EventBus();

    private String defaultServer = "";
    private String defaultUserName = "";
    private PasswordParameter defaultPassword = new PasswordParameter();
    private String email = "";

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Default server URL", description = "The server URL used as default if none is provided. It has the following format [Host]:[Port] or [Host]. If only the host is provided, the port 4064 is assumed.")
    @JIPipeParameter("default-server")
    public String getDefaultServer() {
        return defaultServer;
    }

    @JIPipeParameter("default-server")
    public void setDefaultServer(String defaultServer) {
        this.defaultServer = defaultServer;
    }

    @JIPipeDocumentation(name = "Default user name", description = "The user name used as default if none is provided.")
    @JIPipeParameter("default-user-name")
    public String getDefaultUserName() {
        return defaultUserName;
    }

    @JIPipeParameter("default-user-name")
    public void setDefaultUserName(String defaultUserName) {
        this.defaultUserName = defaultUserName;
    }

    @JIPipeDocumentation(name = "Default password", description = "The password used as default if none is provided. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
    @JIPipeParameter("default-password")
    public PasswordParameter getDefaultPassword() {
        return defaultPassword;
    }

    @JIPipeParameter("default-password")
    public void setDefaultPassword(PasswordParameter defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    @JIPipeDocumentation(name = "E-Mail", description = "E-Mail that is passed to the OMERO importer.")
    @JIPipeParameter("email")
    public String getEmail() {
        return email;
    }

    @JIPipeParameter("email")
    public void setEmail(String email) {
        this.email = email;
    }

    public static OMEROSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, OMEROSettings.class);
    }
}
