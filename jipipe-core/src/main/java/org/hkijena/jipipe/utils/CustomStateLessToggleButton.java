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

package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomStateLessToggleButton extends JButton implements ActionListener {
    private final ToggledEventEmitter toggledEventEmitter;
    private String labelUnselected;
    private String labelSelected;
    private Icon iconSelected;
    private Icon iconUnselected;
    private boolean state;

    public CustomStateLessToggleButton(String labelUnselected, Icon iconUnselected, String labelSelected, Icon iconSelected, boolean state) {
        this.labelUnselected = labelUnselected;
        this.labelSelected = labelSelected;
        this.iconSelected = iconSelected;
        this.iconUnselected = iconUnselected;
        this.state = state;
        toggledEventEmitter = new ToggledEventEmitter();
        updateState();
        addActionListener(this);

    }

    public void updateState() {
        if (state) {
            setText(labelSelected);
            setIcon(iconSelected);
        } else {
            setText(labelUnselected != null ? labelUnselected : labelSelected);
            setIcon(iconUnselected != null ? iconUnselected : iconSelected);
        }
    }

    public String getLabelUnselected() {
        return labelUnselected;
    }

    public void setLabelUnselected(String labelUnselected) {
        this.labelUnselected = labelUnselected;
        updateState();
    }

    public String getLabelSelected() {
        return labelSelected;
    }

    public void setLabelSelected(String labelSelected) {
        this.labelSelected = labelSelected;
        updateState();
    }

    public Icon getIconSelected() {
        return iconSelected;
    }

    public void setIconSelected(Icon iconSelected) {
        this.iconSelected = iconSelected;
        updateState();
    }

    public Icon getIconUnselected() {
        return iconUnselected;
    }

    public void setIconUnselected(Icon iconUnselected) {
        this.iconUnselected = iconUnselected;
        updateState();
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
        updateState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        state = !state;
        updateState();
        toggledEventEmitter.emit(new ToggledEvent(this));
    }

    public ToggledEventEmitter getToggledEventEmitter() {
        return toggledEventEmitter;
    }

    public interface ToggledEventListener {
        void onToggle(ToggledEvent event);
    }

    public static class ToggledEvent extends AbstractJIPipeEvent {
        private final CustomStateLessToggleButton button;

        public ToggledEvent(CustomStateLessToggleButton button) {
            super(button);
            this.button = button;
        }

        public CustomStateLessToggleButton getButton() {
            return button;
        }
    }

    public static class ToggledEventEmitter extends JIPipeEventEmitter<ToggledEvent, ToggledEventListener> {

        @Override
        protected void call(ToggledEventListener toggledEventListener, ToggledEvent event) {
            toggledEventListener.onToggle(event);
        }
    }


}
