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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

public class OMEROSettings extends AbstractJIPipeParameterCollection {
    public static final String ID = "org.hkijena.jipipe:omero";

    private String defaultHost = "localhost";

    private int defaultPort = 4064;
    private String defaultUserName = "";
    private PasswordParameter defaultPassword = new PasswordParameter();
    private String email = "";

    public static OMEROSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, OMEROSettings.class);
    }

    @JIPipeDocumentation(name = "Default server host", description = "The server host used as default if none is provided. For example <code>localhost</code>, <code>my.server.name</code>, or <code>wss://my.server.name</code>.")
    @JIPipeParameter("default-host")
    @StringParameterSettings(monospace = true)
    public String getDefaultHost() {
        return defaultHost;
    }

    @JIPipeParameter("default-host")
    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    @JIPipeDocumentation(name = "Default server port", description = "The server port used as default if none is provided.")
    @JIPipeParameter("default-port")
    public int getDefaultPort() {
        return defaultPort;
    }

    @JIPipeParameter("default-port")
    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    @JIPipeDocumentation(name = "Default user name", description = "The user name used as default if none is provided.")
    @JIPipeParameter("default-user-name")
    @StringParameterSettings(monospace = true)
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
}
