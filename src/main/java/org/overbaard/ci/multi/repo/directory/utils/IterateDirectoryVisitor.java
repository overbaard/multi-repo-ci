package org.overbaard.ci.multi.repo.directory.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class IterateDirectoryVisitor extends SimpleFileVisitor<Path> {
    private final LargeFileAction largeFileAction;

    public IterateDirectoryVisitor(LargeFileAction largeFileAction) {
        this.largeFileAction = largeFileAction;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (largeFileAction == LargeFileAction.MERGE) {
            if (SplitFileUtil.isSplitFilesDirectory(dir)) {
                new SplitFileUtil().mergeFiles(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> list = stream.collect(Collectors.toList());
            for (Path f : list) {
                if (largeFileAction == LargeFileAction.SPLIT) {
                    new SplitFileUtil().splitFile(f);
                }
            }
        }

        return FileVisitResult.CONTINUE;
    }
}
