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

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CachePreviewControlUI extends JPanel {

    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private final JButton zoomStatusButton = new JButton();

    public CachePreviewControlUI() {
        initialize();
        refreshZoomStatus();
        dataSettings.getEventBus().register(this);
    }

    private void refreshZoomStatus() {
        zoomStatusButton.setText(dataSettings.getPreviewSize() + "px");
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/zoom-out.png"));
        UIUtils.makeFlat25x25(zoomOutButton);
        zoomOutButton.addActionListener(e -> decreaseSize());
        add(zoomOutButton);

        UIUtils.makeBorderlessWithoutMargin(zoomStatusButton);
        zoomStatusButton.addActionListener(e -> setSizeManually());
        add(zoomStatusButton);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/zoom-in.png"));
        UIUtils.makeFlat25x25(zoomInButton);
        zoomInButton.addActionListener(e -> increaseSize());
        add(zoomInButton);
    }

    private void setSizeManually() {
        int current = dataSettings.getPreviewSize();
        Integer selected = UIUtils.getIntegerByDialog(this, "Set preview size", "Set the preview size in pixels:", current, 40, Integer.MAX_VALUE);
        if(selected != null) {
            int newSize = selected;
            if(newSize != current) {
                JIPipeParameterCollection.setParameter(dataSettings, "preview-size", newSize);
            }
        }
    }

    private void decreaseSize() {
        int current = dataSettings.getPreviewSize();
        int newSize = Math.max(40, current - Math.max(1, current / 3));
        if(newSize != current) {
            JIPipeParameterCollection.setParameter(dataSettings, "preview-size", newSize);
        }
    }

    private void increaseSize() {
        int current = dataSettings.getPreviewSize();
        int newSize = Math.max(40, current - Math.max(1, current / 3));
        if(newSize != current) {
            JIPipeParameterCollection.setParameter(dataSettings, "preview-size", newSize);
        }
    }

    @Subscribe
    public void onSettingChanged(ParameterChangedEvent event) {
        if("preview-width".equals(event.getKey())) {
            refreshZoomStatus();
        }
    }
}
