package org.overbaard.ci.multi.repo.directory.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.overbaard.ci.multi.repo.ToolCommand;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SplitLargeFilesInDirectory {

    private final LargeFileAction largeFileAction;
    private final Path dir;

    public SplitLargeFilesInDirectory(LargeFileAction largeFileAction, Path dir) {
        this.largeFileAction = largeFileAction;
        this.dir = dir;
    }

    private static void processDir(LargeFileAction largeFileAction, String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalStateException("Need the following args: <root directory to split files in>");
        }
        Path path = Paths.get(args[0]);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Indicated root directory does not exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("Not a directory: " + path);
        }

        SplitLargeFilesInDirectory splitter = new SplitLargeFilesInDirectory(largeFileAction, path.toAbsolutePath());
        splitter.processDir();
    }

    private void processDir() throws Exception {
        Files.walkFileTree(dir, new IterateDirectoryVisitor(largeFileAction));
    }


    public static class SplitCommand implements ToolCommand {
        public static String NAME = "split-large-files-in-directory";

        @Override
        public String getDescription() {
            return "Splits files in the indicated directory";
        }

        @Override
        public void invoke(String[] args) throws Exception {
            SplitLargeFilesInDirectory.processDir(LargeFileAction.SPLIT, args);
        }
    }

    public static class MergeCommand implements ToolCommand {
        public static String NAME = "merge-large-files-in-directory";

        @Override
        public String getDescription() {
            return "Merges split files in the indicated directory";
        }

        @Override
        public void invoke(String[] args) throws Exception {
            SplitLargeFilesInDirectory.processDir(LargeFileAction.MERGE, args);
        }
    }

}
