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

package org.hkijena.jipipe.extensions.ijtrackmate.display;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SpotListCellRenderer extends JPanel implements ListCellRenderer<Spot> {

    private final SpotsManagerPlugin spotsManagerPlugin;
    private SolidColorIcon strokeFillPreview = new SolidColorIcon(16, 16);
    private JLabel iconLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel infoLabel = new JLabel();

    private FeatureColorGenerator<Spot> strokeColorGenerator;

    public SpotListCellRenderer(SpotsManagerPlugin spotsManagerPlugin) {
        this.spotsManagerPlugin = spotsManagerPlugin;
        initialize();
        updateColorMaps();
        spotsManagerPlugin.getDisplaySettings().listeners().add(this::updateColorMaps);
    }

    public void updateColorMaps() {
        if(spotsManagerPlugin.getSpotsCollection() != null) {
            strokeColorGenerator = FeatureUtils.createSpotColorGenerator(spotsManagerPlugin.getSpotsCollection().getModel(), spotsManagerPlugin.getDisplaySettings());
        }
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

    @Override
    public Component getListCellRendererComponent(JList<? extends Spot> list, Spot value, int index, boolean isSelected, boolean cellHasFocus) {

        if (!StringUtils.isNullOrEmpty(value.getName()))
            nameLabel.setText(value.getName());
        else
            nameLabel.setText("Unnamed [" + index + "]");

        strokeFillPreview.setFillColor(Color.WHITE);
        if(strokeColorGenerator == null) {
            strokeFillPreview.setBorderColor(Color.RED);
        }
        else {
            strokeFillPreview.setBorderColor(strokeColorGenerator.color(value));
        }

        DecimalFormat format = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

        iconLabel.setText("" + index);
        infoLabel.setText("x: " + format.format(value.getDoublePosition(0)) + ", y: " + format.format(value.getDoublePosition(1)) + ", z: " + format.format(value.getDoublePosition(2)) +
                ", t: " + format.format(value.getFeature(Spot.FRAME)) + ", r: " + format.format(value.getFeature(Spot.RADIUS)) + ", q: " + format.format(value.getFeature(Spot.QUALITY)));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
