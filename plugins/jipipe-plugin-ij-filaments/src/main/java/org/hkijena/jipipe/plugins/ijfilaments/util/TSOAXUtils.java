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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TSOAXUtils {

    public static ResultsTableData parseSnakesAsTable(Path resultsFile, Multimap<Integer, Integer> snakeToTrackIdMap, JIPipeProgressInfo progressInfo) {

        progressInfo.log("Reading snakes into table ...");

        ResultsTableData result = new ResultsTableData();
        result.addNumericColumn("frame");
        result.addNumericColumn("snake_type");
        result.addNumericColumn("snake_index");
        result.addStringColumn("track_ids");
        result.addNumericColumn("point_index");
        result.addNumericColumn("x");
        result.addNumericColumn("y");
        result.addNumericColumn("z");
        result.addNumericColumn("intensity");
        try (BufferedReader reader = Files.newBufferedReader(resultsFile)) {
            String line;
            boolean inParameters = true;
            int snakeType = 0;
            int frame = 0;
            progressInfo.log("Current frame is " + frame);
            do {
                line = reader.readLine();
                if (!StringUtils.isNullOrEmpty(line)) {
                    if (inParameters) {
                        if (line.startsWith("#")) {
                            // Reconfigure the snake type
                            snakeType = Integer.parseInt(line.substring(1));

                            // We are not in parameters anymore
                            inParameters = false;
                        } else {
//                            progressInfo.log("Skipping line " + line);
                        }
                    } else if (line.startsWith("#")) {
                        // Reconfigure the snake type
                        snakeType = Integer.parseInt(line.substring(1));
                    } else if (line.startsWith("[")) {
//                        progressInfo.log("Skipping line " + line);
                    } else if (line.startsWith("$")) {
                        // Reconfigure frame
                        frame = Integer.parseInt(line.split(" ")[1]) + 1;
                        progressInfo.log("Current frame is " + frame);
                    } else if (line.startsWith("Tracks")) {
                        progressInfo.log("Reached track marker. Stopping parsing.");
                        break;
                    } else {
                        if (StringUtils.isNullOrEmpty(line.trim())) {
                            continue;
                        }
                        String[] items = line.trim().split("\\s+");
                        int snakeId = Integer.parseInt(items[0]);
                        result.addAndModifyRow()
                                .set("frame", frame)
                                .set("snake_type", snakeType)
                                .set("snake_index", snakeId)
                                .set("track_ids", snakeToTrackIdMap.get(snakeId).stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")))
                                .set("point_index", Integer.parseInt(items[1]))
                                .set("x", Double.parseDouble(items[2]))
                                .set("y", Double.parseDouble(items[3]))
                                .set("z", Double.parseDouble(items[4]))
                                .set("intensity", Double.parseDouble(items[5]))
                                .build();
                    }
                }
            }
            while (line != null);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Filaments3DData extractFilaments(ResultsTableData snakesResult, int trackId, boolean enableTrackIdFilter, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments3DData = new Filaments3DData();

        // Create snake vertices
        progressInfo.log("Creating " + snakesResult.getRowCount() + " vertices ...");
        Map<Integer, Integer> snakeTypes = new HashMap<>();
        Map<Integer, List<FilamentVertex>> snakeVertices = new HashMap<>();
        for (int row = 0; row < snakesResult.getRowCount(); row++) {
            int frame = (int) snakesResult.getValueAsDouble(row, "frame");
            int snakeIndex = (int) snakesResult.getValueAsDouble(row, "snake_index");
            int snakeType = (int) snakesResult.getValueAsDouble(row, "snake_type");
            int pointIndex = (int) snakesResult.getValueAsDouble(row, "point_index");
            double x = snakesResult.getValueAsDouble(row, "x");
            double y = snakesResult.getValueAsDouble(row, "y");
            double z = snakesResult.getValueAsDouble(row, "z");
            double value = snakesResult.getValueAsDouble(row, "intensity");
            String trackIds = StringUtils.nullToEmpty(snakesResult.getValueAsString(row, "track_ids")).trim();

            if (enableTrackIdFilter) {
                if (StringUtils.isNullOrEmpty(trackIds)) {
                    if (trackId != -1) {
                        // ID=-1 maps to no tracks
                        continue;
                    }
                } else if (trackIds.contains(",")) {
                    boolean found = false;
                    for (String s : trackIds.split(",")) {
                        int id = Integer.parseInt(s.trim());
                        if (id == trackId) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                } else {
                    int rowId = Integer.parseInt(trackIds);
                    if (rowId != trackId) {
                        continue;
                    }
                }
            }

            FilamentVertex vertex = new FilamentVertex();
            vertex.setValue(value);
            vertex.setSpatialLocation(new Point3d(x, y, z));
            vertex.setNonSpatialLocation(new NonSpatialPoint3d(0, frame));
            vertex.getMetadata().put("snake_index", String.valueOf(snakeIndex));
            vertex.getMetadata().put("snake_type", String.valueOf(snakeType));
            vertex.getMetadata().put("point_index", String.valueOf(pointIndex));
            filaments3DData.addVertex(vertex);

            snakeTypes.put(snakeIndex, snakeType);
            List<FilamentVertex> vertexList = snakeVertices.getOrDefault(snakeIndex, null);
            if (vertexList == null) {
                vertexList = new ArrayList<>();
                snakeVertices.put(snakeIndex, vertexList);
            }
            vertexList.add(vertex);
        }

        // Connect vertices
        // 0 = closed, 1 = open
        progressInfo.log("Connecting vertices ...");
        for (Map.Entry<Integer, List<FilamentVertex>> entry : snakeVertices.entrySet()) {
            List<FilamentVertex> vertexList = entry.getValue();
            for (int i = 1; i < vertexList.size(); i++) {
                filaments3DData.addEdge(vertexList.get(i - 1), vertexList.get(i));
            }
            if (vertexList.size() > 1 && snakeTypes.get(entry.getKey()) == 0) {
                filaments3DData.addEdge(vertexList.get(vertexList.size() - 1), vertexList.get(0));
            }
        }

        return filaments3DData;
    }

    public static List<List<Integer>> parseSnakesTracks(Path resultsFile, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Reading snakes tracks ...");
        List<List<Integer>> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(resultsFile)) {
            boolean isParsingTracks = false;
            String line;
            do {
                line = reader.readLine();
                if (!StringUtils.isNullOrEmpty(line)) {
                    if (line.startsWith("Tracks")) {
                        isParsingTracks = true;
                    } else if (isParsingTracks) {
                        if (StringUtils.isNullOrEmpty(line.trim())) {
                            continue;
                        }
                        String[] items = line.trim().split("\\s+");
                        List<Integer> snakeIds = new ArrayList<>();
                        for (String item : items) {
                            snakeIds.add(Integer.parseInt(item));
                        }
                        result.add(snakeIds);
                    }
                }
            }
            while (line != null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Detected " + result.size() + " tracks");

        return result;
    }

    public static Multimap<Integer, Integer> assignSnakesIdsToTrackIds(List<List<Integer>> tracks) {
        Multimap<Integer, Integer> snakeToTrackIdMap = HashMultimap.create();
        for (int i = 0; i < tracks.size(); i++) {
            List<Integer> track = tracks.get(i);
            for (Integer snakeId : track) {
                snakeToTrackIdMap.put(snakeId, i);
            }
        }
        return snakeToTrackIdMap;
    }

    public static Set<Integer> findTrackIds(ResultsTableData snakesResult) {
        TableColumn trackIdsColumn = snakesResult.getColumnReference("track_ids");
        Set<Integer> knownTrackIds = new HashSet<>();
        for (int i = 0; i < trackIdsColumn.getRows(); i++) {
            String rowAsString = StringUtils.nullToEmpty(trackIdsColumn.getRowAsString(i)).trim();
            if (StringUtils.isNullOrEmpty(rowAsString)) {
                knownTrackIds.add(-1);
            } else if (rowAsString.contains(",")) {
                for (String s : rowAsString.split(",")) {
                    knownTrackIds.add(Integer.parseInt(s.trim()));
                }
            } else {
                knownTrackIds.add(Integer.parseInt(rowAsString));
            }
        }
        return knownTrackIds;
    }
}
