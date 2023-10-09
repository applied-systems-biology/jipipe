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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import omero.gateway.LoginCredentials;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

public class OMEROCredentialsEnvironment extends JIPipeEnvironment {
    private String host = "";
    private int port = 4064;
    private String userName = "";
    private PasswordParameter password = new PasswordParameter();
    private String email = "anonymous@anonymous";
    private String webclientUrl = "";

    public OMEROCredentialsEnvironment() {
    }

    public OMEROCredentialsEnvironment(OMEROCredentialsEnvironment other) {
        this.host = other.host;
        this.userName = other.userName;
        this.port = other.port;
        this.password = new PasswordParameter(other.password);
        this.email = other.email;
        this.webclientUrl = other.webclientUrl;
    }

    @Override
    public Icon getIcon() {
        return OMEROExtension.RESOURCES.getIconFromResources("omero.png");
    }

    @Override
    public String getInfo() {
        return StringUtils.orElse(userName, "[no user]") + "@" + StringUtils.orElse(host, "[no host]") + ":" + port;
    }

    @JIPipeDocumentation(name = "Host", description = "The server host. For example <code>localhost</code>, <code>my.server.name</code>, or <code>wss://my.server.name</code>.")
    @JIPipeParameter(value = "host", uiOrder = -100, important = true)
    @StringParameterSettings(monospace = true)
    @JsonGetter("host")
    public String getHost() {
        return host;
    }

    @JIPipeParameter("host")
    @JsonSetter("host")
    public void setHost(String host) {
        this.host = host;
    }

    @JIPipeDocumentation(name = "Port", description = "The server port. Set to zero to use the global default port.")
    @JIPipeParameter(value = "port", uiOrder = -99, important = true)
    @JsonGetter("port")
    public int getPort() {
        return port;
    }

    @JIPipeParameter("port")
    @JsonSetter("port")
    public void setPort(int port) {
        this.port = port;
    }

    @JIPipeDocumentation(name = "User name", description = "The user name")
    @JIPipeParameter(value = "user-name", uiOrder = -98, important = true)
    @StringParameterSettings(monospace = true)
    @JsonGetter("user-name")
    public String getUserName() {
        return userName;
    }

    @JIPipeParameter("user-name")
    @JsonSetter("user-name")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JIPipeDocumentation(name = "Password", description = "The password. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
    @JIPipeParameter(value = "password", uiOrder = -97, important = true)
    @JsonGetter("password")
    public PasswordParameter getPassword() {
        return password;
    }

    @JIPipeParameter("password")
    @JsonSetter("password")
    public void setPassword(PasswordParameter password) {
        this.password = password;
    }

    @JIPipeDocumentation(name = "E-Mail", description = "The E-Mail address sent to the server")
    @JIPipeParameter(value = "e-mail", important = true, uiOrder = -96)
    @JsonGetter("e-mail")
    public String getEmail() {
        return email;
    }

    @JIPipeParameter("e-mail")
    @JsonSetter("e-mail")
    public void setEmail(String email) {
        this.email = email;
    }

    @JIPipeDocumentation(name = "Web-client URL", description = "URL of the OMERO web-client. Used for displaying data.")
    @JIPipeParameter(value = "webclient-url", important = true, uiOrder = -95)
    @StringParameterSettings(monospace = true)
    @JsonGetter("webclient-url")
    public String getWebclientUrl() {
        return webclientUrl;
    }

    @JIPipeParameter("webclient-url")
    @JsonGetter("webclient-url")
    public void setWebclientUrl(String webclientUrl) {
        this.webclientUrl = webclientUrl;
    }

    /**
     * Converts the credentials into a SciJava OMERO location
     *
     * @return the location
     */
    public LoginCredentials toLoginCredentials() {
        return new LoginCredentials(userName, password.getPassword(), host, port);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if(StringUtils.isNullOrEmpty(userName) || StringUtils.isNullOrEmpty(host) || StringUtils.isNullOrEmpty(email)) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Invalid OMERO credentials",
                    "Please ensure to always provide a user name, host, and email address."));
        }
    }

    public static class List extends ListParameter<OMEROCredentialsEnvironment> {

        public List() {
            super(OMEROCredentialsEnvironment.class);
        }

        public List(List other) {
            super(OMEROCredentialsEnvironment.class);
            for (OMEROCredentialsEnvironment environment : other) {
                add(new OMEROCredentialsEnvironment(environment));
            }
        }
    }
}
