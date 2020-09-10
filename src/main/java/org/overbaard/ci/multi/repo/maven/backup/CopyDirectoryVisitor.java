package org.overbaard.ci.multi.repo.maven.backup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {

    // GitHub's max file size is 100Mb. Set it to 90 just to have some leeway
    private static final long MAX_SIZE_BYTES = 90 * 1024 * 1024;

    private static final String SPLIT_FILE_SUFFIX = ".splitfile";
    private static final String SPLIT_FILE_PREFIX = SPLIT_FILE_SUFFIX + "-";

    private final LargeFileAction largeFileAction;
    private final Path sourceDir;
    private final Path targetDir;

    public CopyDirectoryVisitor(Path sourceDir, Path targetDir) {
        this(LargeFileAction.NONE, sourceDir, targetDir);
    }

    public CopyDirectoryVisitor(LargeFileAction largeFileAction, Path sourceDir, Path targetDir) {
        this.largeFileAction = largeFileAction;
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
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
        Files.copy(file, target);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (largeFileAction != LargeFileAction.NONE) {
            // Do any handling of files in the target directory
            Path relative = sourceDir.relativize(dir);
            Path target = targetDir.resolve(relative);

            try (Stream<Path> stream = Files.list(target)) {
                List<Path> list = stream.collect(Collectors.toList());
                for (Path f : list) {
                    if (largeFileAction == LargeFileAction.SPLIT) {
                        splitFile(f);
                    } else if (largeFileAction == LargeFileAction.MERGE) {
                        mergeFile(f);
                    }
                }
            }
        }
        return super.postVisitDirectory(dir, exc);
    }

    private void mergeFile(Path file) throws IOException {
        String filename = file.getFileName().toString();
        if (filename.startsWith(SPLIT_FILE_PREFIX) && filename.endsWith(SPLIT_FILE_SUFFIX)) {
            Path mergedFile;
            List<Path> partNames = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                String line = reader.readLine();
                mergedFile = file.getParent().resolve(line);
                line = reader.readLine();

                while (line != null) {
                    partNames.add(file.getParent().resolve(line));
                    line = reader.readLine();
                }
            }

            try (RandomAccessFile toFile = new RandomAccessFile(mergedFile.toFile(), "rw");
                    FileChannel toChannel = toFile.getChannel()) {

                int position = 0;

                for (Path path : partNames) {
                    try (RandomAccessFile part = new RandomAccessFile(path.toFile(), "r");
                            FileChannel fromChannel = part.getChannel()) {
                        long size = Files.size(path);
                        toChannel.transferFrom(fromChannel, position, size);
                        position += size;
                    }
                }
            }
            Files.delete(file);
            for (Path path : partNames) {
                Files.delete(path);
            }
        }

    }

    private void splitFile(Path file) throws IOException {
        long sourceSize = Files.size(file);
        long bytesPerSplit = MAX_SIZE_BYTES;
        if (sourceSize > bytesPerSplit) {
            long numSplits = sourceSize / bytesPerSplit;
            long remainingBytes = sourceSize % bytesPerSplit;

            List<Path> partFiles = new ArrayList<>();

            try (RandomAccessFile sourceFile = new RandomAccessFile(file.toFile(), "r");
                    FileChannel sourceChannel = sourceFile.getChannel()) {

                int position = 0;
                for (; position < numSplits; position++) {
                    Path part = writePartToFile(file, bytesPerSplit, position * bytesPerSplit, sourceChannel, partFiles);
                    partFiles.add(part);
                }

                if (remainingBytes > 0) {
                    Path part = writePartToFile(file, remainingBytes, position * bytesPerSplit, sourceChannel, partFiles);
                    partFiles.add(part);
                }
            }

            String markerFileName = SPLIT_FILE_PREFIX + file.getFileName().toString() + SPLIT_FILE_SUFFIX;
            Path markerPath = file.getParent().resolve(markerFileName);
            try (Writer writer = new BufferedWriter(new FileWriter(markerPath.toFile()))) {
                writer.write(file.getFileName().toString());
                writer.write("\n");
                for (Path path : partFiles) {
                    writer.write(path.getFileName().toString());
                    writer.write("\n");
                }
            }

            Files.delete(file);
        }
    }

    private Path writePartToFile(Path file, long byteSize, long position, FileChannel sourceChannel, List<Path> partFiles) throws IOException {
        Path fileName = Paths.get(file.toString() + "-" + UUID.randomUUID());
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");
             FileChannel toChannel = toFile.getChannel()) {

            sourceChannel.position(position);
            toChannel.transferFrom(sourceChannel, 0, byteSize);
        }
        return fileName;
    }

    enum LargeFileAction {
        NONE,
        SPLIT,
        MERGE
    }
}
