package org.overbaard.ci.multi.repo.directory.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {

    private final LargeFileAction largeFileAction;
    private final Path sourceDir;
    private final Path targetDir;

    public CopyDirectoryVisitor(Path sourceDir, Path targetDir) {
        this(LargeFileAction.NONE, sourceDir, targetDir);
    }

    public CopyDirectoryVisitor(LargeFileAction largeFileAction, Path sourceDir, Path targetDir) {
        this.largeFileAction = largeFileAction;
        this.sourceDir = sourceDir.toAbsolutePath();
        this.targetDir = targetDir.toAbsolutePath();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(dir);
        Path target = targetDir.resolve(relative);
        Files.createDirectories(target);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(file);
        Path target = targetDir.resolve(relative);

        System.out.println("Copying " + file + " to " + target);
        if (Files.exists(target)) {
            Files.delete(target);
        }
        Files.copy(file, target);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (largeFileAction != LargeFileAction.NONE) {
            // Do any handling of files in the target directory
            Path relative = sourceDir.relativize(dir);
            Path target = targetDir.resolve(relative);

            if (largeFileAction == LargeFileAction.MERGE) {
                if (SplitFileUtil.isSplitFilesDirectory(dir)) {
                    new SplitFileUtil().mergeFiles(target);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            try (Stream<Path> stream = Files.list(target)) {
                List<Path> list = stream.collect(Collectors.toList());
                for (Path f : list) {
                    if (largeFileAction == LargeFileAction.SPLIT) {
                        new SplitFileUtil().splitFile(f);
                    }
                }
            }
        }
        return super.postVisitDirectory(dir, exc);
    }

    static class DeleteFilesVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }


}
