package org.hkijena.acaq5.extensions.filesystem.compat;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.utils.ReflectionUtils;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows importing of path data from within ImageJ
 * The main 'ImageJ' data type is {@link File}, but this also allows conversion from {@link String}, {@link Path} and {@link ResultsTable}.
 */
public class PathDataImageJAdapter implements ImageJDatatypeAdapter {

    private Class<? extends PathData> acaqDataClass;

    /**
     * Creates a new instance
     * @param acaqDataClass the exact data class that is imported
     */
    public PathDataImageJAdapter(Class<? extends PathData> acaqDataClass) {
        this.acaqDataClass = acaqDataClass;
    }

    @Override
    public boolean canConvertImageJToACAQ(Object imageJData) {
        return false;
    }

    @Override
    public boolean canConvertACAQToImageJ(ACAQData acaqData) {
        return false;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return File.class;
    }

    @Override
    public Class<? extends ACAQData> getACAQDatatype() {
        return acaqDataClass;
    }

    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        if(imageJData instanceof String) {
            return (ACAQData) ReflectionUtils.newInstance(acaqDataClass, Paths.get((String) imageJData));
        }
        else if(imageJData instanceof File) {
            return (ACAQData) ReflectionUtils.newInstance(acaqDataClass, ((File) imageJData).toPath());
        }
        else if(imageJData instanceof Path) {
            return (ACAQData) ReflectionUtils.newInstance(acaqDataClass, imageJData);
        }
        else if(imageJData instanceof ResultsTable) {
            ResultsTableData resultsTableData = new ResultsTableData((ResultsTable)imageJData);
            return (ACAQData) ReflectionUtils.newInstance(acaqDataClass, Paths.get(resultsTableData.getValueAsString(0,0)));
        }
        else {
            throw new IllegalArgumentException("Cannot convert to " + acaqDataClass + ": " + imageJData);
        }
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate, boolean noWindow, String windowName) {
        PathData data = (PathData)acaqData;
        ResultsTableData resultsTableData = new ResultsTableData();
        resultsTableData.addColumn("Path", true);
        resultsTableData.addRow();
        resultsTableData.setValueAt(data.getPath().toString(), 0, 0);
        if(activate && !noWindow) {
            resultsTableData.getTable().show(windowName);
        }
        return data.getPath().toFile();
    }

    @Override
    public List<Object> convertMultipleACAQToImageJ(List<ACAQData> acaqData, boolean activate, boolean noWindow, String windowName) {
        ResultsTableData resultsTableData = new ResultsTableData();
        resultsTableData.addColumn("Path", true);
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < acaqData.size(); ++i)  {
            PathData data = (PathData) acaqData.get(i);
            result.add(data.getPath());

            resultsTableData.addRow();
            resultsTableData.setValueAt(data.getPath() + "", i, 0);
        }
        if(activate && !noWindow) {
            resultsTableData.getTable().show(windowName);
        }
        return result;
    }

    @Override
    public ACAQData importFromImageJ(String parameters) {
        return (ACAQData) ReflectionUtils.newInstance(acaqDataClass, Paths.get(parameters));
    }
}
