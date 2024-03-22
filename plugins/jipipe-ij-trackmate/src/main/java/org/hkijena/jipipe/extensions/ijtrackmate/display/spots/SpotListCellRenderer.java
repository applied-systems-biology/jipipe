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

package org.hkijena.jipipe.extensions.ijtrackmate.display.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.UniformSpotColorGenerator;
import ij.measure.Calibration;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotDrawer;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackMateUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class SpotListCellRenderer extends JPanel implements ListCellRenderer<Spot> {

    private final SpotsManagerPlugin2D spotsManagerPlugin;
    private final SolidJIPipeDesktopColorIcon strokeFillPreview = new SolidJIPipeDesktopColorIcon(16, 16);
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();
    private FeatureColorGenerator<Spot> strokeColorGenerator;

    public SpotListCellRenderer(SpotsManagerPlugin2D spotsManagerPlugin) {
        this.spotsManagerPlugin = spotsManagerPlugin;
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        iconLabel.setIcon(strokeFillPreview);
        infoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
    }

    public void updateColorMaps() {
        if (spotsManagerPlugin.getSpotsCollection() != null) {
            SpotDrawer spotDrawer = spotsManagerPlugin.getSpotDrawer();
            if (spotDrawer.isUniformStrokeColor()) {
                strokeColorGenerator = new UniformSpotColorGenerator(spotDrawer.getStrokeColor());
            } else {
                DisplaySettings displaySettings = spotDrawer.createDisplaySettings(spotsManagerPlugin.getSpotsCollection());
                strokeColorGenerator = FeatureUtils.createSpotColorGenerator(spotsManagerPlugin.getSpotsCollection().getModel(), displaySettings);
            }
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Spot> list, Spot value, int index, boolean isSelected, boolean cellHasFocus) {

        if (!StringUtils.isNullOrEmpty(value.getName()))
            nameLabel.setText(value.getName());
        else
            nameLabel.setText("Unnamed [" + index + "]");

        strokeFillPreview.setFillColor(Color.WHITE);
        if (strokeColorGenerator == null) {
            strokeFillPreview.setBorderColor(spotsManagerPlugin.getSpotDrawer().getStrokeColor());
        } else {
            strokeFillPreview.setBorderColor(strokeColorGenerator.color(value));
        }

        iconLabel.setText("" + index);
        final DecimalFormat format = TrackMateUtils.FEATURE_DECIMAL_FORMAT;
        Calibration calibration = spotsManagerPlugin.getSpotsCollection().getImage().getCalibration();
        infoLabel.setText("x: " + format.format(value.getDoublePosition(0)) + " " + calibration.getXUnit() +
                ", y: " + format.format(value.getDoublePosition(1)) + " " + calibration.getYUnit() +
                ", z: " + format.format(value.getDoublePosition(2)) + " " + calibration.getZUnit() +
                ", t: " + format.format(value.getFeature(Spot.FRAME)) +
                ", r: " + format.format(value.getFeature(Spot.RADIUS)) + " " + calibration.getXUnit() +
                ", q: " + format.format(value.getFeature(Spot.QUALITY)));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
