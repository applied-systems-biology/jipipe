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

package org.hkijena.jipipe.installer.linux.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.installer.linux.api.InstallerRun;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.installer.linux.ui.utils.JIPipeRunExecuterUI;
import org.hkijena.jipipe.installer.linux.ui.utils.JIPipeRunnerQueue;
import org.hkijena.jipipe.installer.linux.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainWindow extends JFrame {

    private final InstallerRun installerRun = new InstallerRun();

    public MainWindow() {
        initialize();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout()));
        setTitle("Install JIPipe");
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close the installer?", "Close installer");

        setContentPane(new SetupPanel(this));
        revalidate();
        repaint();
    }

    public InstallerRun getInstallerRun() {
        return installerRun;
    }

    public void installNow() {
        JIPipeRunExecuterUI ui = new JIPipeRunExecuterUI(installerRun);
        setContentPane(ui);
        revalidate();
        repaint();
        ui.startRun();
    }

    @Subscribe
    public void onInstallerSuccessful(RunUIWorkerFinishedEvent event) {
        JOptionPane.showMessageDialog(this,
                "The installation was successful. Click OK to close the setup.",
                "Installation successful",
                JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    @Subscribe
    public void onInstallerUnsuccessful(RunUIWorkerInterruptedEvent event) {
        JOptionPane.showMessageDialog(this,
                "The installation failed! Please take a look at the log to find out the reason.",
                "Installation failed",
                JOptionPane.ERROR_MESSAGE);
        try {
            Files.write(Paths.get("installer-log.txt"), installerRun.getLog().toString().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
