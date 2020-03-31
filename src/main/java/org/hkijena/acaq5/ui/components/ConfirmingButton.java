package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Button that shows a nice confirmation of some action taken
 */
public class ConfirmingButton extends JButton {

    private Timer timer;
    private Icon iconBackup;
    private Icon confirmIcon;

    /**
     * Creates a button
     */
    public ConfirmingButton() {
        super();
        initialize();
    }

    /**
     * @param icon icon
     */
    public ConfirmingButton(Icon icon) {
        super(icon);
        initialize();
    }

    /**
     * @param text button text
     */
    public ConfirmingButton(String text) {
        super(text);
        initialize();
    }

    /**
     * @param a button action
     */
    public ConfirmingButton(Action a) {
        super(a);
        initialize();
    }

    /**
     * @param text button text
     * @param icon button icon
     */
    public ConfirmingButton(String text, Icon icon) {
        super(text, icon);
        initialize();
    }

    private void initialize() {
        timer = new Timer(1000, this::restoreIcon);
        confirmIcon = UIUtils.getIconFromResources("check-circle-green.png");
        addActionListener(this::showConfirmIcon);
    }

    private void showConfirmIcon(ActionEvent actionEvent) {
        iconBackup = getIcon();
        setIcon(confirmIcon);
        timer.start();
    }

    private void restoreIcon(ActionEvent actionEvent) {
        setIcon(iconBackup);
    }

    public Icon getConfirmIcon() {
        return confirmIcon;
    }

    public void setConfirmIcon(Icon confirmIcon) {
        this.confirmIcon = confirmIcon;
    }
}
