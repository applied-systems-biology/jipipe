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

package org.hkijena.jipipe.desktop.app.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/**
 * In-window replacement for the former modal project loading/closing
 * dialogs (feature #1359): a centered spinner, a task label, an optional
 * detail line (the project path), a status line fed from the run's
 * progress log, and an optional cancel button.
 * <p>
 * The project window swaps this panel in as its content pane, so no second
 * OS window is created. A fast operation can therefore no longer leave a
 * stray window behind (the former modal dialog could get stuck open on
 * some window managers, e.g. Ubuntu 24.04 with Mutter).
 */
public class JIPipeDesktopProjectWindowLoadingScreen extends JPanel {
    private final SpinnerIcon spinnerIcon;
    private final JLabel spinnerLabel;
    private final JLabel taskLabel;
    private final JLabel detailLabel;
    private final JLabel statusLabel;
    private final JButton cancelButton;

    /**
     * @param task   the task label (e.g. "Loading project")
     * @param detail optional detail line (e.g. the project path)
     */
    public JIPipeDesktopProjectWindowLoadingScreen(String task, Path detail) {
        setLayout(new GridBagLayout());

        spinnerIcon = new SpinnerIcon(this, 32);
        spinnerIcon.start();
        spinnerLabel = new JLabel(spinnerIcon);
        spinnerLabel.setAlignmentX(CENTER_ALIGNMENT);

        taskLabel = new JLabel(task);
        taskLabel.setFont(taskLabel.getFont().deriveFont(Font.BOLD));
        taskLabel.setAlignmentX(CENTER_ALIGNMENT);

        detailLabel = new JLabel(detail != null ? detail.toString() : null);
        detailLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, taskLabel.getFont().getSize()));
        Color detailColor = UIManager.getColor("Label.disabledForeground");
        detailLabel.setForeground(detailColor != null ? detailColor : Color.GRAY);
        detailLabel.setVisible(detail != null);
        detailLabel.setAlignmentX(CENTER_ALIGNMENT);

        statusLabel = new JLabel();
        statusLabel.setVisible(false);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);

        cancelButton = new JButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
        cancelButton.setAlignmentX(CENTER_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(spinnerLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(taskLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(detailLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(16));
        content.add(cancelButton);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.NONE;
        add(content, constraints);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        // Stop the animation timer once the screen is no longer shown
        spinnerIcon.stop();
    }

    public void setTask(String task) {
        taskLabel.setText(task);
    }

    public void setDetail(Path detail) {
        detailLabel.setText(detail != null ? detail.toString() : null);
        detailLabel.setVisible(detail != null);
    }

    /**
     * Shows the latest progress log line (hidden while empty), so the user
     * can follow what is happening for larger projects
     *
     * @param status the status line (may be empty to hide the line)
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
        statusLabel.setVisible(!StringUtils.isNullOrEmpty(status));
        revalidate();
        repaint();
    }

    public void setCancelVisible(boolean visible) {
        cancelButton.setVisible(visible);
        revalidate();
        repaint();
    }

    public void setCancelAction(Runnable action) {
        for (ActionListener listener : cancelButton.getActionListeners()) {
            cancelButton.removeActionListener(listener);
        }
        if (action != null) {
            cancelButton.addActionListener(e -> action.run());
        }
    }
}
