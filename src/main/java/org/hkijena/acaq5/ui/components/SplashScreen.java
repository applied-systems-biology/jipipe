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

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.ResourceUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SplashScreen extends JFrame {

    private static SplashScreen instance;

    public SplashScreen() {
        initialize();
    }

    private void initialize() {
        setSize(640, 480);
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setContentPane(contentPane);

        try {
            BufferedImage image = ImageIO.read(ResourceUtils.getPluginResource("splash-screen.png"));
            JLabel label = new JLabel(new ImageIcon(image));
            contentPane.add(label, BorderLayout.CENTER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showSplash() {
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void hideSplash() {
        setVisible(false);
    }

    public static void main(String[] args) {
        getInstance().showSplash();
    }

    public static SplashScreen getInstance() {
        if (instance == null) {
            instance = new SplashScreen();
        }
        return instance;
    }
}
