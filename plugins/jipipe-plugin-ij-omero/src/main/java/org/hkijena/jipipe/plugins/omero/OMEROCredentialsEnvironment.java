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

package org.hkijena.jipipe.plugins.omero;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import omero.gateway.LoginCredentials;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDummyWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class OMEROCredentialsEnvironment extends JIPipeEnvironment {
    private String host = "";
    private int port = 4064;
    private String userName = "";
    private PasswordParameter password = new PasswordParameter();
    private String email = "anonymous@anonymous";
    private String webclientUrl = "";

    private SecretCredentials secretCredentials = null;

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
        return OMEROPlugin.RESOURCES.getIconFromResources("omero.png");
    }

    @Override
    public String getInfo() {
        return StringUtils.orElse(userName, "[no user]") + "@" + StringUtils.orElse(host, "[no host]") + ":" + port;
    }

    @SetJIPipeDocumentation(name = "Host", description = "The server host. For example <code>localhost</code>, <code>my.server.name</code>, or <code>wss://my.server.name</code>.")
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

    @SetJIPipeDocumentation(name = "Port", description = "The server port. Set to zero to use the global default port.")
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

    @SetJIPipeDocumentation(name = "User name", description = "The user name. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the username. " +
            "In a CLI environment, the pipeline will fail.")
    @JIPipeParameter(value = "user-name", uiOrder = -98)
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

    @SetJIPipeDocumentation(name = "Password", description = "The password. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
            "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
    @JIPipeParameter(value = "password", uiOrder = -97)
    @JsonGetter("password")
    public PasswordParameter getPassword() {
        return password;
    }

    @JIPipeParameter("password")
    @JsonSetter("password")
    public void setPassword(PasswordParameter password) {
        this.password = password;
    }

    @SetJIPipeDocumentation(name = "E-Mail", description = "The E-Mail address sent to the server")
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

    @SetJIPipeDocumentation(name = "Web-client URL", description = "URL of the OMERO web-client. Used for displaying data.")
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
        String secretPassword, secretUserName, secretHost;
        int secretPort;

        if(secretCredentials != null) {
            secretPassword = StringUtils.orElse(secretCredentials.getPassword().getPassword(), password.getPassword());
            secretUserName = StringUtils.orElse(secretCredentials.getUserName(), userName);
            secretHost = StringUtils.orElse(secretCredentials.getHost(), host);
            secretPort = secretCredentials.getPort() > 0 ? secretCredentials.getPort() : port;
        }
        else {
            secretPassword = password.getPassword();
            secretUserName = userName;
            secretHost = host;
            secretPort = port;
        }

        return new LoginCredentials(secretUserName, secretPassword, secretHost, secretPort);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if(JIPipe.getInstance().getMode() == JIPipeMode.Headless) {
            if (StringUtils.isNullOrEmpty(userName) || StringUtils.isNullOrEmpty(host) || StringUtils.isNullOrEmpty(email)) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        reportContext,
                        "Invalid OMERO credentials",
                        "Please ensure to always provide a user name, host, and email address."));
            }
        }
    }

    @Override
    public void runPreconfigure(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {
        super.runPreconfigure(run, progressInfo);

        if(secretCredentials != null) {
            return;
        }

        if(StringUtils.isNullOrEmpty(host) || StringUtils.isNullOrEmpty(userName) || password == null || StringUtils.isNullOrEmpty(password.getPassword())) {
            if(JIPipe.getInstance().getMode() == JIPipeMode.GUI) {
                progressInfo.log("-> OMERO connection to " + host + " has missing credentials. Asking for password interactively.");
                progressInfo.log("OMERO: Waiting for user input ...");

                AtomicBoolean cancelled = new AtomicBoolean(true);
                AtomicBoolean windowOpened = new AtomicBoolean(true);
                SecretCredentials newSecrets = new SecretCredentials(this);
                Object lock = new Object();

                synchronized (lock) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            JIPipeDesktopWorkbench workbench = JIPipeDesktopProjectWorkbench.tryFindProjectWorkbench(run.getProject().getGraph(), new JIPipeDummyWorkbench());
                            if(JIPipeDesktopParameterFormPanel.showDialog(workbench,
                                    newSecrets,
                                    new MarkdownText("# OMERO login\n\nPlease fill out all the fields to supply the required OMERO login credentials."),
                                    "OMERO login",
                                    JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.WITH_SCROLLING)) {
                                cancelled.set(false);
                            }
                            else {
                                cancelled.set(true);
                            }

                            windowOpened.set(false);
                            synchronized (lock) {
                                lock.notify();
                            }
                        } catch (Throwable e) {
                            windowOpened.set(false);
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });

                    try {
                        while (windowOpened.get()) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (cancelled.get()) {
                    progressInfo.log("No login credentials provided (cancelled)!");
                    throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new UnspecifiedValidationReportContext(),
                            "Operation cancelled by user",
                            "You clicked 'Cancel'"));
                }
                else {
                    // Confirm secrets
                    this.secretCredentials = newSecrets;
                }
            }
            else {
                progressInfo.log("-> OMERO connection to " + host + " has missing credentials, but JIPipe is in non-GUI mode. CONTINUING WITH EMPTY VALUES WHERE NONE ARE PROVIDED!");
            }
        }
    }

    @Override
    public void runPostprocessing(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {
        super.runPostprocessing(run, progressInfo);

        if(secretCredentials != null) {
            progressInfo.log("Clearing OMERO secrets ...");
            this.secretCredentials = null;
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

    public static class SecretCredentials extends AbstractJIPipeParameterCollection {
        private String host = "";
        private int port = 4064;
        private String userName = "";
        private PasswordParameter password = new PasswordParameter();
        private String email = "anonymous@anonymous";

        public SecretCredentials() {
        }

        public SecretCredentials(OMEROCredentialsEnvironment environment) {
            this.host = StringUtils.orElse(environment.getHost(), "");
            this.port = environment.getPort();
            this.userName = StringUtils.orElse(environment.getUserName(), "");
            this.email = environment.getEmail();
        }

        @SetJIPipeDocumentation(name = "Host", description = "The server host. For example <code>localhost</code>, <code>my.server.name</code>, or <code>wss://my.server.name</code>.")
        @JIPipeParameter(value = "host", uiOrder = -100)
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

        @SetJIPipeDocumentation(name = "Port", description = "The server port. Set to zero to use the global default port.")
        @JIPipeParameter(value = "port", uiOrder = -99)
        @JsonGetter("port")
        public int getPort() {
            return port;
        }

        @JIPipeParameter("port")
        @JsonSetter("port")
        public void setPort(int port) {
            this.port = port;
        }

        @SetJIPipeDocumentation(name = "User name", description = "The user name. " +
                "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the username. " +
                "In a CLI environment, the pipeline will fail.")
        @JIPipeParameter(value = "user-name", uiOrder = -98)
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

        @SetJIPipeDocumentation(name = "Password", description = "The password. The password is not saved in clear text, but encoded in Base64, which can be easily decoded by scripts. " +
                "If you use JIPipe in a GUI environment, it will ask for the credentials when running a pipeline if you do not provide the password. In a CLI environment, the pipeline will fail.")
        @JIPipeParameter(value = "password", uiOrder = -97)
        @JsonGetter("password")
        public PasswordParameter getPassword() {
            return password;
        }

        @JIPipeParameter("password")
        @JsonSetter("password")
        public void setPassword(PasswordParameter password) {
            this.password = password;
        }

        @SetJIPipeDocumentation(name = "E-Mail", description = "The E-Mail address sent to the server")
        @JIPipeParameter(value = "e-mail", uiOrder = -96)
        @JsonGetter("e-mail")
        public String getEmail() {
            return email;
        }

        @JIPipeParameter("e-mail")
        @JsonSetter("e-mail")
        public void setEmail(String email) {
            this.email = email;
        }
    }
}
