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

package org.hkijena.jipipe.desktop.commons.components.renderers;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class JIPipeDesktopGenericListCellRenderer<T> extends JLabel implements ListCellRenderer<T> {

    private final Function<T, RenderedItem> renderFunction;

    public JIPipeDesktopGenericListCellRenderer(Function<T, RenderedItem> renderFunction) {
        this.renderFunction = renderFunction;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {

        RenderedItem renderedItem = renderFunction.apply(value);
        setText(renderedItem.getText());
        setIcon(renderedItem.getIcon());

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }

    public Function<T, RenderedItem> getRenderFunction() {
        return renderFunction;
    }

    public static class RenderedItem {
        private final Icon icon;
        private final String text;

        public RenderedItem(Icon icon, String text) {
            this.icon = icon;
            this.text = text;
        }

        public Icon getIcon() {
            return icon;
        }

        public String getText() {
            return text;
        }
    }
}
