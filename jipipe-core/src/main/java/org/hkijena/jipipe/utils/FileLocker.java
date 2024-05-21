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

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileLocker {
    private final JIPipeProgressInfo progressInfo;
    private final Path lockFilePath;
    private FileChannel fileChannel;
    private FileLock fileLock;

    public FileLocker(JIPipeProgressInfo progressInfo, Path lockFilePath) {
        this.progressInfo = progressInfo;
        this.lockFilePath = lockFilePath;
    }

    public boolean acquireWriteLock() {
        try {
            while (true) {
                Files.createDirectories(lockFilePath.getParent());
                progressInfo.log("Attempting to acquire WRITE lock " + lockFilePath);
                fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                fileLock = fileChannel.tryLock();
                if (fileLock != null) {
                    return true;
                }

                progressInfo.log("... not successful, retrying in 1s");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean acquireReadLock() {
        try {

            // Create the file if it does not exist
            if (!Files.isRegularFile(lockFilePath)) {
                acquireWriteLock();
                releaseLock();
            }

            while (true) {
                progressInfo.log("Attempting to acquire READ lock " + lockFilePath);
                fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.READ);
                fileLock = fileChannel.tryLock();
                if (fileLock != null) {
                    return true;
                }

                progressInfo.log("... not successful, retrying in 1s");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void releaseLock() {
        try {
            progressInfo.log("Releasing lock " + lockFilePath);
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release(); // Release the lock
            }
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close(); // Close the file channel
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
