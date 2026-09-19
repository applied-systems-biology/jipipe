package org.hkijena.jipipe.desktop.commons.components.servers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEvent;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeListener;
import org.hkijena.jipipe.api.servers.JIPipeServerInstance;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.desktop.commons.components.servers.monitor.JIPipeDesktopServerMonitorWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

/**
 * Status bar button that displays the current external server instance status and provides
 * controls to stop individual instances or open the server monitor.
 *
 * <p>Subscribes to the service-level {@link org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEventEmitter}
 * for event-driven updates instead of polling.</p>
 */
public class JIPipeDesktopServerStatusControl extends JButton
        implements MicroserviceStateChangeListener {

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final SpinnerIcon busyIcon;

    public JIPipeDesktopServerStatusControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.busyIcon = new SpinnerIcon(this);
        initialize();
        updateStatus();
        JIPipe.getInstance().getServerService()
                .getStateChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(JIPipe.RESOURCES.getIcon16("actions/server.png"));
        setText("Servers");
        setToolTipText("External server instances");
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        SwingUtilities.invokeLater(this::updateStatus);
    }

    private void updateStatus() {
        List<JIPipeServerInstance<?>> instances = JIPipe.getInstance().getServerService().getActiveInstances();
        int count = instances.size();
        boolean anyStarting = instances.stream().anyMatch(i ->
                i.getState() == MicroserviceState.Starting || i.getState() == MicroserviceState.Stopping);
        boolean anyFailed = instances.stream().anyMatch(i -> i.getState() == MicroserviceState.Failed);

        if (anyStarting) {
            setIcon(busyIcon);
            busyIcon.start();
        } else if (anyFailed) {
            busyIcon.stop();
            setIcon(JIPipe.RESOURCES.getIcon16("status/dialog-error.png"));
        } else {
            busyIcon.stop();
            setIcon(JIPipe.RESOURCES.getIcon16("actions/server.png"));
        }

        if (count == 0) {
            setText("No servers");
            setToolTipText("No external server instances running");
        } else {
            setText(count + " server" + (count > 1 ? "s" : ""));
            setToolTipText(count + " external server instance" + (count > 1 ? "s" : "") + " running");
        }
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        List<JIPipeServerInstance<?>> instances = JIPipe.getInstance().getServerService().getActiveInstances();
        if (instances.isEmpty()) {
            JMenuItem empty = new JMenuItem("No active servers");
            empty.setEnabled(false);
            popupMenu.add(empty);
        } else {
            for (JIPipeServerInstance<?> instance : instances) {
                String label = instance.getDisplayName() + " — " + instance.getState().name() + " (port " + instance.getPort() + ")";
                popupMenu.add(UIUtils.createMenuItem(label,
                        instance.getStateDetail(),
                        JIPipe.RESOURCES.getIcon16("actions/server.png"),
                        () -> stopInstance(instance)));
            }
        }
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Open server monitor",
                "Opens the server monitor window",
                JIPipe.RESOURCES.getIcon16("actions/document-preview.png"),
                this::openServerMonitor));
    }

    private void stopInstance(JIPipeServerInstance<?> instance) {
        JIPipe.getInstance().getServerService().stopInstance(instance);
    }

    private void openServerMonitor() {
        JIPipeDesktopServerMonitorWindow window = new JIPipeDesktopServerMonitorWindow(workbench);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
    }
}
