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

package org.hkijena.jipipe.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveUtils {

    private static final int BUFFER_SIZE = 8192;

    public static void zipFileOrDirectory(Path fileToZip, Path zipFile, JIPipeProgressInfo progressInfo) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipFile(fileToZip, fileToZip.getFileName().toString(), zipOut, progressInfo);
        }
    }

    public static void zipDirectory(Path rootPath, String rootPathName, Path zipFile, JIPipeProgressInfo progressInfo) throws IOException {
        if (!Files.isDirectory(rootPath)) {
            throw new IOException("Path is not a directory");
        }
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipFile(rootPath, rootPathName, zipOut, progressInfo);
        }
    }

    private static void zipFile(Path fileToZip, String fileName, ZipOutputStream zipOut, JIPipeProgressInfo progressInfo) throws IOException {
        if (Files.isHidden(fileToZip)) {
            return;
        }
        if (Files.isDirectory(fileToZip)) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            List<Path> children = Files.list(fileToZip).collect(Collectors.toList());
            for (Path childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getFileName(), zipOut, progressInfo);
            }
            return;
        }
        progressInfo.log("ZIP " + fileToZip + " -> " + fileName);
        try (FileInputStream fis = new FileInputStream(fileToZip.toFile())) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }

    /**
     * Unzips a file
     *
     * @param zipFile      the zip file
     * @param targetDir    the target dir
     * @param progressInfo the progress info
     * @throws IOException io exception
     */
    public static void decompressZipFile(Path zipFile, Path targetDir, JIPipeProgressInfo progressInfo) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            if (zipEntry.isDirectory()) {
                File newDirectory = decompressZipFileNewFile(targetDir.toFile(), zipEntry);
                progressInfo.log(newDirectory.toString());
                if (!Files.isDirectory(newDirectory.toPath()))
                    Files.createDirectories(newDirectory.toPath());
            } else {
                File newFile = decompressZipFileNewFile(targetDir.toFile(), zipEntry);
                progressInfo.log(newFile.toString());
                if (!Files.isDirectory(newFile.toPath().getParent()))
                    Files.createDirectories(newFile.toPath().getParent());
                if (Files.exists(newFile.toPath())) {
                    Files.delete(newFile.toPath());
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File decompressZipFileNewFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void decompressTarGZ(Path zipFile, Path targetDir, JIPipeProgressInfo progressInfo) throws IOException {
        try (FileInputStream in = new FileInputStream(zipFile.toFile())) {
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
                TarArchiveEntry entry;

                Map<Path, Path> createdLinks = new HashMap<>();

                while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                    String entryName = StringUtils.nullTerminate(entry.getName());

                    Path entryOutputPath = targetDir.resolve(Paths.get(entryName));
                    progressInfo.log("Entry " + entryName + " -> " + entryOutputPath);

                    if (entry.isSymbolicLink() || entry.isLink()) {
                        Path linkName = Paths.get(StringUtils.nullTerminate(entry.getLinkName()));
                        if (!linkName.isAbsolute()) {
                            linkName = entryOutputPath.getParent().resolve(linkName);
                        }
                        // Needs to be deferred
                        createdLinks.put(linkName, entryOutputPath);
                    } else if (entry.isDirectory()) {
                        Files.createDirectories(entryOutputPath);
                    } else if (entry.isFile()) {

                        // Ensure that parent directories exist
                        Files.createDirectories(entryOutputPath.getParent());

                        int count;
                        byte[] data = new byte[BUFFER_SIZE];
                        try (FileOutputStream fos = new FileOutputStream(entryOutputPath.toFile(), false)) {
                            try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                                while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                                    dest.write(data, 0, count);
                                }
                            }
                        }
                    } else {
                        progressInfo.log("Unsupported entry: " + entryName);
                    }
                }

                for (Map.Entry<Path, Path> pathEntry : createdLinks.entrySet()) {
                    Path linkName = pathEntry.getKey();
                    Path entryOutputPath = pathEntry.getValue();
                    progressInfo.log("Linking " + entryOutputPath + " -> " + linkName);
                    Files.createDirectories(entryOutputPath.getParent());
                    Files.createSymbolicLink(entryOutputPath, linkName);
                }

            }
        }
    }

    /**
     * Returns the contents of the specified entry inside an archive (ZIP or tar.gz)
     * as a byte array, or null if the entry is not found.
     *
     * @param archivePath  the path to the archive file
     * @param internalPath the path to the entry within the archive
     * @return the entry’s data as a byte array, or null if not found
     * @throws IOException if there is an error reading or processing the archive
     */
    public static byte[] decompressArchiveEntry(Path archivePath, Path internalPath) throws IOException {
        // Normalize the entry name to use forward slashes (as is common in archives)
        String targetEntryName = internalPath.toString().replace('\\', '/');
        // Get the archive file name in lower-case for extension checking
        String fileName = archivePath.getFileName().toString().toLowerCase(Locale.ROOT);

        try (InputStream fis = Files.newInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            // Wrap the stream in a Gzip decompressor if the file is a tar.gz (or .tgz)
            InputStream decompressedStream = (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz"))
                    ? new GzipCompressorInputStream(bis)
                    : bis;

            // Create the archive stream (auto-detects the archive type, e.g. ZIP or TAR)
            try (ArchiveInputStream archiveInputStream =
                         new ArchiveStreamFactory().createArchiveInputStream(decompressedStream)) {

                ArchiveEntry entry;
                // Loop through all entries in the archive
                while ((entry = archiveInputStream.getNextEntry()) != null) {
                    // Compare the entry's name with the target name
                    if (entry.getName().equals(targetEntryName)) {
                        // Read the contents of the found entry into a byte array
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = archiveInputStream.read(buffer)) != -1) {
                            baos.write(buffer, 0, n);
                        }
                        return baos.toByteArray();
                    }
                }
            }
        } catch (ArchiveException e) {
            throw new IOException("Error processing archive " + archivePath, e);
        }

        // Return null if the entry was not found
        return null;
    }

    public static void main(String[] args) throws IOException {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.setLogToStdOut(true);
        Path tarFile = Paths.get("/data/JIPipe/dist-omnipose/easy-python3.8-omnipose0.2.1-cpu-linux-ubuntu22.04.tar.gz");
        Path outputDir = Paths.get("/home/rgerst/tmp/python/");
        decompressTarGZ(tarFile, outputDir, progressInfo);
        Files.list(outputDir.resolve("python").resolve("bin")).forEach(PathUtils::makeUnixExecutable);
    }
}
