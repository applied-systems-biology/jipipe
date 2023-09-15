package org.hkijena.jipipe.extensions.ilastik.utils.hdf5;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import hdf.hdf5lib.exceptions.HDF5Exception;
import org.hkijena.jipipe.extensions.ilastik.utils.Hdf5Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scijava.log.LogService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * From ilastik4ij
 *
 * MIT License
 *
 * Copyright (c) 2017 ilastik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class HDF5DatasetEntryProvider implements DatasetEntryProvider {
    private final LogService logService;
    static class InvalidAxisTagsException extends RuntimeException {}

    public HDF5DatasetEntryProvider(LogService logService) {
        this.logService = logService;
    }

    @Override
    public List<DatasetEntry> findAvailableDatasets(String path) {
        try {
            return this.findAvailableDatasets(path, "/");
        } catch (HDF5Exception e) {
            throw new ReadException(e.getMessage(), e);
        }
    }

    private DatasetEntry getDatasetEntry(String internalPath, IHDF5Reader reader) {
        HDF5DataSetInformation hdf5DatasetInfo = reader.object().getDataSetInformation(internalPath);
        int dsRank = hdf5DatasetInfo.getRank();
        String axisTags = defaultAxisOrder(dsRank);
        logService.info("Detected internal path " + internalPath);

        try {
            String axisTagsJSON = reader.string().getAttr(internalPath, "axistags");
            axisTags = parseAxisTags(axisTagsJSON);
            logService.debug("Detected axistags " + axisTags + " in dataset " + internalPath);
        } catch (HDF5AttributeException e) {
            logService.debug("No axistags attribute in dataset");
        } catch (InvalidAxisTagsException e) {
            logService.debug("Invalid axistags attribute in dataset");
        }

        return new DatasetEntry(internalPath, dsRank, axisTags, makeVerboseName(internalPath, hdf5DatasetInfo));
    }

    private static String makeVerboseName(String internalPath, HDF5DataSetInformation dsInfo) {
        String shape = Arrays.stream(dsInfo.getDimensions())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        String dtype = Hdf5Utils.getTypeInfo(dsInfo);
        return String.format("%s: (%s) %s", internalPath, shape, dtype);
    }

    private static String defaultAxisOrder(int rank) {
        // Uses ilastik default axis order,
        // see https://github.com/ilastik/ilastik/blob/a1bb868b0a8d43ac3c89e681cc89d43be9591ff7/lazyflow/utility/helpers.py#L107
        switch (rank) {
            case 5:
                return "tzyxc";
            case 4:
                return "zyxc";
            case 3:
                return "zyx";
            default:
                return "yx";
        }
    }

    private List<DatasetEntry> findAvailableDatasets(String path, String intenalPath) {
        List<DatasetEntry> result = new ArrayList<>();
        logService.info("Trying to open: " + path);

        try (IHDF5Reader reader = HDF5Factory.openForReading(path)) {
            HDF5LinkInformation link = reader.object().getLinkInformation(intenalPath);
            List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);

            for (HDF5LinkInformation linkInfo : members) {
                switch (linkInfo.getType()) {
                    case DATASET:
                        DatasetEntry datasetEntry = getDatasetEntry(linkInfo.getPath(), reader);
                        if (datasetEntry.rank >= 2) {
                            result.add(datasetEntry);
                        }
                        break;
                    case GROUP:
                        result.addAll(findAvailableDatasets(path, linkInfo.getPath()));
                        break;
                }
            }
        }

        return result;
    }

    private static String parseAxisTags(String jsonString) throws InvalidAxisTagsException {
        try {
            JSONObject axisObject = new JSONObject(jsonString);
            JSONArray axesArray = axisObject.getJSONArray("axes");
            StringBuilder axisTags = new StringBuilder();

            for (int i = 0; i < axesArray.length(); i++) {
                JSONObject axisEntry = axesArray.getJSONObject(i);
                String axisTag = axisEntry.getString("key");

                axisTags.append(axisTag);
            }

            return axisTags.toString();
        } catch (JSONException e) {
            throw new InvalidAxisTagsException();
        }
    }

}
