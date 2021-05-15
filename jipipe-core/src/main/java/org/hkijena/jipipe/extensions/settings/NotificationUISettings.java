package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalIntegerParameter;

public class NotificationUISettings implements JIPipeParameterCollection {
    public static String ID = "notification-ui";
    private final EventBus eventBus = new EventBus();

    private boolean showRunNotifications = true;
    private boolean showRunOnlyIfInactive = false;
    private OptionalIntegerParameter showAfterMinRuntime = new OptionalIntegerParameter(true, 7);

    @JIPipeDocumentation(name = "Show run notifications", description = "If enabled, show notifications when a run is finished. " +
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

    @JIPipeDocumentation(name = "Run notifications only if window is inactive", description = "If enabled, run notifications " +
            "(see description of 'Show run notifications') are only shown if no JIPipe window is currently active.")
    @JIPipeParameter("show-run-notifications-only-if-inactive")
    public boolean isShowRunOnlyIfInactive() {
        return showRunOnlyIfInactive;
    }

    @JIPipeParameter("show-run-notifications-only-if-inactive")
    public void setShowRunOnlyIfInactive(boolean showRunOnlyIfInactive) {
        this.showRunOnlyIfInactive = showRunOnlyIfInactive;
    }

    @JIPipeDocumentation(name = "Run notifications only for long runs", description = "If enabled, run notifications " +
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static NotificationUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, NotificationUISettings.class);
    }
}