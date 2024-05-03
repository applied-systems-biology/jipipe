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

package org.hkijena.jipipe.desktop.app;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.scijava.Context;

import javax.swing.*;

/**
 * A dummy implementation of {@link JIPipeWorkbench} that will trigger no errors, but will not work for various functions.
 * Use at own discretion.
 */
public class JIPipeDummyWorkbench implements JIPipeWorkbench {

    private final JFrame frame = new JFrame();
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();


    @Override
    public void sendStatusBarText(String text) {

    }

    @Override
    public Context getContext() {
        if (JIPipe.getInstance() != null)
            return JIPipe.getInstance().getContext();
        else
            return new Context();
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Override
    public void showMessageDialog(String message, String title) {

    }

    @Override
    public void showErrorDialog(String message, String title) {

    }

    @Override
    public JIPipeProject getProject() {
        return null;
    }
}
