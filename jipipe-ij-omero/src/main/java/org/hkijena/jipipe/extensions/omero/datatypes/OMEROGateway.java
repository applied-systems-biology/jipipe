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

package org.hkijena.jipipe.extensions.omero.datatypes;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.exception.DSOutOfServiceException;
import omero.log.SimpleLogger;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

public class OMEROGateway implements JIPipeData {

    private final LoginCredentials credentials;
    private final Gateway gateway;

    public OMEROGateway(LoginCredentials credentials) {
        this.credentials = credentials;
        this.gateway = new Gateway(new SimpleLogger());
    }

    public OMEROGateway(OMEROGateway other) {
        this.credentials = other.credentials;
        this.gateway = other.gateway;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    @Override
    public JIPipeData duplicate() {
        return new OMEROGateway(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench) {
    }

    @Override
    public void flush() {
        if(gateway.isConnected()) {
            gateway.disconnect();
        }
    }

    /**
     * Ensures that the connection is up and alive
     * Should be run before executing any operation on the gateway
     */
    public void ensureConnection() {
        if(!gateway.isConnected()) {
            try {
                gateway.connect(credentials);
            } catch (DSOutOfServiceException e) {
                throw new UserFriendlyRuntimeException(e,
                        "Could not connect to OMERO server",
                        "OMERO Gateway",
                        "Tried to connect to OMERO server '" + credentials.getServer().getHostname() + ":" + credentials.getServer().getPort() + "' with user name '" + credentials.getUser().getUsername() + "', but the connection was not successful.",
                        "Please check that the server URL and your credentials are correct.");
            }
        }
    }

    public Gateway getGateway() {
        return gateway;
    }
}
