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

package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

/*
 * Hough_Circle.java:
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * @author Ben Smith (benjamin.smith@berkeley.edu)
 * @Based on original plugin implementation by Hemerson Pistori (pistori@ec.ucdb.br) and Eduardo Rocha Costa
 * @created February 4, 2017
 * <p>
 * The Hough Transform implementation was based on
 * Mark A. Schulze applet (http://www.markschulze.net/)
 */

/*
This implementation was updated to allow direct usage from within ImageJ plugins
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import ij.process.LUT;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.gc;

/**
 * @author Ben
 */
public class Hough_Circle extends SwingWorker<Integer, String> {
    private final static int GUI_UPDATE_DELAY = 100; //How long to wait between GUI updates
    //Build LUTs for colorimetric output
    private final static byte[] RAND_R = new byte[]{(byte) 0, (byte) 231, (byte) 25, (byte) 27, (byte) 112, (byte) 89, (byte) 53, (byte) 77, (byte) 155, (byte) 153, (byte) 91, (byte) 19, (byte) 25, (byte) 135, (byte) 187, (byte) 40, (byte) 52, (byte) 39, (byte) 147, (byte) 72, (byte) 92, (byte) 23, (byte) 5, (byte) 202, (byte) 52, (byte) 50, (byte) 108, (byte) 66, (byte) 238, (byte) 46, (byte) 136, (byte) 8, (byte) 162, (byte) 126, (byte) 75, (byte) 60, (byte) 135, (byte) 167, (byte) 136, (byte) 244, (byte) 161, (byte) 204, (byte) 158, (byte) 112, (byte) 192, (byte) 2, (byte) 98, (byte) 207, (byte) 233, (byte) 121, (byte) 191, (byte) 18, (byte) 210, (byte) 151, (byte) 252, (byte) 155, (byte) 139, (byte) 42, (byte) 188, (byte) 170, (byte) 127, (byte) 151, (byte) 38, (byte) 16, (byte) 227, (byte) 120, (byte) 137, (byte) 2, (byte) 72, (byte) 71, (byte) 214, (byte) 130, (byte) 194, (byte) 96, (byte) 102, (byte) 33, (byte) 92, (byte) 47, (byte) 211, (byte) 226, (byte) 237, (byte) 132, (byte) 244, (byte) 45, (byte) 82, (byte) 73, (byte) 239, (byte) 198, (byte) 118, (byte) 191, (byte) 170, (byte) 238, (byte) 177, (byte) 246, (byte) 66, (byte) 5, (byte) 44, (byte) 3, (byte) 18, (byte) 40, (byte) 226, (byte) 94, (byte) 130, (byte) 216, (byte) 68, (byte) 89, (byte) 150, (byte) 40, (byte) 96, (byte) 155, (byte) 95, (byte) 5, (byte) 132, (byte) 88, (byte) 124, (byte) 241, (byte) 143, (byte) 15, (byte) 1, (byte) 220, (byte) 46, (byte) 220, (byte) 216, (byte) 155, (byte) 190, (byte) 4, (byte) 98, (byte) 33, (byte) 142, (byte) 153, (byte) 185, (byte) 61, (byte) 153, (byte) 160, (byte) 164, (byte) 102, (byte) 75, (byte) 8, (byte) 26, (byte) 231, (byte) 160, (byte) 245, (byte) 119, (byte) 69, (byte) 95, (byte) 137, (byte) 232, (byte) 249, (byte) 89, (byte) 220, (byte) 54, (byte) 3, (byte) 43, (byte) 134, (byte) 172, (byte) 31, (byte) 106, (byte) 234, (byte) 132, (byte) 1, (byte) 29, (byte) 65, (byte) 12, (byte) 107, (byte) 52, (byte) 196, (byte) 95, (byte) 165, (byte) 4, (byte) 101, (byte) 124, (byte) 240, (byte) 203, (byte) 50, (byte) 142, (byte) 231, (byte) 5, (byte) 23, (byte) 43, (byte) 198, (byte) 41, (byte) 80, (byte) 66, (byte) 232, (byte) 86, (byte) 151, (byte) 126, (byte) 51, (byte) 122, (byte) 111, (byte) 162, (byte) 192, (byte) 148, (byte) 210, (byte) 5, (byte) 251, (byte) 40, (byte) 176, (byte) 32, (byte) 2, (byte) 22, (byte) 91, (byte) 180, (byte) 31, (byte) 218, (byte) 9, (byte) 10, (byte) 178, (byte) 27, (byte) 115, (byte) 54, (byte) 199, (byte) 163, (byte) 10, (byte) 180, (byte) 222, (byte) 102, (byte) 198, (byte) 202, (byte) 220, (byte) 96, (byte) 145, (byte) 237, (byte) 127, (byte) 172, (byte) 51, (byte) 64, (byte) 47, (byte) 84, (byte) 37, (byte) 196, (byte) 147, (byte) 244, (byte) 206, (byte) 244, (byte) 43, (byte) 170, (byte) 186, (byte) 162, (byte) 157, (byte) 125, (byte) 230, (byte) 212, (byte) 140, (byte) 186, (byte) 127, (byte) 12, (byte) 48, (byte) 0, (byte) 92, (byte) 168, (byte) 73, (byte) 228, (byte) 121, (byte) 248, (byte) 255};
    private final static byte[] RAND_G = new byte[]{(byte) 0, (byte) 217, (byte) 131, (byte) 18, (byte) 66, (byte) 198, (byte) 59, (byte) 170, (byte) 108, (byte) 8, (byte) 167, (byte) 207, (byte) 248, (byte) 138, (byte) 215, (byte) 204, (byte) 95, (byte) 249, (byte) 102, (byte) 66, (byte) 1, (byte) 138, (byte) 71, (byte) 33, (byte) 209, (byte) 80, (byte) 72, (byte) 87, (byte) 129, (byte) 24, (byte) 200, (byte) 98, (byte) 16, (byte) 160, (byte) 47, (byte) 1, (byte) 224, (byte) 49, (byte) 126, (byte) 41, (byte) 235, (byte) 205, (byte) 84, (byte) 249, (byte) 222, (byte) 125, (byte) 5, (byte) 58, (byte) 90, (byte) 137, (byte) 73, (byte) 98, (byte) 109, (byte) 167, (byte) 91, (byte) 106, (byte) 68, (byte) 23, (byte) 10, (byte) 241, (byte) 51, (byte) 35, (byte) 77, (byte) 112, (byte) 243, (byte) 235, (byte) 53, (byte) 41, (byte) 21, (byte) 134, (byte) 106, (byte) 60, (byte) 231, (byte) 107, (byte) 124, (byte) 96, (byte) 131, (byte) 62, (byte) 213, (byte) 220, (byte) 78, (byte) 96, (byte) 44, (byte) 106, (byte) 128, (byte) 96, (byte) 129, (byte) 164, (byte) 59, (byte) 86, (byte) 45, (byte) 87, (byte) 237, (byte) 192, (byte) 171, (byte) 93, (byte) 223, (byte) 206, (byte) 158, (byte) 42, (byte) 255, (byte) 175, (byte) 142, (byte) 177, (byte) 48, (byte) 67, (byte) 221, (byte) 123, (byte) 125, (byte) 182, (byte) 135, (byte) 31, (byte) 163, (byte) 211, (byte) 220, (byte) 28, (byte) 81, (byte) 113, (byte) 235, (byte) 236, (byte) 30, (byte) 104, (byte) 238, (byte) 227, (byte) 18, (byte) 134, (byte) 193, (byte) 15, (byte) 122, (byte) 151, (byte) 6, (byte) 132, (byte) 0, (byte) 128, (byte) 88, (byte) 20, (byte) 130, (byte) 87, (byte) 127, (byte) 55, (byte) 13, (byte) 76, (byte) 15, (byte) 124, (byte) 251, (byte) 160, (byte) 177, (byte) 179, (byte) 14, (byte) 7, (byte) 189, (byte) 49, (byte) 210, (byte) 77, (byte) 63, (byte) 132, (byte) 17, (byte) 84, (byte) 214, (byte) 63, (byte) 206, (byte) 252, (byte) 184, (byte) 168, (byte) 109, (byte) 129, (byte) 89, (byte) 70, (byte) 66, (byte) 151, (byte) 151, (byte) 100, (byte) 27, (byte) 123, (byte) 35, (byte) 210, (byte) 112, (byte) 183, (byte) 135, (byte) 97, (byte) 19, (byte) 7, (byte) 159, (byte) 221, (byte) 46, (byte) 253, (byte) 42, (byte) 229, (byte) 101, (byte) 187, (byte) 10, (byte) 33, (byte) 152, (byte) 138, (byte) 236, (byte) 208, (byte) 117, (byte) 230, (byte) 98, (byte) 200, (byte) 139, (byte) 200, (byte) 255, (byte) 19, (byte) 219, (byte) 183, (byte) 181, (byte) 3, (byte) 43, (byte) 143, (byte) 130, (byte) 75, (byte) 21, (byte) 228, (byte) 121, (byte) 208, (byte) 51, (byte) 82, (byte) 41, (byte) 233, (byte) 56, (byte) 220, (byte) 190, (byte) 176, (byte) 136, (byte) 108, (byte) 88, (byte) 118, (byte) 228, (byte) 206, (byte) 11, (byte) 170, (byte) 236, (byte) 232, (byte) 204, (byte) 241, (byte) 241, (byte) 69, (byte) 71, (byte) 28, (byte) 143, (byte) 207, (byte) 52, (byte) 188, (byte) 183, (byte) 80, (byte) 4, (byte) 222, (byte) 162, (byte) 30, (byte) 213, (byte) 228, (byte) 119, (byte) 142, (byte) 10, (byte) 255};
    private final static byte[] RAND_B = new byte[]{(byte) 0, (byte) 43, (byte) 203, (byte) 219, (byte) 253, (byte) 158, (byte) 5, (byte) 190, (byte) 72, (byte) 11, (byte) 23, (byte) 233, (byte) 220, (byte) 246, (byte) 53, (byte) 10, (byte) 90, (byte) 49, (byte) 215, (byte) 182, (byte) 74, (byte) 50, (byte) 27, (byte) 107, (byte) 39, (byte) 48, (byte) 192, (byte) 134, (byte) 247, (byte) 89, (byte) 14, (byte) 3, (byte) 67, (byte) 65, (byte) 30, (byte) 136, (byte) 78, (byte) 129, (byte) 178, (byte) 138, (byte) 186, (byte) 204, (byte) 5, (byte) 160, (byte) 18, (byte) 103, (byte) 255, (byte) 162, (byte) 42, (byte) 128, (byte) 213, (byte) 204, (byte) 49, (byte) 80, (byte) 181, (byte) 130, (byte) 60, (byte) 185, (byte) 31, (byte) 203, (byte) 184, (byte) 89, (byte) 108, (byte) 190, (byte) 109, (byte) 157, (byte) 231, (byte) 62, (byte) 128, (byte) 96, (byte) 150, (byte) 153, (byte) 160, (byte) 54, (byte) 24, (byte) 143, (byte) 90, (byte) 216, (byte) 128, (byte) 120, (byte) 87, (byte) 244, (byte) 15, (byte) 213, (byte) 235, (byte) 142, (byte) 140, (byte) 33, (byte) 98, (byte) 164, (byte) 202, (byte) 38, (byte) 84, (byte) 63, (byte) 229, (byte) 163, (byte) 28, (byte) 239, (byte) 210, (byte) 131, (byte) 79, (byte) 83, (byte) 168, (byte) 79, (byte) 89, (byte) 170, (byte) 26, (byte) 168, (byte) 149, (byte) 217, (byte) 31, (byte) 20, (byte) 11, (byte) 141, (byte) 121, (byte) 139, (byte) 21, (byte) 58, (byte) 194, (byte) 75, (byte) 110, (byte) 234, (byte) 16, (byte) 126, (byte) 24, (byte) 41, (byte) 62, (byte) 88, (byte) 232, (byte) 38, (byte) 243, (byte) 83, (byte) 195, (byte) 58, (byte) 84, (byte) 106, (byte) 151, (byte) 32, (byte) 146, (byte) 24, (byte) 140, (byte) 217, (byte) 176, (byte) 186, (byte) 174, (byte) 170, (byte) 250, (byte) 1, (byte) 48, (byte) 141, (byte) 99, (byte) 213, (byte) 127, (byte) 46, (byte) 97, (byte) 12, (byte) 143, (byte) 237, (byte) 153, (byte) 185, (byte) 72, (byte) 170, (byte) 23, (byte) 222, (byte) 216, (byte) 23, (byte) 92, (byte) 232, (byte) 54, (byte) 64, (byte) 72, (byte) 128, (byte) 182, (byte) 38, (byte) 192, (byte) 138, (byte) 115, (byte) 162, (byte) 231, (byte) 2, (byte) 143, (byte) 117, (byte) 167, (byte) 196, (byte) 4, (byte) 182, (byte) 220, (byte) 52, (byte) 252, (byte) 34, (byte) 2, (byte) 233, (byte) 132, (byte) 135, (byte) 230, (byte) 36, (byte) 199, (byte) 228, (byte) 222, (byte) 43, (byte) 119, (byte) 7, (byte) 148, (byte) 59, (byte) 10, (byte) 35, (byte) 158, (byte) 237, (byte) 17, (byte) 116, (byte) 110, (byte) 129, (byte) 172, (byte) 233, (byte) 172, (byte) 47, (byte) 114, (byte) 26, (byte) 115, (byte) 212, (byte) 177, (byte) 157, (byte) 74, (byte) 183, (byte) 102, (byte) 132, (byte) 151, (byte) 18, (byte) 242, (byte) 242, (byte) 12, (byte) 138, (byte) 21, (byte) 159, (byte) 165, (byte) 213, (byte) 230, (byte) 147, (byte) 174, (byte) 206, (byte) 116, (byte) 9, (byte) 242, (byte) 233, (byte) 202, (byte) 205, (byte) 116, (byte) 169, (byte) 80, (byte) 53, (byte) 161, (byte) 239, (byte) 211, (byte) 73, (byte) 96, (byte) 255};
    private final static LUT RAND_LUT = new LUT(RAND_R, RAND_G, RAND_B);
    private final static byte[] GYR_R = new byte[]{(byte) 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 253, (byte) 251, (byte) 249, (byte) 247, (byte) 245, (byte) 243, (byte) 241, (byte) 239, (byte) 237, (byte) 235, (byte) 233, (byte) 231, (byte) 229, (byte) 227, (byte) 225, (byte) 223, (byte) 221, (byte) 219, (byte) 217, (byte) 215, (byte) 213, (byte) 211, (byte) 209, (byte) 207, (byte) 205, (byte) 203, (byte) 201, (byte) 199, (byte) 197, (byte) 195, (byte) 193, (byte) 191, (byte) 189, (byte) 187, (byte) 185, (byte) 183, (byte) 181, (byte) 179, (byte) 177, (byte) 175, (byte) 173, (byte) 171, (byte) 169, (byte) 167, (byte) 165, (byte) 163, (byte) 161, (byte) 159, (byte) 157, (byte) 155, (byte) 153, (byte) 151, (byte) 149, (byte) 147, (byte) 145, (byte) 143, (byte) 141, (byte) 139, (byte) 137, (byte) 135, (byte) 133, (byte) 131, (byte) 129, (byte) 127, (byte) 125, (byte) 123, (byte) 121, (byte) 119, (byte) 117, (byte) 115, (byte) 113, (byte) 111, (byte) 109, (byte) 107, (byte) 105, (byte) 103, (byte) 101, (byte) 99, (byte) 97, (byte) 95, (byte) 93, (byte) 91, (byte) 89, (byte) 87, (byte) 85, (byte) 83, (byte) 81, (byte) 79, (byte) 77, (byte) 75, (byte) 73, (byte) 71, (byte) 69, (byte) 67, (byte) 65, (byte) 63, (byte) 61, (byte) 59, (byte) 57, (byte) 55, (byte) 53, (byte) 51, (byte) 49, (byte) 47, (byte) 45, (byte) 43, (byte) 41, (byte) 39, (byte) 37, (byte) 35, (byte) 33, (byte) 31, (byte) 29, (byte) 27, (byte) 25, (byte) 23, (byte) 21, (byte) 19, (byte) 17, (byte) 15, (byte) 13, (byte) 11, (byte) 9, (byte) 7, (byte) 5, (byte) 3, (byte) 1};
    private final static byte[] GYR_G = new byte[]{(byte) 0, (byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9, (byte) 11, (byte) 13, (byte) 15, (byte) 17, (byte) 19, (byte) 21, (byte) 23, (byte) 25, (byte) 27, (byte) 29, (byte) 31, (byte) 33, (byte) 35, (byte) 37, (byte) 39, (byte) 41, (byte) 43, (byte) 45, (byte) 47, (byte) 49, (byte) 51, (byte) 53, (byte) 55, (byte) 57, (byte) 59, (byte) 61, (byte) 63, (byte) 65, (byte) 67, (byte) 69, (byte) 71, (byte) 73, (byte) 75, (byte) 77, (byte) 79, (byte) 81, (byte) 83, (byte) 85, (byte) 87, (byte) 89, (byte) 91, (byte) 93, (byte) 95, (byte) 97, (byte) 99, (byte) 101, (byte) 103, (byte) 105, (byte) 107, (byte) 109, (byte) 111, (byte) 113, (byte) 115, (byte) 117, (byte) 119, (byte) 121, (byte) 123, (byte) 125, (byte) 127, (byte) 129, (byte) 131, (byte) 133, (byte) 135, (byte) 137, (byte) 139, (byte) 141, (byte) 143, (byte) 145, (byte) 147, (byte) 149, (byte) 151, (byte) 153, (byte) 155, (byte) 157, (byte) 159, (byte) 161, (byte) 163, (byte) 165, (byte) 167, (byte) 169, (byte) 171, (byte) 173, (byte) 175, (byte) 177, (byte) 179, (byte) 181, (byte) 183, (byte) 185, (byte) 187, (byte) 189, (byte) 191, (byte) 193, (byte) 195, (byte) 197, (byte) 199, (byte) 201, (byte) 203, (byte) 205, (byte) 207, (byte) 209, (byte) 211, (byte) 213, (byte) 215, (byte) 217, (byte) 219, (byte) 221, (byte) 223, (byte) 225, (byte) 227, (byte) 229, (byte) 231, (byte) 233, (byte) 235, (byte) 237, (byte) 239, (byte) 241, (byte) 243, (byte) 245, (byte) 247, (byte) 249, (byte) 251, (byte) 253, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private final static byte[] GYR_B = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final static LUT GYR_LUT = new LUT(GYR_R, GYR_G, GYR_B);
    //Input parameters
    private int radiusMin;  // Find circles with radius grater or equal radiusMin - argument syntax: "min=#"
    private int radiusMax;  // Find circles with radius less or equal radiusMax - argument syntax: "max=#"
    private int radiusInc;  // Increment used to go from radiusMin to radiusMax - argument syntax: "inc=#"
    private int minCircles; // Minumum number of circles to be found - argument syntax: "minCircles=#"
    private int maxCircles; // Maximum number of circles to be found - argument syntax: "maxCircles=#"
    private int threshold = -1; // An alternative to maxCircles. All circles with a value in the hough space greater then threshold are marked. Higher thresholds result in fewer circles. - argument syntax: "threshold=#"
    private double thresholdRatio; //Ratio input from GUI that expresses threshold as ratio of resolution (highest possible # of votes)
    private int resolution; //The number of steps to use per transform (i.e. number of voting rounds)
    private double ratio; // Ratio of found circle radius to clear out surrounding neighbors
    private int searchBand = 0; //The +/- range of radii to search for relative to the last found radius - argument syntax: "bandwidth=#"
    private int searchRadius = 0; //The search radius to look for the next centroid relative to the last found centroid - argument syntax: "radius=#"
    private boolean reduce = false; //Cap the transform resolution by removeing redundant steps
    private boolean local = false; //Whether or not the search is going to be local
    //Output parameters
    private boolean houghSeries = false; //Contains whether the user wants the Hough series stack as an output - argument syntax: "show_raw"
    private boolean showCircles = false; //Contains whether the user wants the circles found as an output - argument syntax: "show_mask"
    private boolean showID = false; //Contains whether the user wants a map of centroids and radii outputed from search - argument syntax: "show_centroids"
    private boolean showScores = false; //Contains whether the user wants a map of centroids and Hough scores outputed from search - argument syntax: "show_scores"
    private boolean results = false; //Contains whether the user wants to export the measurements to a reuslts table
    private Calibration pixelCal;
    private String pixelUnits; //Stores the unit of measurement fo the pixels
    private double pixelDimensions; //Stores the size of each pixel
    private int[] imageDimensions; //Pixel dimensions of stack
    private String timeUnits; //Frame units
    private double timeDimension; //Time step per frame
    private String currentStatus = ""; //String for outputting current status
    private boolean isGUI; //Whether a GUI is active (or a macro called the plugin)
    private boolean cancelThread = false;
    //Hough transform variables
    private ImagePlus imp; //Initalize the variable to hold the image
    private boolean isStack = false; //True if there is more than one slice in the input data
    private int stackSlices; //number of slices in the stack
    private int maxHough; //Contains the brights pixel in the entire Hough array
    private Point maxPoint; //Stores the location of the brightest pixel in the Hough array
    private int maxRadius; //Stores the radius of the brightest pixel in the Hough array
    private Rectangle r; //Stores the ROI on the original image
    private float imageValues[]; // Raw image (returned by ip.getPixels()) - float is used to allow 8, 16 or 32 bit images
    private int houghValues[][][]; // Hough Space Values [X coord][Y coord][radius index]
    private int localHoughValues[][][][]; //Local Hough space [circle#][X coord][Y coord][radius index]
    private int localHoughParameters[][]; //Array to pass local Hough space parameters to centroid search [circle#][parameter vector]
    private int width; // ROI width
    private int height;  // ROI height
    private int depth;  // Number of slices
    private int fullWidth; // Image Width
    private int fullHeight; //Image Height
    private int offx;   // ROI x origin position
    private int offy;   // ROI y origin position
    private Point centerPoint[]; // Center Points of the Circles Found.
    private int centerRadii[]; //Corresponding radii of the cricles marked by the center points
    private int houghScores[]; //Corresponding Hough scores for each centroid
    private int circleID[]; //Corresponding ID # for each centroid
    private int circleIDcounter; //Counter for keeping track of current max ID #
    private int lut[][][]; // LookUp Table for x and y tranform shifts in an octahedral manner
    private int lutSize; //Stores the actual number of transforms performed (<=selected resolution)
    private int nCircles; //Number of circles found during search - <= maxCircles
    private int nCirlcesPrev; //Stores nCirlces from last iteration
    private boolean localSearch = false; //Record whether a local-only search was done for the frame
    //Variables for storing the results and exporting result images
    private ImagePlus houghPlus;
    private ImageStack houghStack;
    private ImagePlus circlePlus;
    private ImageStack circleStack;
    private ImagePlus idPlus;
    private ImageStack idStack;
    private ImagePlus scorePlus;
    private ImageStack scoreStack;
    private ImageProcessor circlesip;
    private ImageProcessor IDip;
    private ImageProcessor scoresip;
    private ResultsTable rt;
    private String method;
    //Variables for max Hough score search
    private int maxHoughArray[][]; //Matrix to store hough scores, radii, and points from multi-threaded max search
    private int ithread;
    //private int totalTime = 0; //Variable to test beenfits of multithreading
    // </editor-fold>

    /**
     * Start all given threads and wait on each of them until all are done.
     * From Stephan Preibisch's Multithreading.java class. See:
     * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
     *
     * @param threads
     */
    public static void startAndJoin(Thread[] threads) {
        for (int ithread = 0; ithread < threads.length; ++ithread) {
            threads[ithread].setPriority(Thread.NORM_PRIORITY);
            threads[ithread].start();
        }

        try {
            for (int ithread = 0; ithread < threads.length; ++ithread)
                threads[ithread].join();
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    /**
     * Import values from GUI class before starting the analysis thread
     *
     * @param radiusMin
     * @param radiusMax
     * @param radiusInc
     * @param minCircles
     * @param maxCircles
     * @param thresholdRatio
     * @param resolution
     * @param ratio
     * @param searchBand
     * @param searchRadius
     * @param reduce
     * @param local
     * @param houghSeries
     * @param showCircles
     * @param showID
     * @param showScores
     * @param results
     * @param isGUI
     */
    public void setParameters(int radiusMin, int radiusMax, int radiusInc, int minCircles, int maxCircles, double thresholdRatio, int resolution, double ratio, int searchBand,
                              int searchRadius, boolean reduce, boolean local, boolean houghSeries, boolean showCircles, boolean showID, boolean showScores, boolean results, boolean isGUI) {

        this.radiusMin = radiusMin;
        this.radiusMax = radiusMax;
        this.radiusInc = radiusInc;
        this.minCircles = minCircles;
        this.maxCircles = maxCircles;
        this.thresholdRatio = thresholdRatio;
        this.resolution = resolution;
        this.ratio = ratio;
        this.searchBand = searchBand;
        this.searchRadius = searchRadius;
        this.reduce = reduce;
        this.local = local;
        this.houghSeries = houghSeries;
        this.showCircles = showCircles;
        this.showID = showID;
        this.showScores = showScores;
        this.results = results;
        this.isGUI = isGUI;
    }

    @Override
    //Start the Hough transform on a separate thread from the GUI
    protected Integer doInBackground() throws Exception {
        startTransform();
        return 100;
    }

    public void startTransform() {
        //Initialize the results table
        rt = Analyzer.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }

        //Initialize variables
        nCircles = 0;
        circleIDcounter = 0;

        //Calculate Hough parameters
        depth = ((radiusMax - radiusMin) / radiusInc) + 1;

        //If radiusInc is not a divisor of radiusMax-radiusMin, return error
        if ((radiusMax - radiusMin) % radiusInc != 0) {
            IJ.showMessage("Error: Radius increment must be a divisor of maximum radius - minimum radius.");
            IJ.showMessage("radiusMin=" + radiusMin + ", radiusMax=" + radiusMax + ", radiusInc=" + radiusInc);
            return;
        }

        //Build the transform LUT (all necessary translations in Cartesian coordinates)
        //NOTE: This step must precede the calculatation of the threshold, as it can change the resolution from the original input value
        lutSize = buildLookUpTable();

        //Calculate the threshold based off the the actual resolution
        threshold = (int) Math.round(thresholdRatio * resolution);

        //If the threshold rounds to 0, set threshold to 1 as 0 is nonsensical
        if (threshold <= 0) {
            threshold = 1;
        }
        // <editor-fold desc="Send arguments to record">
        //If the macro was being recorded, return the set values to the recorder
        if (Recorder.record) {
            String Command = "run(\"Hough Circle Transform\",\"minRadius=" + radiusMin + ", maxRadius=" + radiusMax + ", inc=" + radiusInc +
                    ", minCircles=" + minCircles + ", maxCircles=" + maxCircles + ", threshold=" + thresholdRatio + ", resolution=" + resolution +
                    ", ratio=" + ratio + ", bandwidth=" + searchBand + ", local_radius=" + searchRadius + ", ";
            if (reduce) Command += " reduce";
            if (local) Command += " local_search";
            if (houghSeries) Command += " show_raw";
            if (showCircles) Command += " show_mask";
            if (showID) Command += " show_centroids";
            if (showScores) Command += " show_scores";
            if (results) Command += " results_table";
            Command += "\");\r\n";
            Recorder.recordString(Command);
        }
        // </editor-fold>

        //Create an ImagePlus instance of the currently active image
        imp = WindowManager.getCurrentImage();

        //Import the ImagePlus as a stack
        ImageStack stack = imp.getStack();

        //Get pixel dimensions and units
        pixelCal = imp.getCalibration();
        pixelUnits = pixelCal.getUnits();
        pixelDimensions = pixelCal.pixelWidth;
        imageDimensions = imp.getDimensions(); //(width, height, nChannels, nSlices, nFrames)

        //If the stack has frames, get the time units and step size
        if (imageDimensions[2] == 1 & imageDimensions[3] == 1 & imageDimensions[4] > 1) {
            timeUnits = pixelCal.getTimeUnit();
            timeDimension = pixelCal.frameInterval;

            //If the time dimension is zero, then use slice # instead
            if (timeDimension == 0) {
                timeUnits = "slice #";
                timeDimension = 1;
            }

            //Put frames into slice dimension to keep things constant
            imp.setDimensions(1, imageDimensions[4], 1);
        }

        //If the stack has Z-steps then use slice # as units
        else if (imageDimensions[2] == 1 & imageDimensions[3] >= 1 & imageDimensions[4] == 1) {
            timeUnits = "slice #";
            timeDimension = 1;
        }

        //If the stack is a hyper-stack, then abort analysis
        else {
            IJ.showMessage("Error: This stack is a hyperstack.  Please convert to stack before processing.");
            IJ.showMessage("nChannels=" + imageDimensions[2] + ", nSlices=" + imageDimensions[3] + ", nFrames=" + imageDimensions[4]);
            return;
        }

        //Get the ROI dimensions - no ROI = full image
        r = stack.getRoi();
        offx = r.x;
        offy = r.y;
        width = r.width;
        height = r.height;
        fullWidth = stack.getWidth();
        fullHeight = stack.getHeight();

        //Convert the stack to float (allows 8, 16, and 32 stacks to all be processed as one type)
        ImageStack stackCopy = stack.duplicate();
        ImageStack floatStack = stackCopy.convertToFloat();

        if (houghSeries) {
            //Frames is transform dimension
            houghStack = new ImageStack(width, height, depth * imp.getNSlices());
        }
        if (showCircles) {
            circleStack = new ImageStack(width, height, imp.getNSlices());
        }
        if (showID) {
            idStack = new ImageStack(width, height, imp.getNSlices());
        }
        if (showScores) {
            scoreStack = new ImageStack(width, height, imp.getNSlices());
        }

        //See if input is stack
        stackSlices = imp.getStackSize();
        if (stackSlices > 1) isStack = true;

        //Build arrays for storing the circle result parameters
        houghScores = new int[maxCircles];
        centerRadii = new int[maxCircles];
        centerPoint = new Point[maxCircles];
        circleID = new int[maxCircles];
        Arrays.fill(circleID, -1); //Flag indeces as -1 = no circle found for this index
        Arrays.fill(centerRadii, -1);
        Arrays.fill(centerPoint, new Point(-1, -1));
        Arrays.fill(houghScores, -1);

        //Start timer to delay log outputs
        long startTime = System.currentTimeMillis();

        //Retrieve the current slice in the stack as an ImageProcessor
        for (int slice = 1; slice <= stackSlices; slice++) {
            //Store and then reset nCircles
            nCirlcesPrev = nCircles;
            nCircles = 0;

            //Set houghMaximum to -1 so that it can be determined whether the maximum has already been found
            maxHough = -1;

            //Initialize local search to false to record whether a local search was performed
            localSearch = false;

            //Show tranform status if there is more than one slice
            if (isStack) {
                if ((System.currentTimeMillis() - startTime) > GUI_UPDATE_DELAY) {
                    if (isGUI) {
                        //Update GUI progress bar
                        publish("Processing frame: " + slice + " of " + stackSlices + "");
                        setProgress(Math.round(100 * slice / stackSlices));
                    }

                    //Update IJ status bar
                    IJ.showStatus("Hough tranform - processing frame: " + slice + " of " + stackSlices + "");
                    IJ.showProgress(slice, stackSlices);

                    //Reset timer
                    startTime = System.currentTimeMillis();
                }
            }

            ImageProcessor ip = floatStack.getProcessor(slice);
            imageValues = (float[]) ip.getPixels();

            // <editor-fold desc="Local search">
            //If the number of circles is greater than min circles - speed up the search by looking locally for each next circle
            //Multithread by giving each core a subset of the total number of circles
            if (local & slice > 1) {
                //If there are still a sufficient number of circles, perform an exclusively local search
                if (nCirlcesPrev >= minCircles) {
                    method = "Local";

                    //Set local search to true
                    localSearch = true;
                    startLocalTransform();

                    //Rebuild the local searches into a full transform if the Hough Series is desired
                    if (houghSeries) {
                        convertLocaltoFullHough(slice, houghStack);

                    }
                }

                //Otherwise, if there still are valid circles, perform the full Hough transform, but perform a local search for the valid circles first before continuing onto a full search
                else if (nCirlcesPrev > 0) {
                    method = "Partial Local";
                    houghTransform();
                    if (houghSeries) {
                        //Create the hyperstach to put into the result if needed
                        HoughSpaceSeries(slice, houghStack);
                    }
                    startPartialLocalSearch();
                    getCenterPoints();
                }
                //If there are no circles, and minCircles is 0, then return blank results
                else if (nCirlcesPrev == 0 & minCircles == 0) {
                    method = "N/A";
                    //Create an empty Hough array
                    houghValues = new int[width][height][depth];
                    if (houghSeries) {
                        //Create the hyperstach to put into the result if needed
                        HoughSpaceSeries(slice, houghStack);
                    }

                    //Set maxHough to an arbitrary value to flag that there is no need to find this value
                    maxHough = 1;
                }

                //If no circles are found, then revert to a full Hough
                else {
                    method = "Full";
                    houghTransform();

                    if (houghSeries) {
                        //Create the hyperstach to put into the result if needed
                        HoughSpaceSeries(slice, houghStack);
                    }
                    // Mark the center of the found circles in a new image(always do this as center points are necessary for local search)
                    getCenterPoints();
                }
            }
            //Otherwise, perform the full transform
            else {
                method = "Full";
                houghTransform();
                if (houghSeries) {
                    //Create the hyperstach to put into the result if needed
                    HoughSpaceSeries(slice, houghStack);
                }
                // Mark the center of the found circles in a new image if user wants to find centers
                if (showCircles || showID || showScores || results || local) getCenterPoints();

            }
            // </editor-fold>

            // Create image View for Marked Circles.
            if (showCircles) drawCircles(slice, circleStack, width, height, offx, offy, fullWidth);

            //Create map of centroids where the intensity is the radius
            if (showID || showScores) drawFilledCircles(slice, idStack, scoreStack);

            //Export measurements to the results table
            if (results) resultsTable(slice);

        }
//startTestTime = System.currentTimeMillis();
//totalTime += System.currentTimeMillis()-startTestTime;
//IJ.log("" + totalTime);
        //Draw the resulting stacks
        if (houghSeries) {
            houghPlus = new ImagePlus("Hough Transform Series", houghStack);
            if (depth > 1)
                houghPlus = HyperStackConverter.toHyperStack(houghPlus, 1, depth, imp.getNSlices(), "default", "grayscale");
//            houghPlus.show();
        }
        if (showCircles) {
            ImagePlus Circle_Map = new ImagePlus("Centroid overlay", circleStack);
//            Circle_Map.show();

            //If orignal stack was movie, convert this stack to movie
            if (imageDimensions[4] > 1) {
                Circle_Map.setDimensions(1, 1, imageDimensions[4]);
            }
            Circle_Map.setCalibration(pixelCal);
        }
        if (showID) {
            ImagePlus ID_Map = new ImagePlus("Centroid map", idStack);
//            ID_Map.show();
            ID_Map.setLut(RAND_LUT);
            ID_Map.setDisplayRange(0, circleIDcounter);

            //If orignal stack was movie, convert this stack to movie
            if (imageDimensions[4] > 1) {
                ID_Map.setDimensions(1, 1, imageDimensions[4]);
            }
            ID_Map.setCalibration(pixelCal);
        }
        if (showScores) {
            ImagePlus Score_Map = new ImagePlus("Score map", scoreStack);
//            Score_Map.show();
            Score_Map.setLut(GYR_LUT);
            Score_Map.setDisplayRange(thresholdRatio, 1D);

            //If orignal stack was movie, convert this stack to movie
            if (imageDimensions[4] > 1) {
                Score_Map.setDimensions(1, 1, imageDimensions[4]);
            }
            Score_Map.setCalibration(pixelCal);
        }
//        if(results) rt.show("Results");

        //Put slices back into frame dimension if necessary
        if (imageDimensions[4] > 1) {
            imp.setDimensions(1, 1, imageDimensions[4]);
        }
        clearArrays();
        IJ.showProgress(0);
    }

    private void clearArrays() {
        imageValues = null; // Raw image (returned by ip.getPixels()) - float is used to allow 8, 16 or 32 bit images
        houghValues = null; // Hough Space Values [X coord][Y coord][radius index]
        localHoughValues = null; //Local Hough space [circle#][X coord][Y coord][radius index]
        localHoughParameters = null; //Array to pass local Hough space parameters to centroid search [circle#][parameter vector]

        centerPoint = null; // Center Points of the Circles Found.
        centerRadii = null; //Corresponding radii of the cricles marked by the center points
        houghScores = null; //Corresponding Hough scores for each centroid
        circleID = null; //Corresponding ID # for each centroid
        lut = null; // LookUp Table for x and y tranform shifts in an octahedral manner

        houghScores = null;
        centerRadii = null;
        centerPoint = null;
        circleID = null;

        gc();
    }

    //OPTMIZED - cancellable
    private void startLocalTransform() {
        //Initialize an array to store the local Hough transform from each thread
        localHoughValues = new int[nCirlcesPrev][2 * searchRadius + 1][2 * searchRadius + 1][2 * searchBand + 1];

        //Initialize an array to store the final dimensions and location of each local Hough Transform
        localHoughParameters = new int[nCirlcesPrev][9];

        //Setup two processing pipelines to multithread only if there are more than 1 circles
        if (nCirlcesPrev < 2) {
            localHoughTransform(0);
            localGetCenterPoint(0); //Make sure this returns -1 to index if no circle was found
            maxHough = houghScores[0];
        }

        //Otherwise, multithread the local search
        else {
            //Build an array to store the result from each thread
            final Thread[] threads = newThreadArray();

            //Create an atomic integer counter that each thread can use to count through the radii
            final AtomicInteger ai = new AtomicInteger(0);

            //Build a thread for as many CPUs as are available to the JVM
            for (ithread = 0; ithread < threads.length; ithread++) {

                // Concurrently run in as many threads as CPUs
                threads[ithread] = new Thread() {

                    {
                        setPriority(Thread.NORM_PRIORITY);
                    }

                    @Override
                    public void run() {

                        //Divide the task so that each core works on a subset of circles
                        for (int circleNum = ai.getAndAdd(1); circleNum < nCirlcesPrev; circleNum = ai.getAndAdd(1)) {
                            //Check for interrupt
                            if (cancelThread) return;

                            localHoughTransform(circleNum);
                            localGetCenterPoint(circleNum); //Make sure this returns -1 to index if no circle was found
                        }
                    }
                };
            }
            startAndJoin(threads);

            //Retrieve the maximum hough value if the raw Hough is desired
            if (houghSeries) {
                for (int i = 0; i < nCirlcesPrev; i++) {
                    if (houghScores[i] > maxHough) maxHough = houghScores[i];
                }
            }
        }

        //Flag move all circles to the starting indexes, so there are no gaps between circles (i.e. if there were 3 circles, then they occupy indeces 0-2)
        collapseLocalResult();
    }

    //OPTIMIZED - cancellable
    private void startPartialLocalSearch() {
        //Build an array to store the result from each thread
        final Thread[] threads = newThreadArray();

        //Create an atomic integer counter that each thread can use to count through the radii
        final AtomicInteger ai = new AtomicInteger(0);

        //Build a thread for as many CPUs as are available to the JVM
        for (ithread = 0; ithread < threads.length; ithread++) {

            // Concurrently run in as many threads as CPUs
            threads[ithread] = new Thread() {

                {
                    setPriority(Thread.NORM_PRIORITY);
                }

                @Override
                public void run() {

                    //Divide the task so that each core works on a subset of circles
                    for (int circleNum = ai.getAndAdd(1); circleNum < nCirlcesPrev; circleNum = ai.getAndAdd(1)) {
                        getLocalCenterPoint2(circleNum);

                        //Check for interrupt
                        if (cancelThread) return;
                    }
                }
            };
        }
        startAndJoin(threads);

        //Clear out the remainder of the previous circles in the circle information arrays
        for (int a = nCirlcesPrev; a < circleID.length; a++) {
            circleID[a] = -1;
            centerPoint[a] = new Point(-1, -1);
            centerRadii[a] = -1;
            houghScores[a] = -1;
        }

        //Flag move all circles to the starting indexes, so there are no gaps between circles (i.e. if there were 3 circles, then they occupy indeces 0-2)
        collapseLocalResult();
    }

    //OPTMIZED - not time limiting - cancellable
    //Build an array of the cartesion transformations necessary for each increment at each radius
    private int buildLookUpTable() {
        //Build an array to store the X and Y coordinates for each angle increment (resolution) across each radius (depth)
        lut = new int[2][resolution][depth];

        //Initialize a variable that will allow for measuring the maximium LUT size
        int maxLUT = 0;

        //Step through all radii to be sampled
        for (int radius = radiusMax; radius >= radiusMin; radius -= radiusInc) {
            //Check for interrupt
            if (cancelThread) return 0;

            //Index counter that also tracks the largest actual LUT size (may be <= resolution)
            int i = 0;
            for (int resStep = 0; resStep < resolution; resStep++) {

                //Calcualte the angle and corresponding X and Y displacements for the specified radius
                double angle = (2D * Math.PI * (double) resStep) / (double) resolution;
                int indexR = (radius - radiusMin) / radiusInc;
                int rcos = (int) Math.round((double) radius * Math.cos(angle));
                int rsin = (int) Math.round((double) radius * Math.sin(angle));

                //Test to make sure that the coordinate is a new coordinate
                //NOTE: A continuous circle is being discretized into pixels, it is possible that small angle steps for small radii circles will occupy the same pixel.
                //Since there is no point in making redundant calculations, these points are excluded from the LUT
                //NOTE: Using the minRadius as the transform cutoff results in a strong harmonic of the image forming near the min radius
                //threfore, using the max will push this harmonic outside the search range.
                if (radius == radiusMax && reduce) {
                    if (i == 0) {
                        lut[0][i][indexR] = rcos;
                        lut[1][i][indexR] = rsin;
                        i++;
                    } else if ((rcos != lut[0][i - 1][indexR]) | (rsin != lut[1][i - 1][indexR])) {
                        lut[0][i][indexR] = rcos;
                        lut[1][i][indexR] = rsin;
                        i++;
                    }
                } else {
                    lut[0][i][indexR] = rcos;
                    lut[1][i][indexR] = rsin;
                    i++;
                }
            }

            //If this is the smallest radius, see how many transforms could be done, and set this as the new resolution
            if (radius == radiusMax) {
                maxLUT = i;
                resolution = maxLUT;
            }
        }
        return maxLUT;
    }

    //The local Hough is an inversion of the inertial reference frame used in the full Hough
    //In the full Hough the image is seach for pixels with a value > 1, and if this is true
    //then the pixel is projected in a circle about that point.

    //OPTIMIZED - cancellable
    private void houghTransform() {
        //Update progress bar string with current task
        if (isGUI) publish("Performing full Hough transform...");
        IJ.showStatus("Performing full Hough transform...");

        //Build an array to store the result from each thread
        final Thread[] threads = newThreadArray();

        //Create an atomic integer counter that each thread can use to count through the radii
        final AtomicInteger ai = new AtomicInteger(radiusMin);
        final AtomicInteger progress = new AtomicInteger(0);
        final AtomicInteger lastProgress = new AtomicInteger(0);

        //Create an array to store the Hough values
        houghValues = new int[width][height][depth];

        //Create a variable for storing the total progress possible (100 = complete)
        //Nute depth is devided by nCPUs, therefore depth*nCPUs/nCPUs = depth
        double totalProgress = height * depth / 100;

        //Build a thread for as many CPUs as are available to the JVM
        for (ithread = 0; ithread < threads.length; ithread++) {

            // Concurrently run in as many threads as CPUs
            threads[ithread] = new Thread() {

                {
                    setPriority(Thread.NORM_PRIORITY);
                }

                @Override
                public void run() {

                    //Divide the radius tasks across the cores available
                    int currentProgress = 0;
                    for (int radius = ai.getAndAdd(radiusInc); radius <= radiusMax; radius = ai.getAndAdd(radiusInc)) {
                        int indexR = (radius - radiusMin) / radiusInc;
                        //For a given radius, transform each pixel in a circle, and add-up the votes
                        for (int y = 1; y < height - 1; y++) {
                            //Increment the progress counter, and submit the current progress status
                            progress.getAndAdd(1);

                            //Calculate the current progress value
                            currentProgress = Math.round((float) (progress.get() / totalProgress));

                            //There is a significant time penalty for progress updates, so only update if needed
                            if (currentProgress > lastProgress.get()) { //7.8s with if, 8.7s without if, 7.8s with no progress update, 8.7s with delay between GUI updates
                                if (isGUI && currentProgress <= 100) setProgress(currentProgress);
                                IJ.showProgress(currentProgress, 100);
                                lastProgress.set(currentProgress);
                            }

                            //Check for interrupt
                            if (cancelThread) return;

                            for (int x = 1; x < width - 1; x++) {
                                if (imageValues[(x + offx) + (y + offy) * fullWidth] != 0) {// Edge pixel found
                                    for (int i = 0; i < lutSize; i++) {
                                        int a = x + lut[1][i][indexR];
                                        int b = y + lut[0][i][indexR];
                                        if ((b >= 0) & (b < height) & (a >= 0) & (a < width)) {
                                            houghValues[a][b][indexR] += 1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }
        startAndJoin(threads);
    }

    //OPTMIZED - cancellable
    //To reduce the necessary transform space,
    private void localHoughTransform(int index) {
        //Initialize local search variables
        int startWidth = centerPoint[index].x - searchRadius;
        if (startWidth < 1) startWidth = 1; //Keep search area inside the image
        int endWidth = centerPoint[index].x + searchRadius;
        if (endWidth > width) endWidth = width; //Keep search area inside the image
        int lwidth = endWidth - startWidth + 1;

        int startHeight = centerPoint[index].y - searchRadius;
        if (startHeight < 1) startHeight = 1; //Keep search area inside the image
        int endHeight = centerPoint[index].y + searchRadius;
        if (endHeight > height) endHeight = height; //Keep search area inside the image
        int lheight = endHeight - startHeight + 1;

        int lradiusMin = centerRadii[index] - searchBand;
        if (lradiusMin < radiusMin) lradiusMin = radiusMin;
        int lradiusMax = centerRadii[index] + searchBand;
        if (lradiusMax > radiusMax) lradiusMax = radiusMax;
        int ldepth = ((lradiusMax - lradiusMin) / radiusInc) + 1;

        //Store local Hough parameters into array
        localHoughParameters[index][0] = startWidth;
        localHoughParameters[index][1] = endWidth;
        localHoughParameters[index][2] = lwidth;
        localHoughParameters[index][3] = startHeight;
        localHoughParameters[index][4] = endHeight;
        localHoughParameters[index][5] = lheight;
        localHoughParameters[index][6] = lradiusMin;
        localHoughParameters[index][7] = lradiusMax;
        localHoughParameters[index][8] = ldepth;

        //Divide the radius tasks across the cores available
        for (int radius = lradiusMin; radius <= lradiusMax; radius += radiusInc) {
            int indexR = (radius - lradiusMin) / radiusInc;
            //For a given radius, transform each pixel in a circle, and add-up the votes
            for (int y = startHeight; y < endHeight - 1; y++) {

                //Check for interrupt
                if (cancelThread) return;

                for (int x = startWidth; x < endWidth - 1; x++) {
                    for (int i = 0; i < lutSize; i++) {
                        int a = x + offx + lut[1][i][indexR + ((lradiusMin - radiusMin) / radiusInc)];
                        int b = y + offy + lut[0][i][indexR + ((lradiusMin - radiusMin) / radiusInc)];

                        //Check to make sure pixel is within the image
                        if ((a > 1) & (a < fullWidth - 1) & (b > 1) & (b < fullHeight - 1)) {
                            //See if the pixel has an intensity > 1, if so, add 1 to the vote
                            if (imageValues[a + (b * fullWidth)] > 0) {
                                localHoughValues[index][x - startWidth][y - startHeight][indexR] += 1;
                            }
                        }
                    }
                }
            }
        }
    }

    //OPTMIZED - cancellable
    //Find the largest Hough pixel in the 3D Hough transform array to scale the 8-bit conversion
    private void houghMaximum() {
        //long startTime = System.currentTimeMillis(); //1337ms without multi, 319ms with multi, 175ms by writing to private variable per thread
        //Build an array to store the result from each thread
        final Thread[] threads = newThreadArray();

        //Build an array to store the max from each thread
        maxHoughArray = new int[threads.length][4];

        //Create an atomic integer counter that each thread can use to count through the radii
        final AtomicInteger ai = new AtomicInteger(0);
        final AtomicInteger progress = new AtomicInteger(0);
        final AtomicInteger lastProgress = new AtomicInteger(0);

        //Create an integer for indexing the results array (one result per thread
        final AtomicInteger Index = new AtomicInteger(0);

        //Create a variable for storing the total progress possible (100 = complete)
        //Nute depth is devided by nCPUs, therefore depth*nCPUs/nCPUs = depth
        double totalProgress = height * depth / 100;

        maxHough = 0;

        //Build a thread for as many CPUs as are available to the JVM
        for (ithread = 0; ithread < threads.length; ithread++) {

            // Concurrently run in as many threads as CPUs
            threads[ithread] = new Thread() {

                {
                    setPriority(Thread.NORM_PRIORITY);
                }

                //Search for the largest score with each thread
                @Override
                public void run() {
                    int maxHoughThread = -1;
                    int maxRadiusThread = -1;
                    int currentProgress = 0;
                    Point maxPointThread = new Point(-1, -1);
                    for (int a = ai.getAndIncrement(); a < depth; a = ai.getAndIncrement()) {
                        for (int j = 0; j < height; j++) {
                            //Increment the progress counter, and submit the current progress status
                            progress.getAndAdd(1);

                            //Gui updates can be time intensive, so only update at fixed time intervals
                            currentProgress = Math.round((float) (progress.get() / totalProgress));

                            //There is a significant time penalty for progress updates, so only update if needed
                            if (currentProgress > lastProgress.get() & currentProgress >= 0 & currentProgress <= 100) { //7.8s with if, 8.7s without if, 7.8s with no progress update, 8.7s with delay between GUI updates
                                if (isGUI) setProgress(currentProgress);
                                IJ.showProgress(currentProgress, 100);
                                lastProgress.set(currentProgress);
                            }

                            //Check for interrupt
                            if (cancelThread) return;

                            for (int k = 0; k < width; k++) {
                                if (houghValues[k][j][a] > maxHoughThread) {
                                    maxHoughThread = houghValues[k][j][a];
                                    maxPointThread = new Point(k, j);
                                    maxRadiusThread = a * radiusInc + radiusMin;
                                }
                            }
                        }
                    }
                    //Have each thread report the score to a common array
                    maxHoughArray[Index.getAndIncrement()] = new int[]{maxHoughThread, maxRadiusThread, maxPointThread.x, maxPointThread.y};
                }
            };
        }
        startAndJoin(threads);

        //Search common array for highest score
        for (int[] maxHoughArray1 : maxHoughArray) {
            if (maxHoughArray1[0] > maxHough) {
                maxHough = maxHoughArray1[0];
                maxRadius = maxHoughArray1[1];
                maxPoint = new Point((int) maxHoughArray1[2], (int) maxHoughArray1[3]);
            }
        }
    }

    //OPTMIZED - cancellable
    //Create a Hough stack series using the local transforms
    private void convertLocaltoFullHough(int slice, ImageStack houghStack) {


        int startFrame = ((slice - 1) * depth);

        //Create an array to store the Hough values
        byte localHoughPixels[][] = new byte[depth][width * height];

        //NOTE: Only do multithreading if the problem is sufficiently complex
        if (localHoughParameters.length < 2 & localHoughParameters.length * searchBand * searchRadius * searchRadius < 15000) {
            //Extract the local search parameters
            //Divide the task so that each core works on a subset of circles
            for (int circleNum = 0; circleNum < localHoughParameters.length; circleNum++) {
                //Extract the local search parameters
                int startWidth = localHoughParameters[circleNum][0];
                int endWidth = localHoughParameters[circleNum][1];
                int lwidth = localHoughParameters[circleNum][2];
                int startHeight = localHoughParameters[circleNum][3];
                int endHeight = localHoughParameters[circleNum][4];
                int lheight = localHoughParameters[circleNum][5];
                int lradiusMin = localHoughParameters[circleNum][6];
                int lradiusMax = localHoughParameters[circleNum][7];
                int ldepth = localHoughParameters[circleNum][8];
                for (int h = 0; h < lheight; h++) {
                    //Check for interrupt
                    if (cancelThread) return;

                    for (int w = 0; w < lwidth; w++) {
                        for (int i = 0; i < ldepth; i++) {
                            localHoughPixels[i + (lradiusMin - radiusMin) / radiusInc][(w + startWidth) + (h + startHeight) * width] = (byte) Math.round((localHoughValues[circleNum][w][h][i] * 255D) / maxHough);
                        }
                    }
                }
            }
        }

        //Otherwise, multithread the depositing of pixels
        else {
            //Build an array to store the result from each thread
            final Thread[] threads = newThreadArray();

            //Create an atomic integer counter that each thread can use to count through the radii
            final AtomicInteger ai = new AtomicInteger(0);

            //Build a thread for as many CPUs as are available to the JVM
            for (ithread = 0; ithread < threads.length; ithread++) {

                // Concurrently run in as many threads as CPUs
                threads[ithread] = new Thread() {

                    {
                        setPriority(Thread.NORM_PRIORITY);
                    }

                    @Override
                    public void run() {

                        //Divide the task so that each core works on a subset of circles
                        for (int circleNum = ai.getAndAdd(1); circleNum < localHoughParameters.length; circleNum = ai.getAndAdd(1)) {
                            //Extract the local search parameters
                            int startWidth = localHoughParameters[circleNum][0];
                            int endWidth = localHoughParameters[circleNum][1];
                            int lwidth = localHoughParameters[circleNum][2];
                            int startHeight = localHoughParameters[circleNum][3];
                            int endHeight = localHoughParameters[circleNum][4];
                            int lheight = localHoughParameters[circleNum][5];
                            int lradiusMin = localHoughParameters[circleNum][6];
                            int lradiusMax = localHoughParameters[circleNum][7];
                            int ldepth = localHoughParameters[circleNum][8];
                            int index = 0;
                            int maxIndex = width * height;
                            for (int h = 0; h < lheight; h++) {
                                //Check for interrupt
                                if (cancelThread) return;

                                for (int w = 0; w < lwidth; w++) {
                                    for (int i = 0; i < ldepth; i++) {
                                        index = (w + startWidth) + (h + startHeight) * width;
                                        if (index < maxIndex)
                                            localHoughPixels[i + (lradiusMin - radiusMin) / radiusInc][index] = (byte) Math.round((localHoughValues[circleNum][w][h][i] * 255D) / maxHough);
                                    }
                                }
                            }
                        }
                    }
                };
            }
            startAndJoin(threads);
        }

        //Deposit the array into the Hough Series stack
        //Not time limiting, even at fairly high resolutions
        for (int radius = radiusMin; radius <= radiusMax; radius += radiusInc) {
            //Calculate the corresponding index
            int houghIndex = (radius - radiusMin) / radiusInc;

            //Deposit the array image into the corresponding slice in the stack
            houghStack.setPixels(localHoughPixels[houghIndex], houghIndex + 1 + startFrame);

            //Give the current slice the appropriate radius label
            houghStack.setSliceLabel("Hough Space [r=" + radius + ", resolution=" + resolution + "]", houghIndex + 1 + startFrame);
        }

    }

    //OPTIMIZED - cancellable
    //Add transform series data to the hyperstack
    private void HoughSpaceSeries(int slice, ImageStack houghStack) {
        //If the maximum Hough value has not yet been assigned, search the whole Hough transform for the maximum value
        if (maxHough == -1) {
            houghMaximum();
        }

        //Create an array to store the Hough values
        byte localHoughPixels[][] = new byte[depth][width * height];
        int startFrame = ((slice - 1) * depth);

        //If a full transform was done, calcualte the Hough Pixels
        if (minCircles > 0 | nCirlcesPrev > 0) {
            //Build an array to store the result from each thread
            final Thread[] threads = newThreadArray();

            //Create an atomic integer counter that each thread can use to count through the radii
            final AtomicInteger ai = new AtomicInteger(radiusMin);

            //Build a thread for as many CPUs as are available to the JVM
            for (ithread = 0; ithread < threads.length; ithread++) {

                // Concurrently run in as many threads as CPUs
                threads[ithread] = new Thread() {

                    {
                        setPriority(Thread.NORM_PRIORITY);
                    }

                    @Override
                    public void run() {

                        //Divide the radius tasks across the cores available
                        for (int radius = ai.getAndAdd(radiusInc); radius <= radiusMax; radius = ai.getAndAdd(radiusInc)) {
                            //Check for interrupt
                            if (cancelThread) return;

                            //Calculate the corresponding index
                            int houghIndex = (radius - radiusMin) / radiusInc;

                            //If full tansform was performed, retrieve the pixel array for the current Hough radius image
                            createHoughPixels(localHoughPixels[houghIndex], houghIndex);

                            //Deposit the array image into the corresponding slice in the stack
                            houghStack.setPixels(localHoughPixels[houghIndex], houghIndex + 1 + startFrame);

                            //Give the current slice the appropriate radius label
                            houghStack.setSliceLabel("Hough Space [r=" + radius + ", resolution=" + resolution + "]", houghIndex + 1 + startFrame);
                        }
                    }
                };
            }
            startAndJoin(threads);
        }

        //Deposit the array into the Hough Series stack
        //Not time limiting, even at fairly high resolutions
        for (int radius = radiusMin; radius <= radiusMax; radius += radiusInc) {
            //Check for interrupt
            if (cancelThread) return;

            //Calculate the corresponding index
            int houghIndex = (radius - radiusMin) / radiusInc;

            //Deposit the array image into the corresponding slice in the stack
            houghStack.setPixels(localHoughPixels[houghIndex], houghIndex + 1 + startFrame);

            //Give the current slice the appropriate radius label
            houghStack.setSliceLabel("Hough Space [r=" + radius + ", resolution=" + resolution + "]", houghIndex + 1 + startFrame);
        }
    }

    //OPTMIZED - cancellable
    // Convert Values in Hough Space to an 8-Bit Image Space.
    private void createHoughPixels(byte houghPixels[], int index) {
        //Rescale all the Hough values to 8-bit to create the Hough image - 47ms to complete - single threading okay
        for (int l = 0; l < height; l++) {
            //Check for interrupt
            if (cancelThread) return;

            for (int i = 0; i < width; i++) {
                houghPixels[i + l * width] = (byte) Math.round((houghValues[i][l][index] * 255D) / maxHough);
            }

        }
    }

    // Draw the circles found in the original image. - cancellable
    private void drawCircles(int slice, ImageStack circleStack, int widthROI, int heightROI, int fullWidthX, int fullWidthY, int fullWidthROI) {

        // Copy original input pixels into output
        // circle location display image and
        // combine with saturation at 100
        byte[] circlespixels = new byte[widthROI * heightROI];

        int roiaddr = 0;
        for (int y = fullWidthY; y < fullWidthY + heightROI; y++) {
            //Check for interrupt
            if (cancelThread) return;
            for (int x = fullWidthX; x < fullWidthX + widthROI; x++) {
                // Copy;
                circlespixels[roiaddr] = (byte) imageValues[x + fullWidthROI * y];
                // Saturate
                if (circlespixels[roiaddr] != 0)
                    circlespixels[roiaddr] = 100;
                else
                    circlespixels[roiaddr] = 0;
                roiaddr++;
            }
        }
        // Copy original image to the circlespixels image.
        // Changing pixels values to 100, so that the marked
        // circles appears more clear. Must be improved in
        // the future to show the resuls in a colored image.
        //for(int i = 0; i < width*height ;++i ) {
        //if(imageValues[i] != 0 )
        //if(circlespixels[i] != 0 )
        //circlespixels[i] = 100;
        //else
        //circlespixels[i] = 0;
        //}
        if (centerPoint == null) getCenterPoints();

        byte cor = (byte) 255;
        // Redefine these so refer to ROI coordinates exclusively
        fullWidthROI = widthROI;
        fullWidthX = 0;
        fullWidthY = 0;

        for (int l = 0; l < nCircles; l++) {
            int i = centerPoint[l].x;
            int j = centerPoint[l].y;

            //Check for interrupt
            if (cancelThread) return;
            // Draw a gray cross marking the center of each circle.
            for (int k = -10; k <= 10; ++k) {
                int p = (j + k + fullWidthY) * fullWidthROI + (i + fullWidthX);
                if (!outOfBounds(j + k + fullWidthY, i + fullWidthX))
                    circlespixels[(j + k + fullWidthY) * fullWidthROI + (i + fullWidthX)] = cor;
                if (!outOfBounds(j + fullWidthY, i + k + fullWidthX))
                    circlespixels[(j + fullWidthY) * fullWidthROI + (i + k + fullWidthX)] = cor;
            }
            for (int k = -2; k <= 2; ++k) {
                if (!outOfBounds(j - 2 + fullWidthY, i + k + fullWidthX))
                    circlespixels[(j - 2 + fullWidthY) * fullWidthROI + (i + k + fullWidthX)] = cor;
                if (!outOfBounds(j + 2 + fullWidthY, i + k + fullWidthX))
                    circlespixels[(j + 2 + fullWidthY) * fullWidthROI + (i + k + fullWidthX)] = cor;
                if (!outOfBounds(j + k + fullWidthY, i - 2 + fullWidthX))
                    circlespixels[(j + k + fullWidthY) * fullWidthROI + (i - 2 + fullWidthX)] = cor;
                if (!outOfBounds(j + k + fullWidthY, i + 2 + fullWidthX))
                    circlespixels[(j + k + fullWidthY) * fullWidthROI + (i + 2 + fullWidthX)] = cor;
            }

            //Draw Bresenham circle
            int x = centerRadii[l];
            int y = 0;
            int err = 0;
            while (x >= y) {
                if (!outOfBounds(j + y + fullWidthY, i + x + fullWidthX))
                    circlespixels[(j + y + fullWidthY) * fullWidthROI + (i + x + fullWidthX)] = cor;
                if (!outOfBounds(j + y + fullWidthY, i - x + fullWidthX))
                    circlespixels[(j + y + fullWidthY) * fullWidthROI + (i - x + fullWidthX)] = cor;
                if (!outOfBounds(j - y + fullWidthY, i + x + fullWidthX))
                    circlespixels[(j - y + fullWidthY) * fullWidthROI + (i + x + fullWidthX)] = cor;
                if (!outOfBounds(j - y + fullWidthY, i - x + fullWidthX))
                    circlespixels[(j - y + fullWidthY) * fullWidthROI + (i - x + fullWidthX)] = cor;

                if (!outOfBounds(j + x + fullWidthY, i + y + fullWidthX))
                    circlespixels[(j + x + fullWidthY) * fullWidthROI + (i + y + fullWidthX)] = cor;
                if (!outOfBounds(j + x + fullWidthY, i - y + fullWidthX))
                    circlespixels[(j + x + fullWidthY) * fullWidthROI + (i - y + fullWidthX)] = cor;
                if (!outOfBounds(j - x + fullWidthY, i + y + fullWidthX))
                    circlespixels[(j - x + fullWidthY) * fullWidthROI + (i + y + fullWidthX)] = cor;
                if (!outOfBounds(j - x + fullWidthY, i - y + fullWidthX))
                    circlespixels[(j - x + fullWidthY) * fullWidthROI + (i - y + fullWidthX)] = cor;

                if (err <= 0) {
                    y += 1;
                    err += 2 * y + 1;
                } else {
                    x -= 1;
                    err -= 2 * x + 1;
                }
            }

        }
        circleStack.setPixels(circlespixels, slice);
        circleStack.setSliceLabel(nCircles + " circles found", slice);
    }

    // Draw the centroids found in the original image where intensity = radius.
    private void drawFilledCircles(int slice, ImageStack idStack, ImageStack scoreStack) {

        //Create arrays the same size as the images
        short[] IDpixels = new short[width * height];
        float[] scorepixels = new float[width * height];

        for (int l = 0; l < nCircles; l++) {
            int i = centerPoint[l].x;
            int j = centerPoint[l].y;
            int radius = centerRadii[l];
            short ID = (short) circleID[l];
            float score = (float) houghScores[l] / (float) resolution;
            int rSquared = radius * radius;

            for (int y = -1 * radius; y <= radius; y++) {
                for (int x = -1 * radius; x <= radius; x++) {
                    if (x * x + y * y <= rSquared) {
                        if (showID) {
                            if (!outOfBounds(j + y, i + x)) IDpixels[(j + y) * width + i + x] = ID;
                        }
                        if (showScores) {
                            if (!outOfBounds(j + y, i + x)) scorepixels[(j + y) * width + i + x] = score;
                        }
                    }
                }
            }
        }

        if (showID) {
            idStack.setPixels(IDpixels, slice);
            idStack.setSliceLabel(nCircles + " circles found", slice);
        }
        if (showScores) {
            scoreStack.setPixels(scorepixels, slice);
            scoreStack.setSliceLabel(nCircles + " circles found", slice);
        }
    }

    //Export the results to the results table
    public void resultsTable(int frame) {
        for (int a = 0; a < nCircles; a++) {
            rt.incrementCounter();
            rt.addValue("ID", circleID[a]);
            rt.addValue("X (" + pixelUnits + ")", centerPoint[a].x * pixelDimensions);
            rt.addValue("Y (" + pixelUnits + ")", centerPoint[a].y * pixelDimensions);
            rt.addValue("Radius (" + pixelUnits + ")", centerRadii[a] * pixelDimensions);
            rt.addValue("Score", ((float) houghScores[a] / resolution));
            rt.addValue("nCircles", nCircles);
            rt.addValue("Resolution", lutSize);
            rt.addValue("Frame (" + timeUnits + ")", frame * timeDimension);
            rt.addValue("Method", method);
        }
    }

    //OPTMIZED - cancellable

    private boolean outOfBounds(int y, int x) {
        if (x >= width)
            return (true);
        if (x <= 0)
            return (true);
        if (y >= height)
            return (true);
        if (y <= 0)
            return (true);
        return (false);
    }

    /**
     * Search for a fixed number of circles.
     */
    private void getCenterPoints() {

        int countCircles = nCircles;
        maxHough = threshold;

        while (countCircles < maxCircles && maxHough >= threshold) {
            //Check for interrupt
            if (cancelThread) return;

            //Update bar string with current circle that is being searched for
            if (isGUI) publish("Searching for circles. " + countCircles + " circles found.");
            IJ.showStatus("Searching for circles. " + countCircles + " circles found.");

            //Search for the highest remaining Hough score in the matrix
            houghMaximum();

            if (maxHough >= threshold) {
                circleIDcounter++;
                circleID[countCircles] = circleIDcounter;
                centerPoint[countCircles] = maxPoint;
                centerRadii[countCircles] = maxRadius;
                houghScores[countCircles] = maxHough;
                countCircles++;
                clearNeighbours(maxPoint.x, maxPoint.y, maxRadius);
            }
        }

        nCircles = countCircles;

        //Clear the remainder of result arrays, since these results are from the previous round
        for (int i = nCircles; i < maxCircles; i++) {
            circleID[i] = -1;
            centerPoint[i] = new Point(-1, -1);
            centerRadii[i] = -1;
            houghScores[i] = -1;
        }
    }

    //OPTIMIZED - cancellable
    private void getLocalCenterPoint2(int index) {
        //Initialize local search variables
        //Initialize local search variables
        int startWidth = centerPoint[index].x - searchRadius;
        if (startWidth < 1) startWidth = 1; //Keep search area inside the image
        int endWidth = centerPoint[index].x + searchRadius;
        if (endWidth > width) endWidth = width; //Keep search area inside the image

        int startHeight = centerPoint[index].y - searchRadius;
        if (startHeight < 1) startHeight = 1; //Keep search area inside the image
        int endHeight = centerPoint[index].y + searchRadius;
        if (endHeight > height) endHeight = height; //Keep search area inside the image

        int lradiusMin = centerRadii[index] - searchBand;
        if (lradiusMin < radiusMin) lradiusMin = radiusMin;
        int lradiusMax = centerRadii[index] + searchBand;
        if (lradiusMax > radiusMax) lradiusMax = radiusMax;


        //Search locally for the highest hough score in the full Hough Space
        int maxLocalHough = -1;
        int maxLocalRadius = -1;
        Point maxLocalPoint = new Point(-1, -1);
        for (int radius = lradiusMin; radius <= lradiusMax; radius += radiusInc) {
            int indexR = (radius - radiusMin) / radiusInc;
            for (int j = startHeight; j < endHeight; j++) {
                //Check for interrupt
                if (cancelThread) return;

                for (int k = startWidth; k < endWidth; k++) {
                    if (houghValues[k][j][indexR] > maxLocalHough) {
                        maxLocalHough = houghValues[k][j][indexR];
                        maxLocalPoint = new Point(k, j);
                        maxLocalRadius = radius;
                    }
                }
            }
        }
        //If the highest score is above the threshold, record the new circle
        if (maxLocalHough >= threshold) {
            centerPoint[index] = maxLocalPoint;
            centerRadii[index] = maxLocalRadius;
            houghScores[index] = maxLocalHough;
            clearNeighbours(maxLocalPoint.x, maxLocalPoint.y, maxLocalRadius);
        }
        //Otherwise, record that the circle was lost
        else {
            circleID[index] = -1;
            centerPoint[index] = new Point(-1, -1);
            centerRadii[index] = -1;
            houghScores[index] = -1;
        }
    }

    //OPTMIZED - cancellable
    private void localGetCenterPoint(int index) {

        //Extract the local search parameters
        int startWidth = localHoughParameters[index][0];
        int endWidth = localHoughParameters[index][1];
        int lwidth = localHoughParameters[index][2];
        int startHeight = localHoughParameters[index][3];
        int endHeight = localHoughParameters[index][4];
        int lheight = localHoughParameters[index][5];
        int lradiusMin = localHoughParameters[index][6];
        int lradiusMax = localHoughParameters[index][7];
        int ldepth = localHoughParameters[index][8];

        //Search for the highest hough score in the local Hough Space
        int maxLocalHough = -1;
        int maxLocalRadius = -1;
        Point maxLocalPoint = new Point(-1, -1);
        for (int a = 0; a < ldepth; a++) {
            for (int j = 0; j < lheight; j++) {
                //Check for interrupt
                if (cancelThread) return;

                for (int k = 0; k < lwidth; k++) {
                    if (localHoughValues[index][k][j][a] > maxLocalHough) {
                        maxLocalHough = localHoughValues[index][k][j][a];
                        maxLocalPoint = new Point(k + startWidth, j + startHeight);
                        maxLocalRadius = a * radiusInc + lradiusMin;
                    }
                }
            }
        }

        //If the highest score is above the threshold, record the new circle
        if (maxLocalHough >= threshold) {
            centerPoint[index] = maxLocalPoint;
            centerRadii[index] = maxLocalRadius;
            houghScores[index] = maxLocalHough;
        }
        //Otherwise, record that the circle was lost
        else {
            circleID[index] = -1;
            centerPoint[index] = new Point(-1, -1);
            centerRadii[index] = -1;
            houghScores[index] = -1;
        }
    }

    ;

    //OPTIMIZED - Not time limiting, even with large circles - cancellable

    //OPTMIZED
    private void collapseLocalResult() {
        //search for indeces containing circles (i.e. index != -1)
        int[] idIndeces = new int[circleID.length];
        int indexCounter = 0;
        for (int i = 0; i < circleID.length; i++) {
            //If a valid ID is found, record the index
            if (circleID[i] != -1) {
                idIndeces[indexCounter] = i;
                indexCounter++;
            }
        }

        //Move all the found results to the starting indeces of the arrays if needed
        if (indexCounter < maxCircles) {
            for (int i = 0; i < indexCounter; i++) {
                //If index is empty, then move the result
                if (circleID[i] == -1) {
                    //Check to see index is empty
                    //Move results
                    circleID[i] = circleID[idIndeces[i]];
                    houghScores[i] = houghScores[idIndeces[i]];
                    centerRadii[i] = centerRadii[idIndeces[i]];
                    centerPoint[i] = centerPoint[idIndeces[i]];

                    //Clear original index
                    circleID[idIndeces[i]] = -1;
                    houghScores[idIndeces[i]] = -1;
                    centerRadii[idIndeces[i]] = -1;
                    centerPoint[idIndeces[i]] = new Point(-1, -1);
                }
            }
        }

        //Update nCircles to reflect the new found number of circles
        nCircles = indexCounter;
    }

    /**
     * Clear, from the Hough Space, all the counter that are near (radius/2) a previously found circle C.
     *
     * @param x The x coordinate of the circle C found.
     * @param x The y coordinate of the circle C found.
     * @param x The radius of the circle C found.
     */
    private void clearNeighbours(int x, int y, int radius) {
        // The following code just clean the points around the center of the circle found.
        radius = (int) Math.round(radius * ratio); //Scale the radius by the desired clearing ratio
        int radiusSquared = (int) Math.pow(radius, 2D);
        //int radiusSquared = radius*radius;

        int y1 = (int) Math.floor(y - radius);
        int y2 = (int) Math.ceil(y + radius) + 1;
        int x1 = (int) Math.floor(x - radius);
        int x2 = (int) Math.ceil(x + radius) + 1;

        if (y1 < 0)
            y1 = 0;
        if (y2 > height)
            y2 = height;
        if (x1 < 0)
            x1 = 0;
        if (x2 > width)
            x2 = width;

        for (int indexR = 0; indexR < depth; indexR++) {
            for (int i = y1; i < y2; i++) {
                //Check for interrupt
                if (cancelThread) return;
                for (int j = x1; j < x2; j++) {
                    if ((int) (Math.pow(j - x, 2D) + Math.pow(i - y, 2D)) < radiusSquared) {
                        houghValues[j][i][indexR] = 0;
                    }
                }
            }
        }
    }

    /**
     * Create a Thread[] array as large as the number of processors available.
     * From Stephan Preibisch's Multithreading.java class. See:
     * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
     */
    private Thread[] newThreadArray() {
        int n_cpus = Runtime.getRuntime().availableProcessors();
        return new Thread[n_cpus];
    }

    //Catches publish info from background thread so that the status can be checked
    @Override
    protected void process(List<String> status) {
        currentStatus = status.get(status.size() - 1);
    }

    //Allows GUI class to get status updates from the worker thread
    public String getStatus() {
        return currentStatus;
    }

    //Flags that all active child threads should stop when cancel button is pressed in GUI class
    public void interruptThreads(boolean a) {
        this.cancelThread = a;
    }
}