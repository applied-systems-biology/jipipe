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
import net.imagej.omero.OMEROLocation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.PasswordParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.net.URISyntaxException;

public class OMEROCredentials implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private String server = "";
    private String userName = "";
    private PasswordParameter password = new PasswordParameter();

    public OMEROCredentials() {
    }

    public OMEROCredentials(OMEROCredentials other) {
        this.server = other.server;
        this.userName = other.userName;
        this.password = new PasswordParameter(other.password);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Server", description = "The server URL. It has the following format [Host]:[Port] or [Host]. If only the host is provided, the port 4064 is assumed.")
    @JIPipeParameter(value = "server", uiOrder = 1)
    public String getServer() {
        return server;
    }

    @JIPipeParameter("server")
    public void setServer(String server) {
        this.server = server;
    }

    @JIPipeDocumentation(name = "User name", description = "The user name")
    @JIPipeParameter(value = "user-name", uiOrder = 2)
    public String getUserName() {
        return userName;
    }

    @JIPipeParameter("user-name")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JIPipeDocumentation(name = "Password", description = "The password. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
    @JIPipeParameter(value = "password", uiOrder = 3)
    public PasswordParameter getPassword() {
        return password;
    }

    @JIPipeParameter("password")
    public void setPassword(PasswordParameter password) {
        this.password = password;
    }

    /**
     * Converts the credentials into a SciJava OMERO location
     * @return the location
     */
    public OMEROLocation getLocation() {
        String server_ = StringUtils.orElse(server, OMEROSettings.getInstance().getDefaultServer());
        String user_ = StringUtils.orElse(userName, OMEROSettings.getInstance().getDefaultUserName());
        String password_ = StringUtils.orElse(password.getPassword(), OMEROSettings.getInstance().getDefaultPassword().getPassword());
        int port_ = server_.contains(":") ? Integer.parseInt(server_.substring(server_.indexOf(':') + 1)) : 4064;
        String host_ = server_.contains(":") ? server_.substring(0, server_.indexOf(':')) : server_;
        try {
            return new OMEROLocation(host_, port_, user_, password_);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
