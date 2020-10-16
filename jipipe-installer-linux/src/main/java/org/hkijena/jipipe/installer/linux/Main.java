package org.hkijena.jipipe.installer.linux;

import org.hkijena.jipipe.installer.linux.ui.MainWindow;
import org.hkijena.jipipe.installer.linux.ui.utils.ModernMetalTheme;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if(Arrays.asList(args).contains("nogui")) {

        }
        else {
            try {
                MetalLookAndFeel.setCurrentTheme(new ModernMetalTheme());
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("swing.boldMetal", Boolean.FALSE);
            } catch (IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            MainWindow window = new MainWindow();
            window.pack();
            window.setSize(1024, 768);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        }
    }
}
