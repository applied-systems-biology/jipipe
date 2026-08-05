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

package org.hkijena.jipipe.desktop.commons.components.cards;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopCard extends JPanel {
    private String title;
    private Icon icon;
    private JComponent body;
    private JIPipeDesktopCardVariant variant = JIPipeDesktopCardVariant.Default;
    private final JToolBar headerToolBar = new JToolBar();
    private final JLabel titleLabel = new JLabel();
    private final JToolBar footerToolBar = new JToolBar();
    private boolean headerVisible = false;
    private boolean footerVisible = false;

    public JIPipeDesktopCard() {
        this(null, null);
    }

    public JIPipeDesktopCard(String title) {
        this(title, null);
    }

    public JIPipeDesktopCard(String title, Icon icon) {
        this.title = title;
        this.icon = icon;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        updateBorder();
        initializeHeader();
        initializeFooter();
        updateHeader();
    }

    private void updateBorder() {
        int radius = ThemeUtils.getCurrentStyle().getIslandsCornerRadius();
        setBorder(new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, radius));
    }

    private void initializeHeader() {
        headerToolBar.setFloatable(false);
        headerToolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));
        headerToolBar.add(titleLabel);
        headerToolBar.add(Box.createHorizontalGlue());
    }

    private void initializeFooter() {
        footerToolBar.setFloatable(false);
        footerToolBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.getControlBorderColor()));
        footerToolBar.add(Box.createHorizontalGlue());
    }

    private void updateHeader() {
        boolean hasTitleOrIcon = title != null || icon != null;
        boolean hasActions = headerToolBar.getComponentCount() > 2;
        headerVisible = hasTitleOrIcon || hasActions;

        if (headerVisible) {
            titleLabel.setText(title != null ? title : "");
            titleLabel.setIcon(icon);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD,
                    ThemeUtils.getCurrentStyle().getFontSizeLarge()));
            titleLabel.setBorder(UIUtils.createEmptyBorder(8));

            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            Color variantColor = variant.resolveColor(style);
            if (variantColor != null) {
                headerToolBar.setBackground(ColorUtils.mix(variantColor,
                        UIManager.getColor("Panel.background"), 0.92));
            } else {
                headerToolBar.setBackground(UIManager.getColor("Panel.background"));
            }

            add(headerToolBar, BorderLayout.NORTH);
        } else {
            remove(headerToolBar);
        }
        revalidate();
        repaint();
    }

    private void updateFooter() {
        footerVisible = footerToolBar.getComponentCount() > 1;
        if (footerVisible) {
            add(footerToolBar, BorderLayout.SOUTH);
        } else {
            remove(footerToolBar);
        }
        revalidate();
        repaint();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        updateHeader();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        updateHeader();
    }

    public JComponent getBody() {
        return body;
    }

    public void setBody(JComponent body) {
        if (this.body != null) {
            remove(this.body);
        }
        this.body = body;
        if (body != null) {
            add(body, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public JIPipeDesktopCardVariant getVariant() {
        return variant;
    }

    public void setVariant(JIPipeDesktopCardVariant variant) {
        this.variant = variant;
        updateHeader();
    }

    public void addHeaderAction(JButton button) {
        headerToolBar.add(button);
        updateHeader();
    }

    public void removeHeaderAction(JButton button) {
        headerToolBar.remove(button);
        updateHeader();
    }

    public int getHeaderActionCount() {
        return Math.max(0, headerToolBar.getComponentCount() - 2);
    }

    public void addFooterAction(JButton button) {
        footerToolBar.add(button);
        updateFooter();
    }

    public void removeFooterAction(JButton button) {
        footerToolBar.remove(button);
        updateFooter();
    }

    public int getFooterActionCount() {
        return Math.max(0, footerToolBar.getComponentCount() - 1);
    }

    public boolean hasHeader() {
        return headerVisible;
    }

    public boolean hasFooter() {
        return footerVisible;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static class Builder {
        private final JIPipeDesktopCard card;

        public Builder(String title) {
            card = new JIPipeDesktopCard(title);
        }

        public Builder icon(Icon icon) {
            card.setIcon(icon);
            return this;
        }

        public Builder variant(JIPipeDesktopCardVariant variant) {
            card.setVariant(variant);
            return this;
        }

        public Builder body(JComponent body) {
            card.setBody(body);
            return this;
        }

        public Builder headerButton(JButton button) {
            card.addHeaderAction(button);
            return this;
        }

        public Builder footerButton(JButton button) {
            card.addFooterAction(button);
            return this;
        }

        public JIPipeDesktopCard build() {
            return card;
        }
    }
}
