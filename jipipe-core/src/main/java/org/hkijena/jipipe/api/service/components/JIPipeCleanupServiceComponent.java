package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.utils.FileLocker;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service component responsible for application-wide cleanup operations
 */
public class JIPipeCleanupServiceComponent extends JIPipeServiceComponent {

    private final JIPipeRunnableQueue cleanupQueue = new JIPipeRunnableQueue("Cleanup");
    private final Set<Path> cleanedTemporaryFileRoots =  new HashSet<>();

    public JIPipeCleanupServiceComponent(JIPipeService service) {
        super(service);
    }

    public JIPipeRunnableQueue getCleanupQueue() {
        return cleanupQueue;
    }

    /**
     * Schedules a cleanup operation that will look for old temporary files
     * @param project the project directory
     */
    public void scheduleProjectTemporaryFilesCleanup(JIPipeProject project) {
        if(project == null) {
            return;
        }
        if(project.getProjectFile() == null) {
            // Skip unsaved projects
            return;
        }
        Path temporaryFileRoot = project.getTemporaryBaseDirectory();
        if(temporaryFileRoot == null || temporaryFileRoot.getParent() == null) {
            return;
        }
        temporaryFileRoot = temporaryFileRoot.getParent();

        // Must be named JIPipe.tmp.dir
        if(!"JIPipe.tmp.dir".equals(StringUtils.nullToEmpty(temporaryFileRoot.getFileName()))) {
            return;
        }
        temporaryFileRoot = temporaryFileRoot.toAbsolutePath();

        if(cleanedTemporaryFileRoots.contains(temporaryFileRoot)) {
            return;
        }

        cleanedTemporaryFileRoots.add(temporaryFileRoot);
        cleanupQueue.enqueue(new CleanupTemporaryFileRootRun(temporaryFileRoot));
    }

    public static class CleanupTemporaryFileRootRun extends DefaultJIPipeRunnable {

        private final Path root;

        public CleanupTemporaryFileRootRun(Path root) {
            this.root = root;
        }

        @Override
        public String getTaskLabel() {
            return "Cleanup temporary file root";
        }

        @Override
        public void run() {
            JIPipeProgressInfo progressInfo = getProgressInfo();
//            progressInfo.setLogToStdOut(true);
            progressInfo.log("Cleanup for " + root);

            if(!"JIPipe.tmp.dir".equals(StringUtils.nullToEmpty(root.getFileName()))) {
                progressInfo.aggressiveError("Safety guard failed for " + root);
                throw new RuntimeException("Safety guard failed for " + root);
            }

            for (Path tmpRoot : PathUtils.listSubDirectories(root)) {
                if(!Files.isDirectory(tmpRoot)) {
                    continue;
                }

                // Check if the directory name is consistent with old notation (tmp.*) or new notation (PathUtils random generator)
                String dirName = tmpRoot.getFileName().toString();
                JIPipeProgressInfo tmpRootProgress = progressInfo.resolve(tmpRoot.toString());
                boolean suitable;
                if(dirName.startsWith("tmp")) {
                    suitable = true;
                }
                else {
                    suitable = Pattern.matches("^[" + PathUtils.RANDOM_TMP_CHARACTERS + "]{" + PathUtils.RANDOM_TMP_LENGTH + "}$", dirName);
                }

                tmpRootProgress.log("Name-based check yielded match=" + suitable);
                if(!suitable) {
                    continue;
                }

                // Check for a lock file and ensure that it's writable
                Path lockFile = tmpRoot.resolve("lock");

                if(Files.isRegularFile(lockFile)) {
                    FileLocker fileLocker = new FileLocker(tmpRootProgress.resolve("Lock file test"), lockFile);
                    if(!fileLocker.tryWriteLock()) {
                        tmpRootProgress.warn("Seems to be used by another JIPipe project. Skipping.");
                        continue;
                    }
                }

                // Delete operation
                tmpRootProgress.log("All checks passed, deleting " + tmpRoot);
                PathUtils.deleteDirectoryRecursively(tmpRoot, tmpRootProgress.resolve("Delete"));

            }
        }
    }
}
