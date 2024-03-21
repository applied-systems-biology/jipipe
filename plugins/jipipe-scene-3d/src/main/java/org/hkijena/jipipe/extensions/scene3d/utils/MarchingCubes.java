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

package org.hkijena.jipipe.extensions.scene3d.utils;

import gnu.trove.list.array.TFloatArrayList;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;

/**
 * Adapted from <a href="https://github.com/PrimozLavric/MarchingCubes">PrimozLavric</a>
 */
public class MarchingCubes {

    private static final int[] MC_EDGE_TABLE = {
            0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
            0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
            0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
            0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
            0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
            0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
            0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
            0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
            0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
            0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
            0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
            0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
            0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
            0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
            0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
            0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
            0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
            0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
            0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
            0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
            0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
            0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
            0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
            0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
            0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
            0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
            0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
            0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
            0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
            0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
            0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
            0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0};

    private static final int[] MC_TRI_TABLE = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1,
            3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1,
            3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1,
            3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1,
            9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1,
            2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1,
            8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1,
            4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1,
            3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1,
            1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1,
            4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1,
            4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1,
            5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1,
            2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1,
            9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1,
            0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1,
            2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1,
            10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1,
            5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1,
            5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1,
            9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1,
            1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1,
            10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1,
            8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1,
            2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1,
            7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1,
            2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1,
            11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1,
            5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1,
            11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1,
            11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1,
            9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1,
            2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1,
            6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1,
            3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1,
            6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1,
            10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1,
            6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1,
            8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1,
            7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1,
            3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1,
            0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1,
            9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1,
            8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1,
            5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1,
            0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1,
            6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1,
            10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1,
            10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1,
            8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1,
            1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1,
            0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1,
            10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1,
            3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1,
            6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1,
            9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1,
            8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1,
            3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1,
            6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1,
            0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1,
            10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1,
            10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1,
            2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1,
            7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1,
            7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1,
            2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1,
            1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1,
            11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1,
            8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1,
            0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1,
            7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1,
            10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1,
            2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1,
            6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1,
            7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1,
            2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1,
            10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1,
            10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1,
            0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1,
            7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1,
            6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1,
            8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1,
            9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1,
            6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1,
            4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1,
            10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1,
            8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1,
            0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1,
            1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1,
            8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1,
            10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1,
            4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1,
            10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1,
            11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1,
            9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1,
            6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1,
            7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1,
            3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1,
            7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1,
            3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1,
            6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1,
            9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1,
            1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1,
            4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1,
            7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1,
            6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1,
            3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1,
            0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1,
            6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1,
            0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1,
            11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1,
            6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1,
            5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1,
            9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1,
            1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1,
            1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1,
            10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1,
            0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1,
            5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1,
            10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1,
            11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1,
            9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1,
            7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1,
            2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1,
            8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1,
            9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1,
            9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1,
            1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1,
            9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1,
            5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1,
            0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1,
            10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1,
            2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1,
            0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1,
            0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1,
            9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1,
            5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1,
            3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1,
            5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1,
            8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1,
            0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1,
            9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1,
            1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1,
            3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1,
            4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1,
            9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1,
            11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1,
            11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1,
            2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1,
            9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1,
            3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1,
            1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1,
            4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1,
            3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1,
            0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1,
            1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private static float[] lerp(float[] vec1, float[] vec2, float alpha) {
        return new float[]{vec1[0] + (vec2[0] - vec1[0]) * alpha, vec1[1] + (vec2[1] - vec1[1]) * alpha, vec1[2] + (vec2[2] - vec1[2]) * alpha};
    }

    public static float[] marchingCubesByte(byte[] values, int[] volDim, int volZFull, float[] voxDim, char isoLevel, int offset) {

        // Actual position along edge weighted according to function values.
        float vertList[][] = new float[12][3];

        // Output array
        TFloatArrayList vertices = new TFloatArrayList();

        // Calculate maximal possible axis value (used in vertice normalization)
        float maxX = voxDim[0] * (volDim[0] - 1);
        float maxY = voxDim[1] * (volDim[1] - 1);
        float maxZ = voxDim[2] * (volZFull - 1);
        float maxAxisVal = 1;

        // Volume iteration
        for (int z = 0; z < volDim[2] - 1; z++) {
            for (int y = 0; y < volDim[1] - 1; y++) {
                for (int x = 0; x < volDim[0] - 1; x++) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px

                    int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
                            px = p + 1,
                            py = p + volDim[0],
                            pxy = py + 1,
                            pz = p + volDim[0] * volDim[1],
                            pxz = px + volDim[0] * volDim[1],
                            pyz = py + volDim[0] * volDim[1],
                            pxyz = pxy + volDim[0] * volDim[1];

                    //							  X              Y                    Z
                    float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};

                    // Voxel intensities
                    byte value0 = values[p],
                            value1 = values[px],
                            value2 = values[py],
                            value3 = values[pxy],
                            value4 = values[pz],
                            value5 = values[pxz],
                            value6 = values[pyz],
                            value7 = values[pxyz];

                    // Voxel is active if its intensity is above isolevel
                    int cubeindex = 0;
                    if (Byte.toUnsignedInt(value0) > isoLevel)
                        cubeindex |= 1;
                    if (Byte.toUnsignedInt(value1) > isoLevel)
                        cubeindex |= 2;
                    if (Byte.toUnsignedInt(value2) > isoLevel)
                        cubeindex |= 8;
                    if (Byte.toUnsignedInt(value3) > isoLevel)
                        cubeindex |= 4;
                    if (Byte.toUnsignedInt(value4) > isoLevel)
                        cubeindex |= 16;
                    if (Byte.toUnsignedInt(value5) > isoLevel)
                        cubeindex |= 32;
                    if (Byte.toUnsignedInt(value6) > isoLevel)
                        cubeindex |= 128;
                    if (Byte.toUnsignedInt(value7) > isoLevel)
                        cubeindex |= 64;

                    // Fetch the triggered edges
                    int bits = MC_EDGE_TABLE[cubeindex];

                    // If no edge is triggered... skip
                    if (bits == 0) continue;

                    // Interpolate the positions based od voxel intensities
                    float mu = 0.5f;

                    // bottom of the cube
                    if ((bits & 1) != 0) {
                        mu = (isoLevel - value0) / (value1 - value0);
                        vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
                    }
                    if ((bits & 2) != 0) {
                        mu = (isoLevel - value1) / (value3 - value1);
                        vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 4) != 0) {
                        mu = (isoLevel - value2) / (value3 - value2);
                        vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 8) != 0) {
                        mu = (isoLevel - value0) / (value2 - value0);
                        vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    // top of the cube
                    if ((bits & 16) != 0) {
                        mu = (isoLevel - value4) / (value5 - value4);
                        vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 32) != 0) {
                        mu = (isoLevel - value5) / (value7 - value5);
                        vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 64) != 0) {
                        mu = (isoLevel - value6) / (value7 - value6);
                        vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 128) != 0) {
                        mu = (isoLevel - value4) / (value6 - value4);
                        vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    // vertical lines of the cube
                    if ((bits & 256) != 0) {
                        mu = (isoLevel - value0) / (value4 - value0);
                        vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 512) != 0) {
                        mu = (isoLevel - value1) / (value5 - value1);
                        vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 1024) != 0) {
                        mu = (isoLevel - value3) / (value7 - value3);
                        vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 2048) != 0) {
                        mu = (isoLevel - value2) / (value6 - value2);
                        vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }

                    // construct triangles -- get correct vertices from triTable.
                    int i = 0;
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex <<= 4;

                    while (MC_TRI_TABLE[cubeindex + i] != -1) {
                        int index1 = MC_TRI_TABLE[cubeindex + i];
                        int index2 = MC_TRI_TABLE[cubeindex + i + 1];
                        int index3 = MC_TRI_TABLE[cubeindex + i + 2];

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(new float[]{vertList[index3][0] / maxAxisVal - 0.5f, vertList[index3][1] / maxAxisVal - 0.5f, vertList[index3][2] / maxAxisVal - 0.5f,
                                vertList[index2][0] / maxAxisVal - 0.5f, vertList[index2][1] / maxAxisVal - 0.5f, vertList[index2][2] / maxAxisVal - 0.5f,
                                vertList[index1][0] / maxAxisVal - 0.5f, vertList[index1][1] / maxAxisVal - 0.5f, vertList[index1][2] / maxAxisVal - 0.5f});

                        i += 3;
                    }
                }
            }
        }

        return vertices.toArray();
    }

    public static float[] marchingCubesShort(short[] values, int[] volDim, int volZFull, float[] voxDim, short isoLevel, int offset) {
        // Actual position along edge weighted according to function values.
        float vertList[][] = new float[12][3];

        // Output array
        TFloatArrayList vertices = new TFloatArrayList();

        // Calculate maximal possible axis value (used in vertice normalization)
        float maxX = voxDim[0] * (volDim[0] - 1);
        float maxY = voxDim[1] * (volDim[1] - 1);
        float maxZ = voxDim[2] * (volZFull - 1);
        float maxAxisVal = 1;

        // Volume iteration
        for (int z = 0; z < volDim[2] - 1; z++) {
            for (int y = 0; y < volDim[1] - 1; y++) {
                for (int x = 0; x < volDim[0] - 1; x++) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px

                    int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
                            px = p + 1,
                            py = p + volDim[0],
                            pxy = py + 1,
                            pz = p + volDim[0] * volDim[1],
                            pxz = px + volDim[0] * volDim[1],
                            pyz = py + volDim[0] * volDim[1],
                            pxyz = pxy + volDim[0] * volDim[1];

                    //							  X              Y                    Z
                    float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};

                    // Voxel intensities
                    short value0 = values[p],
                            value1 = values[px],
                            value2 = values[py],
                            value3 = values[pxy],
                            value4 = values[pz],
                            value5 = values[pxz],
                            value6 = values[pyz],
                            value7 = values[pxyz];

                    // Voxel is active if its intensity is above isolevel
                    int cubeindex = 0;
                    if (value0 > isoLevel) cubeindex |= 1;
                    if (value1 > isoLevel) cubeindex |= 2;
                    if (value2 > isoLevel) cubeindex |= 8;
                    if (value3 > isoLevel) cubeindex |= 4;
                    if (value4 > isoLevel) cubeindex |= 16;
                    if (value5 > isoLevel) cubeindex |= 32;
                    if (value6 > isoLevel) cubeindex |= 128;
                    if (value7 > isoLevel) cubeindex |= 64;

                    // Fetch the triggered edges
                    int bits = MC_EDGE_TABLE[cubeindex];

                    // If no edge is triggered... skip
                    if (bits == 0) continue;

                    // Interpolate the positions based od voxel intensities
                    float mu = 0.5f;

                    // bottom of the cube
                    if ((bits & 1) != 0) {
                        mu = (isoLevel - value0) / (value1 - value0);
                        vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
                    }
                    if ((bits & 2) != 0) {
                        mu = (isoLevel - value1) / (value3 - value1);
                        vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 4) != 0) {
                        mu = (isoLevel - value2) / (value3 - value2);
                        vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 8) != 0) {
                        mu = (isoLevel - value0) / (value2 - value0);
                        vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    // top of the cube
                    if ((bits & 16) != 0) {
                        mu = (isoLevel - value4) / (value5 - value4);
                        vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 32) != 0) {
                        mu = (isoLevel - value5) / (value7 - value5);
                        vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 64) != 0) {
                        mu = (isoLevel - value6) / (value7 - value6);
                        vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 128) != 0) {
                        mu = (isoLevel - value4) / (value6 - value4);
                        vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    // vertical lines of the cube
                    if ((bits & 256) != 0) {
                        mu = (isoLevel - value0) / (value4 - value0);
                        vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 512) != 0) {
                        mu = (isoLevel - value1) / (value5 - value1);
                        vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 1024) != 0) {
                        mu = (isoLevel - value3) / (value7 - value3);
                        vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 2048) != 0) {
                        mu = (isoLevel - value2) / (value6 - value2);
                        vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }

                    // construct triangles -- get correct vertices from triTable.
                    int i = 0;
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex <<= 4;

                    while (MC_TRI_TABLE[cubeindex + i] != -1) {
                        int index1 = MC_TRI_TABLE[cubeindex + i];
                        int index2 = MC_TRI_TABLE[cubeindex + i + 1];
                        int index3 = MC_TRI_TABLE[cubeindex + i + 2];

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(new float[]{vertList[index3][0] / maxAxisVal - 0.5f, vertList[index3][1] / maxAxisVal - 0.5f, vertList[index3][2] / maxAxisVal - 0.5f});
                        vertices.add(new float[]{vertList[index2][0] / maxAxisVal - 0.5f, vertList[index2][1] / maxAxisVal - 0.5f, vertList[index2][2] / maxAxisVal - 0.5f});
                        vertices.add(new float[]{vertList[index1][0] / maxAxisVal - 0.5f, vertList[index1][1] / maxAxisVal - 0.5f, vertList[index1][2] / maxAxisVal - 0.5f});

                        i += 3;
                    }
                }
            }
        }

        return vertices.toArray();
    }

    public static float[] marchingCubesInt(int[] values, int[] volDim, int volZFull, float[] voxDim, int isoLevel, int offset) {

        TFloatArrayList vertices = new TFloatArrayList();
        // Actual position along edge weighted according to function values.
        float vertList[][] = new float[12][3];


        // Calculate maximal possible axis value (used in vertice normalization)
        float maxX = voxDim[0] * (volDim[0] - 1);
        float maxY = voxDim[1] * (volDim[1] - 1);
        float maxZ = voxDim[2] * (volZFull - 1);
        float maxAxisVal = 1;

        // Volume iteration
        for (int z = 0; z < volDim[2] - 1; z++) {
            for (int y = 0; y < volDim[1] - 1; y++) {
                for (int x = 0; x < volDim[0] - 1; x++) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px

                    int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
                            px = p + 1,
                            py = p + volDim[0],
                            pxy = py + 1,
                            pz = p + volDim[0] * volDim[1],
                            pxz = px + volDim[0] * volDim[1],
                            pyz = py + volDim[0] * volDim[1],
                            pxyz = pxy + volDim[0] * volDim[1];

                    //							  X              Y                    Z
                    float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};

                    // Voxel intensities
                    int value0 = values[p],
                            value1 = values[px],
                            value2 = values[py],
                            value3 = values[pxy],
                            value4 = values[pz],
                            value5 = values[pxz],
                            value6 = values[pyz],
                            value7 = values[pxyz];

                    // Voxel is active if its intensity is above isolevel
                    int cubeindex = 0;
                    if (value0 > isoLevel) cubeindex |= 1;
                    if (value1 > isoLevel) cubeindex |= 2;
                    if (value2 > isoLevel) cubeindex |= 8;
                    if (value3 > isoLevel) cubeindex |= 4;
                    if (value4 > isoLevel) cubeindex |= 16;
                    if (value5 > isoLevel) cubeindex |= 32;
                    if (value6 > isoLevel) cubeindex |= 128;
                    if (value7 > isoLevel) cubeindex |= 64;

                    // Fetch the triggered edges
                    int bits = MC_EDGE_TABLE[cubeindex];

                    // If no edge is triggered... skip
                    if (bits == 0) continue;

                    // Interpolate the positions based od voxel intensities
                    float mu = 0.5f;

                    // bottom of the cube
                    if ((bits & 1) != 0) {
                        mu = (isoLevel - value0) / (value1 - value0);
                        vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
                    }
                    if ((bits & 2) != 0) {
                        mu = (isoLevel - value1) / (value3 - value1);
                        vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 4) != 0) {
                        mu = (isoLevel - value2) / (value3 - value2);
                        vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 8) != 0) {
                        mu = (isoLevel - value0) / (value2 - value0);
                        vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    // top of the cube
                    if ((bits & 16) != 0) {
                        mu = (isoLevel - value4) / (value5 - value4);
                        vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 32) != 0) {
                        mu = (isoLevel - value5) / (value7 - value5);
                        vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 64) != 0) {
                        mu = (isoLevel - value6) / (value7 - value6);
                        vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 128) != 0) {
                        mu = (isoLevel - value4) / (value6 - value4);
                        vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    // vertical lines of the cube
                    if ((bits & 256) != 0) {
                        mu = (isoLevel - value0) / (value4 - value0);
                        vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 512) != 0) {
                        mu = (isoLevel - value1) / (value5 - value1);
                        vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 1024) != 0) {
                        mu = (isoLevel - value3) / (value7 - value3);
                        vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 2048) != 0) {
                        mu = (isoLevel - value2) / (value6 - value2);
                        vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }

                    // construct triangles -- get correct vertices from triTable.
                    int i = 0;
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex <<= 4;

                    while (MC_TRI_TABLE[cubeindex + i] != -1) {
                        int index1 = MC_TRI_TABLE[cubeindex + i];
                        int index2 = MC_TRI_TABLE[cubeindex + i + 1];
                        int index3 = MC_TRI_TABLE[cubeindex + i + 2];

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(new float[]{vertList[index3][0] / maxAxisVal - 0.5f, vertList[index3][1] / maxAxisVal - 0.5f, vertList[index3][2] / maxAxisVal - 0.5f,
                                vertList[index2][0] / maxAxisVal - 0.5f, vertList[index2][1] / maxAxisVal - 0.5f, vertList[index2][2] / maxAxisVal - 0.5f,
                                vertList[index1][0] / maxAxisVal - 0.5f, vertList[index1][1] / maxAxisVal - 0.5f, vertList[index1][2] / maxAxisVal - 0.5f});

                        i += 3;
                    }
                }
            }
        }

        return vertices.toArray();
    }

    public static float[] marchingCubesFloat(float[] values, int[] volDim, int volZFull, float[] voxDim, float isoLevel, int offset) {

        // Actual position along edge weighted according to function values.
        float vertList[][] = new float[12][3];

        // Output array
        TFloatArrayList vertices = new TFloatArrayList();

        // Calculate maximal possible axis value (used in vertice normalization)
        float maxX = voxDim[0] * (volDim[0] - 1);
        float maxY = voxDim[1] * (volDim[1] - 1);
        float maxZ = voxDim[2] * (volZFull - 1);
        float maxAxisVal = 1;

        // Volume iteration
        for (int z = 0; z < volDim[2] - 1; z++) {
            for (int y = 0; y < volDim[1] - 1; y++) {
                for (int x = 0; x < volDim[0] - 1; x++) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px

                    int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
                            px = p + 1,
                            py = p + volDim[0],
                            pxy = py + 1,
                            pz = p + volDim[0] * volDim[1],
                            pxz = px + volDim[0] * volDim[1],
                            pyz = py + volDim[0] * volDim[1],
                            pxyz = pxy + volDim[0] * volDim[1];

                    //							  X              Y                    Z
                    float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};

                    // Voxel intensities
                    float value0 = values[p],
                            value1 = values[px],
                            value2 = values[py],
                            value3 = values[pxy],
                            value4 = values[pz],
                            value5 = values[pxz],
                            value6 = values[pyz],
                            value7 = values[pxyz];

                    // Voxel is active if its intensity is above isolevel
                    int cubeindex = 0;
                    if (value0 > isoLevel) cubeindex |= 1;
                    if (value1 > isoLevel) cubeindex |= 2;
                    if (value2 > isoLevel) cubeindex |= 8;
                    if (value3 > isoLevel) cubeindex |= 4;
                    if (value4 > isoLevel) cubeindex |= 16;
                    if (value5 > isoLevel) cubeindex |= 32;
                    if (value6 > isoLevel) cubeindex |= 128;
                    if (value7 > isoLevel) cubeindex |= 64;

                    // Fetch the triggered edges
                    int bits = MC_EDGE_TABLE[cubeindex];

                    // If no edge is triggered... skip
                    if (bits == 0) continue;

                    // Interpolate the positions based od voxel intensities
                    float mu = 0.5f;

                    // bottom of the cube
                    if ((bits & 1) != 0) {
                        mu = (float) ((isoLevel - value0) / (value1 - value0));
                        vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
                    }
                    if ((bits & 2) != 0) {
                        mu = (float) ((isoLevel - value1) / (value3 - value1));
                        vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 4) != 0) {
                        mu = (float) ((isoLevel - value2) / (value3 - value2));
                        vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 8) != 0) {
                        mu = (float) ((isoLevel - value0) / (value2 - value0));
                        vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    // top of the cube
                    if ((bits & 16) != 0) {
                        mu = (float) ((isoLevel - value4) / (value5 - value4));
                        vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 32) != 0) {
                        mu = (float) ((isoLevel - value5) / (value7 - value5));
                        vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 64) != 0) {
                        mu = (float) ((isoLevel - value6) / (value7 - value6));
                        vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 128) != 0) {
                        mu = (float) ((isoLevel - value4) / (value6 - value4));
                        vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    // vertical lines of the cube
                    if ((bits & 256) != 0) {
                        mu = (float) ((isoLevel - value0) / (value4 - value0));
                        vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 512) != 0) {
                        mu = (float) ((isoLevel - value1) / (value5 - value1));
                        vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 1024) != 0) {
                        mu = (float) ((isoLevel - value3) / (value7 - value3));
                        vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 2048) != 0) {
                        mu = (float) ((isoLevel - value2) / (value6 - value2));
                        vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }

                    // construct triangles -- get correct vertices from triTable.
                    int i = 0;
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex <<= 4;

                    while (MC_TRI_TABLE[cubeindex + i] != -1) {
                        int index1 = MC_TRI_TABLE[cubeindex + i];
                        int index2 = MC_TRI_TABLE[cubeindex + i + 1];
                        int index3 = MC_TRI_TABLE[cubeindex + i + 2];

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(new float[]{vertList[index3][0] / maxAxisVal - 0.5f, vertList[index3][1] / maxAxisVal - 0.5f, vertList[index3][2] / maxAxisVal - 0.5f,
                                vertList[index2][0] / maxAxisVal - 0.5f, vertList[index2][1] / maxAxisVal - 0.5f, vertList[index2][2] / maxAxisVal - 0.5f,
                                vertList[index1][0] / maxAxisVal - 0.5f, vertList[index1][1] / maxAxisVal - 0.5f, vertList[index1][2] / maxAxisVal - 0.5f});

                        i += 3;
                    }
                }
            }
        }

        return vertices.toArray();
    }

    public static float[] marchingCubesDouble(double[] values, int[] volDim, int volZFull, float[] voxDim, double isoLevel, int offset) {

        // Actual position along edge weighted according to function values.
        float vertList[][] = new float[12][3];

        // Output array
        TFloatArrayList vertices = new TFloatArrayList();

        // Calculate maximal possible axis value (used in vertice normalization)
        float maxX = voxDim[0] * (volDim[0] - 1);
        float maxY = voxDim[1] * (volDim[1] - 1);
        float maxZ = voxDim[2] * (volZFull - 1);
        float maxAxisVal = 1;

        // Volume iteration
        for (int z = 0; z < volDim[2] - 1; z++) {
            for (int y = 0; y < volDim[1] - 1; y++) {
                for (int x = 0; x < volDim[0] - 1; x++) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px

                    int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
                            px = p + 1,
                            py = p + volDim[0],
                            pxy = py + 1,
                            pz = p + volDim[0] * volDim[1],
                            pxz = px + volDim[0] * volDim[1],
                            pyz = py + volDim[0] * volDim[1],
                            pxyz = pxy + volDim[0] * volDim[1];

                    //							  X              Y                    Z
                    float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};

                    // Voxel intensities
                    double value0 = values[p],
                            value1 = values[px],
                            value2 = values[py],
                            value3 = values[pxy],
                            value4 = values[pz],
                            value5 = values[pxz],
                            value6 = values[pyz],
                            value7 = values[pxyz];

                    // Voxel is active if its intensity is above isolevel
                    int cubeindex = 0;
                    if (value0 > isoLevel) cubeindex |= 1;
                    if (value1 > isoLevel) cubeindex |= 2;
                    if (value2 > isoLevel) cubeindex |= 8;
                    if (value3 > isoLevel) cubeindex |= 4;
                    if (value4 > isoLevel) cubeindex |= 16;
                    if (value5 > isoLevel) cubeindex |= 32;
                    if (value6 > isoLevel) cubeindex |= 128;
                    if (value7 > isoLevel) cubeindex |= 64;

                    // Fetch the triggered edges
                    int bits = MC_EDGE_TABLE[cubeindex];

                    // If no edge is triggered... skip
                    if (bits == 0) continue;

                    // Interpolate the positions based od voxel intensities
                    float mu = 0.5f;

                    // bottom of the cube
                    if ((bits & 1) != 0) {
                        mu = (float) ((isoLevel - value0) / (value1 - value0));
                        vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
                    }
                    if ((bits & 2) != 0) {
                        mu = (float) ((isoLevel - value1) / (value3 - value1));
                        vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 4) != 0) {
                        mu = (float) ((isoLevel - value2) / (value3 - value2));
                        vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    if ((bits & 8) != 0) {
                        mu = (float) ((isoLevel - value0) / (value2 - value0));
                        vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
                    }
                    // top of the cube
                    if ((bits & 16) != 0) {
                        mu = (float) ((isoLevel - value4) / (value5 - value4));
                        vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 32) != 0) {
                        mu = (float) ((isoLevel - value5) / (value7 - value5));
                        vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 64) != 0) {
                        mu = (float) ((isoLevel - value6) / (value7 - value6));
                        vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 128) != 0) {
                        mu = (float) ((isoLevel - value4) / (value6 - value4));
                        vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    // vertical lines of the cube
                    if ((bits & 256) != 0) {
                        mu = (float) ((isoLevel - value0) / (value4 - value0));
                        vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 512) != 0) {
                        mu = (float) ((isoLevel - value1) / (value5 - value1));
                        vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 1024) != 0) {
                        mu = (float) ((isoLevel - value3) / (value7 - value3));
                        vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }
                    if ((bits & 2048) != 0) {
                        mu = (float) ((isoLevel - value2) / (value6 - value2));
                        vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
                    }

                    // construct triangles -- get correct vertices from triTable.
                    int i = 0;
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex <<= 4;

                    while (MC_TRI_TABLE[cubeindex + i] != -1) {
                        int index1 = MC_TRI_TABLE[cubeindex + i];
                        int index2 = MC_TRI_TABLE[cubeindex + i + 1];
                        int index3 = MC_TRI_TABLE[cubeindex + i + 2];

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(new float[]{vertList[index3][0] / maxAxisVal - 0.5f, vertList[index3][1] / maxAxisVal - 0.5f, vertList[index3][2] / maxAxisVal - 0.5f,
                                vertList[index2][0] / maxAxisVal - 0.5f, vertList[index2][1] / maxAxisVal - 0.5f, vertList[index2][2] / maxAxisVal - 0.5f,
                                vertList[index1][0] / maxAxisVal - 0.5f, vertList[index1][1] / maxAxisVal - 0.5f, vertList[index1][2] / maxAxisVal - 0.5f});

                        i += 3;
                    }
                }
            }
        }

        return vertices.toArray();
    }

    public static float[] marchingCubes(ImagePlus img, int channel, int frame, double isoLevel, int offset, boolean physicalSizes, boolean forceMeshLengthUnit, Quantity.LengthUnit meshLengthUnit) {

        float voxDimX = 1;
        float voxDimY = 1;
        float voxDimZ = 1;

        if (physicalSizes) {
            voxDimX = (float) img.getCalibration().pixelWidth;
            voxDimY = (float) img.getCalibration().pixelHeight;
            voxDimZ = (float) img.getCalibration().pixelDepth;
            if (forceMeshLengthUnit) {
                Quantity inputQuantity = new Quantity(1, img.getCalibration().getUnits());
                Quantity outputQuantity = inputQuantity.convertTo(meshLengthUnit.toString());
                voxDimX *= outputQuantity.getValue();
                voxDimY *= outputQuantity.getValue();
                voxDimZ *= outputQuantity.getValue();
            }
        }

        int[] volDim = new int[]{img.getWidth(), img.getHeight(), img.getNSlices()};
        float[] voxDim = new float[]{voxDimX, voxDimY, voxDimZ};
        if (img.getType() == ImagePlus.GRAY8) {
            byte[] pixels = new byte[img.getWidth() * img.getHeight() * img.getNSlices()];
            int sz = img.getWidth() * img.getHeight();

            for (int z = 0; z < img.getNSlices(); z++) {
                ImageProcessor ip = ImageJUtils.getSliceZero(img, channel, z, frame);
                byte[] ipPixels = (byte[]) ip.getPixels();
                for (int i = 0; i < ipPixels.length; i++) {
                    pixels[i + z * sz] = ipPixels[i];
                }
            }

            return marchingCubesByte(pixels, volDim, volDim[2], voxDim, (char) isoLevel, offset);
        } else if (img.getType() == ImagePlus.GRAY16) {
            short[] pixels = new short[img.getWidth() * img.getHeight() * img.getNSlices()];

            int sz = img.getWidth() * img.getHeight();

            for (int z = 0; z < img.getNSlices(); z++) {
                ImageProcessor ip = ImageJUtils.getSliceZero(img, channel, z, frame);
                short[] ipPixels = (short[]) ip.getPixels();
                for (int i = 0; i < ipPixels.length; i++) {
                    pixels[i + z * sz] = ipPixels[i];
                }
            }

            return marchingCubesShort(pixels, volDim, volDim[2], voxDim, (short) isoLevel, offset);
        } else if (img.getType() == ImagePlus.GRAY32) {
            float[] pixels = new float[img.getWidth() * img.getHeight() * img.getNSlices()];

            int sz = img.getWidth() * img.getHeight();

            for (int z = 0; z < img.getNSlices(); z++) {
                ImageProcessor ip = ImageJUtils.getSliceZero(img, channel, z, frame);
                float[] ipPixels = (float[]) ip.getPixels();
                for (int i = 0; i < ipPixels.length; i++) {
                    pixels[i + z * sz] = ipPixels[i];
                }
            }

            return marchingCubesFloat(pixels, volDim, volDim[2], voxDim, (float) isoLevel, offset);
        } else {
            throw new UnsupportedOperationException("Unsupported image type!");
        }
    }
}
