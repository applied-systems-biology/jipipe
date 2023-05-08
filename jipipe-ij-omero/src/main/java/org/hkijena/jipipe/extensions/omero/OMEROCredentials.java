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

import omero.gateway.LoginCredentials;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

public class OMEROCredentials extends AbstractJIPipeParameterCollection {
    private String host = "localhost";

    private int port = 4064;
    private String userName = "";
    private PasswordParameter password = new PasswordParameter();

    public OMEROCredentials() {
    }

    public OMEROCredentials(OMEROCredentials other) {
        this.host = other.host;
        this.userName = other.userName;
        this.port = other.port;
        this.password = new PasswordParameter(other.password);
    }

    @JIPipeDocumentation(name = "Host", description = "The server host. For example <code>localhost</code>, <code>my.server.name</code>, or <code>wss://my.server.name</code>.")
    @JIPipeParameter(value = "host", uiOrder = 1)
    @StringParameterSettings(monospace = true)
    public String getHost() {
        return host;
    }

    @JIPipeParameter("host")
    public void setHost(String host) {
        this.host = host;
    }

    @JIPipeDocumentation(name = "Port", description = "The server port. Set to zero to use the global default port.")
    @JIPipeParameter(value = "port", uiOrder = 2)
    public int getPort() {
        return port;
    }

    @JIPipeParameter("port")
    public void setPort(int port) {
        this.port = port;
    }

    @JIPipeDocumentation(name = "User name", description = "The user name")
    @JIPipeParameter(value = "user-name", uiOrder = 3)
    @StringParameterSettings(monospace = true)
    public String getUserName() {
        return userName;
    }

    @JIPipeParameter("user-name")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JIPipeDocumentation(name = "Password", description = "The password. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
    @JIPipeParameter(value = "password", uiOrder = 4)
    public PasswordParameter getPassword() {
        return password;
    }

    @JIPipeParameter("password")
    public void setPassword(PasswordParameter password) {
        this.password = password;
    }

    /**
     * Converts the credentials into a SciJava OMERO location
     *
     * @return the location
     */
    public LoginCredentials getCredentials() {
        String host_ = StringUtils.orElse(host, OMEROSettings.getInstance().getDefaultHost());
        int port_ = port > 0 ? port : OMEROSettings.getInstance().getDefaultPort();
        String user_ = StringUtils.orElse(userName, OMEROSettings.getInstance().getDefaultUserName());
        String password_ = StringUtils.orElse(password.getPassword(), OMEROSettings.getInstance().getDefaultPassword().getPassword());

        return new LoginCredentials(user_, password_, host_, port_);
    }
}
