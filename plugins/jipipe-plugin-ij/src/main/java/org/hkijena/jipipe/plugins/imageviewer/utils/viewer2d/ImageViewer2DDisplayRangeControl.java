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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

public class ImageViewer2DDisplayRangeControl extends JPanel implements ThumbListener {

    public static final DecimalFormat DECIMAL_FORMAT_FLOAT = new DecimalFormat("0.###");
    public static final DecimalFormat DECIMAL_FORMAT_INT = new DecimalFormat("0");

    private final CalibrationPlugin2D calibrationPlugin;
    private JXMultiThumbSlider<DisplayRangeStop> slider;
    private ImageViewer2DDisplayRangeControlTrackRenderer trackRenderer;
    private boolean isUpdating = false;
    private double customMin;
    private double customMax;

    private ImageJCalibrationMode mode = ImageJCalibrationMode.Custom;
    private double minSelectableValue;
    private double maxSelectableValue;

    private WeakReference<ImagePlus> lastSelectableValueCalculationBasis;

    public ImageViewer2DDisplayRangeControl(CalibrationPlugin2D calibrationPlugin) {
        this.calibrationPlugin = calibrationPlugin;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        trackRenderer = new ImageViewer2DDisplayRangeControlTrackRenderer(this);
        initializeToolbar();
        slider = new JXMultiThumbSlider<>();
        slider.setPreferredSize(new Dimension(100, 72));
        slider.setMinimumSize(new Dimension(100, 72));
        slider.setOpaque(true);
        slider.setTrackRenderer(trackRenderer);
        slider.setThumbRenderer(new ThumbRenderer());
        DefaultMultiThumbModel<DisplayRangeStop> model = new DefaultMultiThumbModel<>();
        slider.setModel(model);
        model.addThumb(0, DisplayRangeStop.Start);
        model.addThumb(1, DisplayRangeStop.End);
        add(slider, BorderLayout.CENTER);

        slider.addMultiThumbListener(this);
    }

    public double getCurrentMin() {
        return (minSelectableValue + slider.getModel().getThumbAt(0).getPosition() * (maxSelectableValue - minSelectableValue));
    }

    public double getCurrentMax() {
        return (minSelectableValue + slider.getModel().getThumbAt(1).getPosition() * (maxSelectableValue - minSelectableValue));
    }

    public double getMinSelectableValue() {
        return minSelectableValue;
    }

    public double getMaxSelectableValue() {
        return maxSelectableValue;
    }

    private void initializeToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));

//        JButton setMinButton = new JButton("set min");
//        setMinButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
//        setMinButton.addActionListener(e -> {
//            if (calibrationPlugin.getCurrentImagePlus().getImage() != null) {
//                double value = (minSelectableValue + slider.getModel().getThumbAt(0).getPosition() * (maxSelectableValue - minSelectableValue));
//                Optional<Double> newValue = UIUtils.getDoubleByDialog(getCalibrationPlugin().getViewerPanel(),
//                        "Set min display value",
//                        "Please enter the new value:",
//                        value,
//                        minSelectableValue,
//                        maxSelectableValue);
//                if (newValue.isPresent()) {
//                    float position = (float) ((newValue.get() - minSelectableValue) / (maxSelectableValue - minSelectableValue));
//                    slider.getModel().getThumbAt(0).setPosition(Math.max(0, Math.min(position, 1)));
//                    applyCustomCalibration();
//                }
//            }
//        });
//        toolbar.add(Box.createHorizontalStrut(2));
//        toolbar.add(setMinButton);
//
//        JButton setMaxButton = new JButton("set max");
//        setMaxButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
//        setMaxButton.addActionListener(e -> {
//            if (calibrationPlugin.getCurrentImagePlus().getImage() != null) {
//                double value = (minSelectableValue + slider.getModel().getThumbAt(1).getPosition() * (maxSelectableValue - minSelectableValue));
//                Optional<Double> newValue = UIUtils.getDoubleByDialog(getCalibrationPlugin().getViewerPanel(),
//                        "Set max display value",
//                        "Please enter the new value:",
//                        value,
//                        minSelectableValue,
//                        maxSelectableValue);
//                if (newValue.isPresent()) {
//                    float position = (float) ((newValue.get() - minSelectableValue) / (maxSelectableValue - minSelectableValue));
//                    slider.getModel().getThumbAt(1).setPosition(Math.max(0, Math.min(position, 1)));
//                    applyCustomCalibration();
//                }
//            }
//        });
//        toolbar.add(Box.createHorizontalStrut(2));
//        toolbar.add(setMaxButton);

        toolbar.add(Box.createHorizontalGlue());

        JToggleButton logarithmicHistogramToggle = new JToggleButton("log");
        logarithmicHistogramToggle.setBorder(UIUtils.createControlBorder());
        logarithmicHistogramToggle.setSelected(trackRenderer.isLogarithmic());
        logarithmicHistogramToggle.addActionListener(e -> {
            trackRenderer.setLogarithmic(logarithmicHistogramToggle.isSelected());
            repaint();
        });
        toolbar.add(logarithmicHistogramToggle);

        add(toolbar, BorderLayout.NORTH);
    }

    public void updateFromCurrentSlice(boolean clearCustom) {
        ImagePlus currentImage = getCalibrationPlugin().getCurrentImagePlus();

        // Selectable value update
        if (currentImage != null) {
            if (lastSelectableValueCalculationBasis == null || lastSelectableValueCalculationBasis.get() != currentImage) {
                double min;
                double max;
                if (currentImage.getBitDepth() == 32) {
                    // We need to find the min and max
                    if (currentImage.getStackSize() == 1) {
                        ImageStatistics statistics = currentImage.getProcessor().getStats();
                        if (statistics == null)
                            return;
                        min = statistics.min;
                        max = statistics.max;
                    } else {
                        // Initial value
                        {
                            ImageProcessor processor = currentImage.getStack().getProcessor(1);
                            ImageStatistics statistics = processor.getStats();
                            if (statistics == null)
                                return;
                            min = statistics.min;
                            max = statistics.max;
                        }
                        for (int i = 2; i <= currentImage.getStackSize(); i++) {
                            ImageProcessor processor = currentImage.getStack().getProcessor(i);
                            ImageStatistics statistics = processor.getStats();
                            if (statistics == null)
                                continue;
                            min = Math.min(statistics.min, min);
                            max = Math.max(statistics.max, max);
                        }
                    }
                } else {
                    min = currentImage.getProcessor().minValue();
                    max = currentImage.getProcessor().maxValue();
                }
                minSelectableValue = min;
                maxSelectableValue = max;
                lastSelectableValueCalculationBasis = new WeakReference<>(currentImage);
            }
        }

        // Calibration update
        if (clearCustom || mode != ImageJCalibrationMode.Custom) {
            isUpdating = true;
            ImageProcessor currentSlice = getCalibrationPlugin().getCurrentSlice();
            if (currentImage != null && currentSlice != null) {
                if (mode != ImageJCalibrationMode.Custom) {
                    double[] calibration = ImageJUtils.calculateCalibration(currentSlice,
                            mode,
                            minSelectableValue,
                            maxSelectableValue,
                            getCalibrationPlugin().getViewerPanel2D().getCurrentSliceStats().getFastImageStatistics());
                    customMin = calibration[0];
                    customMax = calibration[1];
                }
                double displayRangeMin = customMin;
                double displayRangeMax = customMax;
                double positionMin = Math.min(1, Math.max(0, displayRangeMin - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                double positionMax = Math.min(1, Math.max(0, displayRangeMax - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                slider.getModel().getThumbAt(0).setPosition((float) positionMin);
                slider.getModel().getThumbAt(1).setPosition((float) positionMax);
            }
            isUpdating = false;
        }
        slider.repaint();
    }

    public double getCustomMin() {
        return customMin;
    }

    public double getCustomMax() {
        return customMax;
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        applyCustomCalibration();
        getCalibrationPlugin().uploadSliceToCanvas();
    }

    public void applyCustomCalibration() {
        if (!isUpdating) {
            double min = minSelectableValue;
            double max = maxSelectableValue;
            double diff = max - min;
            float posMin = Float.POSITIVE_INFINITY;
            float posMax = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 2; i++) {
                Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                posMin = Math.min(posMin, thumb.getPosition());
                posMax = Math.max(posMax, thumb.getPosition());
            }
            customMin = min + diff * posMin;
            customMax = min + diff * posMax;
            mode = ImageJCalibrationMode.Custom;
//            calibrationPlugin.disableAutoCalibration();
//            calibrationPlugin.setSelectedCalibration(ImageJCalibrationMode.Custom);
        }
    }

    @Override
    public void thumbSelected(int thumb) {

    }

    @Override
    public void mousePressed(MouseEvent evt) {

    }

    public CalibrationPlugin2D getCalibrationPlugin() {
        return calibrationPlugin;
    }

    public void setCustomMinMax(double displayRangeMin, double displayRangeMax) {
        this.customMin = displayRangeMin;
        this.customMax = displayRangeMax;
        double positionMin = Math.min(1, Math.max(0, displayRangeMin - minSelectableValue) / (maxSelectableValue - minSelectableValue));
        double positionMax = Math.min(1, Math.max(0, displayRangeMax - minSelectableValue) / (maxSelectableValue - minSelectableValue));
        slider.getModel().getThumbAt(0).setPosition((float) positionMin);
        slider.getModel().getThumbAt(1).setPosition((float) positionMax);
        slider.repaint();
    }

    public ImageJCalibrationMode getMode() {
        return mode;
    }

    public void setMode(ImageJCalibrationMode mode) {
        this.mode = mode;
    }

    public enum DisplayRangeStop {
        Start,
        End
    }

    public static class ThumbRenderer extends JComponent implements org.jdesktop.swingx.multislider.ThumbRenderer {

        public static final Polygon SHAPE = new Polygon(new int[]{5, 0, 10}, new int[]{10, 0, 0}, 3);
        public static int SIZE = 5;
        private JXMultiThumbSlider<DisplayRangeStop> slider;
        private int index;

        public ThumbRenderer() {
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            paintComponent(g);
        }

        @Override
        protected void paintComponent(Graphics gfx) {
            Graphics2D g = (Graphics2D) gfx;
            g.setColor(UIManager.getColor("Label.foreground"));
            g.fill(SHAPE);
        }

        @Override
        public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected) {
            this.slider = slider;
            this.index = index;
            return this;
        }
    }

}
