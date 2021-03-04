package org.hkijena.jipipe.extensions.parameters.ranges;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
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
import java.util.function.Supplier;

public class NumberRangeParameterEditorUI extends JIPipeParameterEditorUI implements ThumbListener {

    private JXMultiThumbSlider<DisplayRangeStop> slider;
    private TrackRenderer trackRenderer;
    private JTextField minEditor = new JTextField();
    private JTextField maxEditor = new JTextField();
    private boolean isUpdatingThumbs = false;
    private boolean isUpdatingTextBoxes = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public NumberRangeParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
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

        minEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if(!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(minEditor.getText());
                    s = s.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                    if(NumberUtils.isCreatable(s)) {
                        NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
                        double value = NumberUtils.createDouble(s);
                        if(value != parameter.getMinNumber().doubleValue()) {
                            parameter.setMinNumber(value);
                            setParameter(parameter, false);
                            updateThumbs();
                        }
                    }
                }
            }
        });
        maxEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if(!isUpdatingTextBoxes) {
                    String s = StringUtils.nullToEmpty(minEditor.getText());
                    s = s.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                    if(NumberUtils.isCreatable(s)) {
                        NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
                        double value = NumberUtils.createDouble(s);
                        if(value != parameter.getMaxNumber().doubleValue()) {
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
        }
        finally {
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
        if(parameterSettings != null) {
            min = (float)parameterSettings.min();
            max = (float)parameterSettings.max();
            trackRenderer.setTrackBackground(((Supplier<Paint>)ReflectionUtils.newInstance(parameterSettings.trackBackground())).get());
        }
        slider.getModel().setMinimumValue(min);
        slider.getModel().setMaximumValue(max);
        updateThumbs();
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        if(!isUpdatingThumbs) {
            NumberRangeParameter parameter = getParameter(NumberRangeParameter.class);
            if(thumb == 0) {
                parameter.setMinNumber(pos);
            }
            else if(thumb == 1) {
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
        }
        finally {
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

        private JXMultiThumbSlider<DisplayRangeStop> slider;
        private boolean logarithmic = true;
        private Paint trackBackground;

        public TrackRenderer() {
            this.trackBackground = UIManager.getColor("Panel.background");
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            paintComponent(g);
        }

        public Paint getTrackBackground() {
            return trackBackground;
        }

        public void setTrackBackground(Paint trackBackground) {
            this.trackBackground = trackBackground;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics gfx) {
            Graphics2D g = (Graphics2D) gfx;
            int w = slider.getWidth() - 2 * ThumbRenderer.SIZE;
            int h = slider.getHeight();
            g.setPaint(trackBackground);
            g.fillRect(0, 0, slider.getWidth(), slider.getHeight());
            g.setColor(UIManager.getColor("Button.borderColor"));
//            g.drawLine(0, h, w + 2 * ThumbRenderer.SIZE, h);
            g.drawRect(0,0,slider.getWidth(),h-1);
            g.setColor(UIManager.getColor("Label.foreground"));

            Thumb<DisplayRangeStop> thumb0 = slider.getModel().getThumbAt(0);
            float position0 = Math.max(0, Math.min(thumb0.getPosition(), 1));
            int x0 = ThumbRenderer.SIZE - 1 + (int) (w * position0);
            g.fillRect(x0, 4, 2, h + 4);

            Thumb<DisplayRangeStop> thumb1 = slider.getModel().getThumbAt(1);
            float position1 = Math.max(0, Math.min(thumb1.getPosition(), 1));
            int x1 = ThumbRenderer.SIZE - 1 + (int) (w * position1);
            g.fillRect(x1, 4, 2, h + 4);

            g.setColor(ModernMetalTheme.PRIMARY5);
            g.drawLine(x0, h - 1, x1, h- 1);
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
