package org.hkijena.jipipe.ui.components.icons;

import javax.swing.*;
import java.awt.Color;

public interface ColorIcon extends Icon {
    Color getFillColor();
    Color getBorderColor();
    void setFillColor(Color c);
    void setBorderColor(Color c);
}
