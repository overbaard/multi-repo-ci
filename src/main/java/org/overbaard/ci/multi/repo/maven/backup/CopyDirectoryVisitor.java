package org.overbaard.ci.multi.repo.maven.backup;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {
    private final Path sourceDir;
    private final Path targetDir;

    public CopyDirectoryVisitor(Path sourceDir, Path targetDir) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(dir);
        Files.createDirectories(targetDir.resolve(relative));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(file);
        Path target = targetDir.resolve(relative);
        Files.copy(file, target);
        return FileVisitResult.CONTINUE;
    }
}
