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

package org.hkijena.jipipe.plugins.imageviewer.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPickEnumValueDialog;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.LUTData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMap;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Based on {@link org.jdesktop.swingx.JXGradientChooser}
 */
public abstract class ImageViewerLUTEditor extends JPanel implements ThumbListener {
    private final JIPipeDesktopLegacyImageViewer imageViewerPanel;
    private final int targetChannel;
    private final SolidColorIcon changeColorButtonDisplayedColor = new SolidColorIcon(16, 16);
    private ColorMap lastColorMap = ColorMap.viridis;
    /**
     * The multi-thumb slider to use for the gradient stops
     */
    private JXMultiThumbSlider<Color> slider;
    private JButton deleteThumbButton;
    private JButton changeColorButton;
    private boolean isUpdating = false;
    private String channelName;
    private LUT cachedLUT;

    public ImageViewerLUTEditor(JIPipeDesktopLegacyImageViewer imageViewerPanel, int targetChannel) {
        this.imageViewerPanel = imageViewerPanel;
        this.targetChannel = targetChannel;
        this.channelName = "Channel " + (targetChannel + 1);
        initialize();
    }

    public void loadLUTFromImage() {
        ImagePlus image = imageViewerPanel.getImagePlus();
        if (image != null) {
            if (targetChannel < image.getLuts().length) {
                importLUT(image.getLuts()[targetChannel], true);
            }
        }
    }

    /**
     * Imports a LUT into the editor.
     *
     * @param lut      the LUT
     * @param simplify removes gradient stop points that are too close to each other
     */
    public void importLUT(LUT lut, boolean simplify) {
        LUTData lutData = LUTData.fromLUT(lut, simplify);
        importLUT(lutData);
    }

    public int getTargetChannel() {
        return targetChannel;
    }

    public LUT getLUT() {
        if (cachedLUT == null)
            cachedLUT = generateLUT();
        return cachedLUT;
    }

    private LUT generateLUT() {
        List<Thumb<Color>> stops = this.slider.getModel().getSortedThumbs();
        List<ColorUtils.GradientStop> gradientStops = new ArrayList<>();
        for (Thumb<Color> thumb : stops) {
            gradientStops.add(new ColorUtils.GradientStop(thumb.getPosition(), thumb.getObject()));
        }
        return ImageJUtils.createLUTFromGradient(gradientStops);
    }

    private void updateFromStop(int thumb, Color color) {
        if (thumb == -1) {
            changeColorButton.setEnabled(false);
            changeColorButtonDisplayedColor.setFillColor(Color.black);
            changeColorButton.repaint();
            deleteThumbButton.setEnabled(false);
        } else {
            changeColorButton.setEnabled(true);
            changeColorButton.repaint();
            changeColorButtonDisplayedColor.setFillColor(color);
            deleteThumbButton.setEnabled(true);
        }
        updateDeleteButtons();
    }

    private void updateDeleteButtons() {
        if (slider.getModel().getThumbCount() <= 2) {
            deleteThumbButton.setEnabled(false);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                        BorderFactory.createEmptyBorder(4, 4, 0, 4))));

        // Left (info) panel
        JPanel leftLabel = new JPanel(new BorderLayout());

        JLabel channelLabel = new JLabel("C" + (targetChannel + 1));
        channelLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));
        channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        leftLabel.add(channelLabel, BorderLayout.CENTER);
        leftLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4),
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Button.borderColor"))));

        add(leftLabel, BorderLayout.WEST);

        // Center panel
        JPanel centerPanel = new JPanel(new GridBagLayout());
        add(centerPanel, BorderLayout.CENTER);

        slider = new JXMultiThumbSlider<>();
        slider.getModel().addThumb(0, Color.BLACK);
        slider.getModel().addThumb(1, Color.WHITE);
        slider.setTrackRenderer(new CustomGradientTrackRenderer());
        slider.setThumbRenderer(new CustomGradientThumbRenderer());
        slider.setPreferredSize(new Dimension(100, 35));
        slider.addMultiThumbListener(this);
        centerPanel.add(slider, new GridBagConstraints(0,
                0,
                1,
                3,
                1,
                1,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));

        changeColorButton = new JButton("Edit", changeColorButtonDisplayedColor);
        changeColorButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        changeColorButton.setToolTipText("Set color of selected gradient stop");
        changeColorButton.addActionListener(e -> changeColor());
        changeColorButton.setHorizontalAlignment(SwingConstants.LEFT);
        centerPanel.add(changeColorButton, new GridBagConstraints(1,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(14, 0, 0, 0),
                0,
                0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0),
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Button.borderColor"))));
        centerPanel.add(toolBar, new GridBagConstraints(0,
                4,
                4,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));

        JButton addThumbButton = new JButton(UIUtils.getIconFromResources("actions/color-add.png"));
        addThumbButton.setToolTipText("Add color");
        UIUtils.makeButtonFlat(addThumbButton);
        addThumbButton.setHorizontalAlignment(SwingConstants.LEFT);
        addThumbButton.addActionListener(e -> addColor());
        centerPanel.add(addThumbButton, new GridBagConstraints(2,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(12, 0, 0, 0),
                0,
                0));

        deleteThumbButton = new JButton(UIUtils.getIconFromResources("actions/color-remove.png"));
        deleteThumbButton.setToolTipText("Remove color");
        UIUtils.makeButtonFlat(deleteThumbButton);
        deleteThumbButton.setHorizontalAlignment(SwingConstants.LEFT);
        deleteThumbButton.addActionListener(e -> removeColor());
        centerPanel.add(deleteThumbButton, new GridBagConstraints(3,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(12, 0, 0, 0),
                0,
                0));

        // Menu items
        JButton invertColorsButton = new JButton("Invert", UIUtils.getIconFromResources("actions/object-inverse.png"));
        UIUtils.makeButtonFlat(invertColorsButton);
        invertColorsButton.addActionListener(e -> invertColors());
        toolBar.add(invertColorsButton);

        JButton setToColorMapButton = new JButton("Color map", UIUtils.getIconFromResources("actions/color-gradient.png"));
        UIUtils.makeButtonFlat(setToColorMapButton);
        setToColorMapButton.addActionListener(e -> pickColorsFromColorMap());
        toolBar.add(setToColorMapButton);

        JButton moreButton = new JButton("More ...", UIUtils.getIconFromResources("actions/configure.png"));
        UIUtils.makeButtonFlat(moreButton);
        toolBar.add(moreButton);
        JPopupMenu moreMenu = UIUtils.addPopupMenuToButton(moreButton);

        JMenuItem exportLUTToJSONButton = new JMenuItem("Export LUT as *.json", UIUtils.getIconFromResources("actions/document-export.png"));
        exportLUTToJSONButton.addActionListener(e -> exportLUTToJSON());
        moreMenu.add(exportLUTToJSONButton);

        JMenuItem exportLUTToPNGButton = new JMenuItem("Export LUT as *.png", UIUtils.getIconFromResources("actions/document-export.png"));
        exportLUTToPNGButton.addActionListener(e -> exportLUTToPNG());
        moreMenu.add(exportLUTToPNGButton);

        JMenuItem importLUTFromJSONButton = new JMenuItem("Import LUT from *.json", UIUtils.getIconFromResources("actions/document-import.png"));
        importLUTFromJSONButton.addActionListener(e -> importLUTFromJSON());
        moreMenu.add(importLUTFromJSONButton);

        JMenuItem importLUTFromPNGButton = new JMenuItem("Import LUT from *.png", UIUtils.getIconFromResources("actions/document-import.png"));
        importLUTFromPNGButton.addActionListener(e -> importLUTFromPNG());
        moreMenu.add(importLUTFromPNGButton);
    }

    public void importLUT(LUTData lutData) {
        isUpdating = true;
        while (slider.getModel().getThumbCount() > lutData.size()) {
            slider.getModel().removeThumb(0);
        }
        while (slider.getModel().getThumbCount() < lutData.size()) {
            slider.getModel().addThumb(0, Color.RED);
        }

        for (int i = 0; i < lutData.size(); i++) {
            slider.getModel().getThumbAt(i).setPosition(lutData.get(i).getPosition());
            slider.getModel().getThumbAt(i).setObject(lutData.get(i).getColor());
        }
        cachedLUT = null;
        isUpdating = false;
    }

    private void importLUTFromJSON() {
        Path path = JIPipeFileChooserApplicationSettings.openFile(this, getImageViewerPanel().getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import LUT", UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            LUTData lutData = JsonUtils.readFromFile(path, LUTData.class);
            importLUT(lutData);
            SwingUtilities.invokeLater(this::applyLUT);
        }
    }

    private void exportLUTToJSON() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, getImageViewerPanel().getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export LUT", UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            LUTData lutData = new LUTData();
            for (int i = 0; i < slider.getModel().getThumbCount(); i++) {
                Thumb<Color> thumb = slider.getModel().getThumbAt(i);
                lutData.addStop(thumb.getPosition(), thumb.getObject());
            }
            JsonUtils.saveToFile(lutData, path);
        }
    }

    private void importLUTFromPNG() {
        Path path = JIPipeFileChooserApplicationSettings.openFile(this, getImageViewerPanel().getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import LUT", UIUtils.EXTENSION_FILTER_PNG);
        if (path != null) {
            ImagePlus img = IJ.openImage(path.toString());
            LUT lut = ImageJUtils.lutFromImage(img);
            importLUT(lut, true);
            SwingUtilities.invokeLater(this::applyLUT);
        }
    }

    private void exportLUTToPNG() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, getImageViewerPanel().getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export LUT", UIUtils.EXTENSION_FILTER_PNG);
        if (path != null) {
            LUT lut = generateLUT();
            ImagePlus img = ImageJUtils.lutToImage(lut, 256, 1);
            IJ.saveAs(img, "PNG", path.toString());
        }
    }

    private void pickColorsFromColorMap() {
        Object selected = JIPipeDesktopPickEnumValueDialog.showDialog(this, Arrays.asList(ColorMap.values()), new ColorMapEnumItemInfo(), lastColorMap, "Select LUT");
        if (selected instanceof ColorMap) {
            lastColorMap = (ColorMap) selected;
            importLUT(((ColorMap) selected).toLUT(), true);
            SwingUtilities.invokeLater(this::applyLUT);
        }
    }

    private void invertColors() {
        isUpdating = true;
        List<Color> current = new ArrayList<>();
        for (int i = 0; i < slider.getModel().getThumbCount(); i++) {
            Thumb<Color> thumb = slider.getModel().getThumbAt(i);
            current.add(thumb.getObject());
        }
        for (int i = 0; i < slider.getModel().getThumbCount(); i++) {
            Thumb<Color> thumb = slider.getModel().getThumbAt(i);
            thumb.setObject(current.get(current.size() - i - 1));
        }
        isUpdating = false;
        cachedLUT = null;
        applyLUT();
    }

    private void changeColor() {
        int index = slider.getSelectedIndex();
        if (index >= 0) {
            Color color = slider.getModel().getThumbAt(index).getObject();
            color = JColorChooser.showDialog(this, "Select color", color);
            if (color != null) {
                slider.getModel().getThumbAt(index).setObject(color);
                updateFromStop(index, color);
                cachedLUT = null;
                if (!isUpdating) {
                    applyLUT();
                }
            }
        }
    }

    private void removeColor() {
        if (slider.getModel().getThumbCount() <= 2)
            return;
        int index = slider.getSelectedIndex();
        if (index >= 0) {
            slider.getModel().removeThumb(index);
            updateFromStop(-1, null);
            cachedLUT = null;
        }
    }

    private void addColor() {
        float pos = 0.2f;
        Color color = Color.black;
        int num = slider.getModel().addThumb(pos, color);
        cachedLUT = null;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        Color color = slider.getModel().getThumbAt(thumb).getObject();
        updateFromStop(thumb, color);
        updateDeleteButtons();
        cachedLUT = null;
        if (!isUpdating) {
            applyLUT();
        }
    }

    @Override
    public void thumbSelected(int thumb) {
        if (thumb == -1) {
            updateFromStop(-1, Color.black);
            return;
        }
        Color color = slider.getModel().getThumbAt(thumb).getObject();
        updateFromStop(thumb, color);
        updateDeleteButtons();
        slider.repaint();
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (evt.getClickCount() > 1) {
            changeColor();
        }
    }


    public JIPipeDesktopLegacyImageViewer getImageViewerPanel() {
        return imageViewerPanel;
    }

    public abstract void applyLUT();
}
