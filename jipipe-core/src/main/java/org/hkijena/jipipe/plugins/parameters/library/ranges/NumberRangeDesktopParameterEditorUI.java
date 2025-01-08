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

package org.hkijena.jipipe.plugins.parameters.library.ranges;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseEvent;

public class NumberRangeDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI implements ThumbListener {

    private final JTextField minEditor = new JTextField();
    private final JTextField maxEditor = new JTextField();
    private JXMultiThumbSlider<DisplayRangeStop> slider;
    private TrackRenderer trackRenderer;
    private boolean isUpdatingThumbs = false;
    private boolean isUpdatingTextBoxes = false;

    public NumberRangeDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        trackRenderer = new TrackRenderer();

        slider = new JXMultiThumbSlider<>();
//        slider.setPreferredSize(new Dimension(100, 32));
        slider.setMinimumSize(new Dimension(100, 32));
        slider.setOpaque(true);
        slider.setTrackRenderer(trackRenderer);
        slider.setThumbRenderer(new ThumbRenderer());
        DefaultMultiThumbModel<DisplayRangeStop> model = new DefaultMultiThumbModel<>();
        slider.setModel(model);
        model.addThumb(0, DisplayRangeStop.Start);
        model.addThumb(1, DisplayRangeStop.End);
        add(slider, BorderLayout.CENTER);

        add(minEditor, BorderLayout.WEST);
        add(maxEditor, BorderLayout.EAST);

        minEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(minEditor.getText());
                    s = s.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                    if (NumberUtils.isCreatable(s)) {
                        NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
                        double value = NumberUtils.createDouble(s);
                        if (value != parameter.getMinNumber().doubleValue()) {
                            parameter.setMinNumber(value);
                            setParameter(parameter, false);
                            updateThumbs();
                        }
                    }
                }
            }
        });
        maxEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(maxEditor.getText());
                    s = s.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                    if (NumberUtils.isCreatable(s)) {
                        NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
                        double value = NumberUtils.createDouble(s);
                        if (value != parameter.getMaxNumber().doubleValue()) {
                            parameter.setMaxNumber(value);
                            setParameter(parameter, false);
                            updateThumbs();
                        }
                    }
                }
            }
        });

        slider.addMultiThumbListener(this);
    }

    private void updateThumbs() {
        try {
            isUpdatingThumbs = true;
            NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
            slider.getModel().getThumbAt(0).setPosition(parameter.getMinNumber().floatValue());
            slider.getModel().getThumbAt(1).setPosition(parameter.getMaxNumber().floatValue());
        } finally {
            isUpdatingThumbs = false;
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        NumberRangeParameterSettings parameterSettings = getParameterAccess().getAnnotationOfType(NumberRangeParameterSettings.class);
        float min = 0;
        float max = 1;
        if (parameterSettings != null) {
            min = (float) parameterSettings.min();
            max = (float) parameterSettings.max();
            trackRenderer.setTrackBackgroundGenerator((PaintGenerator) ReflectionUtils.newInstance(parameterSettings.trackBackground()));
            trackRenderer.setInvertedMode(parameterSettings.invertedMode());
        }
        slider.getModel().setMinimumValue(min);
        slider.getModel().setMaximumValue(max);
        FontMetrics fontMetrics = minEditor.getFontMetrics(minEditor.getFont());
        int minWidth = Math.max(fontMetrics.stringWidth("" + min), fontMetrics.stringWidth("" + max));
        minEditor.setMinimumSize(new Dimension(minWidth, 16));
        maxEditor.setMinimumSize(new Dimension(minWidth, 16));
        minEditor.setPreferredSize(new Dimension(minWidth, 21));
        maxEditor.setPreferredSize(new Dimension(minWidth, 21));
        updateThumbs();
        updateTextFields();
        revalidate();
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        if (!isUpdatingThumbs) {
            NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
            if (thumb == 0) {
                parameter.setMinNumber(pos);
            } else if (thumb == 1) {
                parameter.setMaxNumber(pos);
            }
            setParameter(parameter, false);
            updateTextFields();
        }
    }

    private void updateTextFields() {
        try {
            isUpdatingTextBoxes = true;
            NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
            minEditor.setText(parameter.getMinNumber() + "");
            maxEditor.setText(parameter.getMaxNumber() + "");
        } finally {
            isUpdatingTextBoxes = false;
        }
    }

    @Override
    public void thumbSelected(int thumb) {

    }

    @Override
    public void mousePressed(MouseEvent evt) {

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

    public static class TrackRenderer extends JComponent implements org.jdesktop.swingx.multislider.TrackRenderer {

        NumberRangeInvertedMode invertedMode = NumberRangeInvertedMode.SwitchMinMax;
        private JXMultiThumbSlider<DisplayRangeStop> slider;
        private boolean logarithmic = true;
        private PaintGenerator trackBackgroundGenerator;

        public TrackRenderer() {
            this.trackBackgroundGenerator = new DefaultTrackBackground();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            paintComponent(g);
        }

        public PaintGenerator getTrackBackgroundGenerator() {
            return trackBackgroundGenerator;
        }

        public void setTrackBackgroundGenerator(PaintGenerator trackBackgroundGenerator) {
            this.trackBackgroundGenerator = trackBackgroundGenerator;
            repaint();
        }

        public NumberRangeInvertedMode getInvertedMode() {
            return invertedMode;
        }

        public void setInvertedMode(NumberRangeInvertedMode invertedMode) {
            this.invertedMode = invertedMode;
        }

        @Override
        protected void paintComponent(Graphics gfx) {
            Graphics2D g = (Graphics2D) gfx;
            int w = slider.getWidth() - 2 * ThumbRenderer.SIZE;
            int h = slider.getHeight();
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, slider.getWidth(), slider.getHeight());
            g.setPaint(trackBackgroundGenerator.generate(ThumbRenderer.SIZE, 0, w, slider.getHeight()));
            g.fillRect(ThumbRenderer.SIZE, ThumbRenderer.SIZE + 2, w, slider.getHeight() - ThumbRenderer.SIZE - 2);
            g.setColor(UIManager.getColor("Button.borderColor"));
            g.drawRect(ThumbRenderer.SIZE, 0, w, h - 1);
            g.drawRect(ThumbRenderer.SIZE, ThumbRenderer.SIZE + 2, w, h - 1 - ThumbRenderer.SIZE - 2);

            float min = slider.getModel().getMinimumValue();
            float max = slider.getModel().getMaximumValue();

            Thumb<DisplayRangeStop> thumb0 = slider.getModel().getThumbAt(0);
            float position0 = (thumb0.getPosition() - min) / (max - min);
            int x0 = ThumbRenderer.SIZE - 1 + (int) (w * position0);

            Thumb<DisplayRangeStop> thumb1 = slider.getModel().getThumbAt(1);
            float position1 = (thumb1.getPosition() - min) / (max - min);
            int x1 = ThumbRenderer.SIZE - 1 + (int) (w * position1);

            // Range indicator
            g.setColor(JIPipeDesktopModernMetalTheme.PRIMARY5);
            switch (invertedMode) {
                case SwitchMinMax:
                    g.fillRect(Math.min(x0, x1), 1, Math.abs(x1 - x0), ThumbRenderer.SIZE + 1);
                    break;
                case OutsideMinMax:
                    if (x0 <= x1) {
                        g.fillRect(x0, 1, Math.abs(x1 - x0), ThumbRenderer.SIZE + 1);
                    } else {
                        int xStart = ThumbRenderer.SIZE;
                        int xEnd = slider.getWidth() - ThumbRenderer.SIZE;
                        g.fillRect(xStart, 1, Math.max(0, x1 - xStart), ThumbRenderer.SIZE + 1);
                        g.fillRect(x0, 1, Math.max(0, xEnd - x0), ThumbRenderer.SIZE + 1);
                    }
                    break;
            }


            // Vertical lines
            g.setColor(UIManager.getColor("Label.foreground"));
            g.fillRect(x0, 4, 2, h + 4);
            g.fillRect(x1, 4, 2, h + 4);
        }

        @Override
        public JComponent getRendererComponent(JXMultiThumbSlider slider) {
            this.slider = slider;
            return this;
        }

        public boolean isLogarithmic() {
            return logarithmic;
        }

        public void setLogarithmic(boolean logarithmic) {
            this.logarithmic = logarithmic;
        }
    }
}
