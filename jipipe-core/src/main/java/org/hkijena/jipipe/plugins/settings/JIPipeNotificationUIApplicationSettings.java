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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeNotificationUIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static String ID = "org.hkijena.jipipe:notification-ui";
    private boolean showRunNotifications = true;
    private boolean showRunOnlyIfInactive = false;
    private OptionalIntegerParameter showAfterMinRuntime = new OptionalIntegerParameter(true, 7);
    private StringList blockedNotifications = new StringList();
    private boolean enableNotifications = true;

    private boolean showNotificationsAfterFirstStart = true;

    public static JIPipeNotificationUIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeNotificationUIApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Show run notifications", description = "If enabled, show notifications when a run is finished. " +
            "Following processes are considered as run: " +
            "<ul>" +
            "<li>Pipeline run</li>" +
            "<li>Update cache / Cache intermediate results / Run &amp; show results</li>" +
            "<li>Export images or videos (image viewer)</li>" +
            "<li>Importing / exporting data or caches</li>" +
            "<li>Installing environments (Python, R, ...)</li>" +
            "<li>Installing / updating ImageJ plugins (via JIPipe)</li>" +
            "</ul>")
    @JIPipeParameter("show-run-notifications")
    public boolean isShowRunNotifications() {
        return showRunNotifications;
    }

    @JIPipeParameter("show-run-notifications")
    public void setShowRunNotifications(boolean showRunNotifications) {
        this.showRunNotifications = showRunNotifications;
    }

    @SetJIPipeDocumentation(name = "Run notifications only if window is inactive", description = "If enabled, run notifications " +
            "(see description of 'Show run notifications') are only shown if no JIPipe window is currently active.")
    @JIPipeParameter("show-run-notifications-only-if-inactive")
    public boolean isShowRunOnlyIfInactive() {
        return showRunOnlyIfInactive;
    }

    @JIPipeParameter("show-run-notifications-only-if-inactive")
    public void setShowRunOnlyIfInactive(boolean showRunOnlyIfInactive) {
        this.showRunOnlyIfInactive = showRunOnlyIfInactive;
    }

    @SetJIPipeDocumentation(name = "Run notifications only for long runs", description = "If enabled, run notifications " +
            "(see description of 'Show run notifications') are only shown if the run took the minimum time in minutes as " +
            "provided by the parameter.")
    @JIPipeParameter("only-show-run-after-min-runtime")
    public OptionalIntegerParameter getShowAfterMinRuntime() {
        return showAfterMinRuntime;
    }

    @JIPipeParameter("only-show-run-after-min-runtime")
    public void setShowAfterMinRuntime(OptionalIntegerParameter showAfterMinRuntime) {
        this.showAfterMinRuntime = showAfterMinRuntime;
    }

    @SetJIPipeDocumentation(name = "Blocked action notifications", description = "This is a list of action notification Ids that are blocked and will " +
            "not be shown at the top right.")
    @JIPipeParameter("blocked-action-notifications")
    public StringList getBlockedNotifications() {
        return blockedNotifications;
    }

    @JIPipeParameter("blocked-action-notifications")
    public void setBlockedNotifications(StringList blockedNotifications) {
        this.blockedNotifications = blockedNotifications;
    }

    @SetJIPipeDocumentation(name = "Enable action notifications", description = "If enabled, JIPipe will inform you about broken configurations " +
            "or missing functions at the top right of the window.")
    @JIPipeParameter("enable-action-notifications")
    public boolean isEnableNotifications() {
        return enableNotifications;
    }

    @JIPipeParameter("enable-action-notifications")
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    @SetJIPipeDocumentation(name = "Show action notifications on first start", description = "If enabled, show the list of action notifications on the first start of JIPipe")
    @JIPipeParameter("show-notifications-after-first-start")
    public boolean isShowNotificationsAfterFirstStart() {
        return showNotificationsAfterFirstStart;
    }

    @JIPipeParameter("show-notifications-after-first-start")
    public void setShowNotificationsAfterFirstStart(boolean showNotificationsAfterFirstStart) {
        this.showNotificationsAfterFirstStart = showNotificationsAfterFirstStart;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/dialog-messages.png");
    }

    @Override
    public String getName() {
        return "Notifications";
    }

    @Override
    public String getDescription() {
        return "Determines when and how you are notified by JIPipe";
    }
}
