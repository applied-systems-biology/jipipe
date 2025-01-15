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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import gnu.trove.list.array.TIntArrayList;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;

public class LabelConnectedComponents {
    private LabelConnectedComponents() {

    }

    public static ImageProcessor process(ImageProcessor ip, Neighborhood2D connectivity) {

        int width = ip.getWidth();
        int height = ip.getHeight();

        // Output image for labeled components
        ImageProcessor output;
        if (ip instanceof ShortProcessor) {
            output = new ShortProcessor(width, height);
        } else {
            output = new FloatProcessor(width, height);
        }
        int[] newLabels = new int[width * height];
        int nextLabel = 1;
        TIntArrayList neighborhoodNewDifferentLabels = new TIntArrayList(8);

        // Find the neighborhoods and track them
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int currentLabel = (int) ip.getf(x, y);
                if (currentLabel <= 0) {
                    continue;
                }

                int currentNewLabel = newLabels[y * width + x];
                if (currentNewLabel == 0) {
                    // Hitting this label the first time, so we assign it
                    currentNewLabel = nextLabel++;
                    newLabels[y * width + x] = currentNewLabel;
                }

                neighborhoodNewDifferentLabels.resetQuick();
                boolean containsUnassignedNeighbors = false;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;

                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        if (connectivity == Neighborhood2D.FourConnected && dx == dy) {
                            continue;
                        }
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                            continue;
                        }

                        final int otherLabel = (int) ip.getf(nx, ny);
                        if (otherLabel <= 0) {
                            continue;
                        }

                        if (currentLabel == otherLabel) {
                            // We track which new assignments may be already in the neighborhood
                            int otherNewLabel = newLabels[ny * width + nx];
                            if (otherNewLabel > 0) {
                                if (otherNewLabel != currentNewLabel) {
                                    neighborhoodNewDifferentLabels.add(otherNewLabel);
                                }
                            } else {
                                // For later meaning that we need to override all values
                                containsUnassignedNeighbors = true;
                            }
                        }
                    }
                }

                final int currentNewLabel_ = currentNewLabel;
                boolean writeAssignments = false;

                if (!neighborhoodNewDifferentLabels.isEmpty()) {
                    // Find a new label
                    for (int i = 0; i < neighborhoodNewDifferentLabels.size(); i++) {
                        currentNewLabel = Math.min(currentNewLabel, neighborhoodNewDifferentLabels.get(i));
                    }

                    // Apply label renaming for the current one
                    for (int i = 0; i < newLabels.length; i++) {
                        if (newLabels[i] == currentNewLabel_) {
                            newLabels[i] = currentNewLabel;
                        }
                    }

                    // Apply label renaming for the neighbors
                    for (int j = 0; j < neighborhoodNewDifferentLabels.size(); j++) {
                        int neighborNewLabel = neighborhoodNewDifferentLabels.get(j);

                        for (int i = 0; i < newLabels.length; i++) {
                            if (newLabels[i] == neighborNewLabel) {
                                newLabels[i] = currentNewLabel;
                            }
                        }
                    }
                } else {
                    if (containsUnassignedNeighbors) {
                        // We need to ensure that all the neighbors have the correct label
                        writeAssignments = true;
                    } else {
                        // Nothing to do
                    }
                }

                if (containsUnassignedNeighbors) {
                    // Explicitly write into the neighborhood
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int ny = y + dy;
                            int nx = x + dx;

                            if (dx == 0 && dy == 0) {
                                continue;
                            }
                            if (connectivity == Neighborhood2D.FourConnected && dx == dy) {
                                continue;
                            }
                            if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                                continue;
                            }
                            final int otherLabel = (int) ip.getf(nx, ny);
                            if (otherLabel <= 0) {
                                continue;
                            }
                            if (currentLabel == otherLabel) {
                                newLabels[nx + width * ny] = currentNewLabel;
                            }
                        }
                    }
                }
            }
        }

        // Copy the labels into the output
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newLabel = newLabels[x + y * width];
                output.setf(x, y, newLabel);
            }
        }

        // Remap labels
        LabelImages.remapLabels(output);

        return output;
    }
}
