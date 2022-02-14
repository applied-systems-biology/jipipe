package org.hkijena.jipipe.ui.components.icons;

import javax.swing.*;
import java.awt.*;

public interface ColorIcon extends Icon {
    Color getFillColor();

    void setFillColor(Color c);

    Color getBorderColor();

    void setBorderColor(Color c);
}
