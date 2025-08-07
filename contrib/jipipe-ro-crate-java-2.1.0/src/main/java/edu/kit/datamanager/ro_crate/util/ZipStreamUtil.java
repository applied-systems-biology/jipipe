package edu.kit.datamanager.ro_crate.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;

/**
 *
 * @author jejkal
 */
public class ZipStreamUtil {

    /**
     * Adds a folder and its contents to a ZipOutputStream.
     *
     * @param zipOutputStream The ZipOutputStream to which the folder will be added.
     * @param folder The folder to be added.
     * @param parentPath The path in the zip file where the folder will be added.
     * @throws IOException If an I/O error occurs.
     */
    public static void addFolderToZipStream(
            ZipOutputStream zipOutputStream,
            File folder,
            String parentPath
    ) throws IOException {
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException(
                    "The provided folder path is not a valid directory: %s"
                            .formatted(folder.getAbsolutePath())
            );
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String zipEntryPath = parentPath.isEmpty() ? file.getName() : parentPath + "/" + file.getName();
            if (file.isDirectory()) {
                addFolderToZipStream(zipOutputStream, file.getAbsolutePath(), zipEntryPath);
            } else {
                addFileToZipStream(zipOutputStream, file, zipEntryPath);
            }
        }
    }

    /**
     * Adds a folder and its contents to a ZipOutputStream.
     * @param zipOutputStream The ZipOutputStream to which the folder will be added.
     * @param folderPath The path of the folder to be added.
     * @param parentPath The path in the zip file where the folder will be added.
     * @throws IOException If an I/O error occurs.
     */
    public static void addFolderToZipStream(
            ZipOutputStream zipOutputStream,
            String folderPath,
            String parentPath
    ) throws IOException {
        addFolderToZipStream(zipOutputStream, new File(folderPath), parentPath);
    }

    /**
     * Adds a file to a ZipOutputStream.
     *
     * @param zipOutputStream The ZipOutputStream to which the file will be added.
     * @param file The file to be added.
     * @param zipEntryPath The path in the zip file where the file will be added.
     * @throws IOException If an I/O error occurs.
     */
    public static void addFileToZipStream(
            ZipOutputStream zipOutputStream,
            File file,
            String zipEntryPath
    ) throws IOException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setFileNameInZip(zipEntryPath);
        zipOutputStream.putNextEntry(zipParameters);

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, len);
            }
        }

        zipOutputStream.closeEntry();
    }
}
