package org.overbaard.ci.multi.repo.maven.backup;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CopyDirectoryVisitor extends SimpleFileVisitor<Path> {

    // GitHub's max file size is 100Mb. Set it to 90 just to have some leeway
    private static final long MAX_SIZE_BYTES = 90 * 1024 * 1024;

    private static final String SPLIT_FILE_DIRECTORY_SUFFIX = ".split.file.dir";

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
                if (dir.getFileName().toString().endsWith(SPLIT_FILE_DIRECTORY_SUFFIX)) {
                    mergeFiles(target);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            try (Stream<Path> stream = Files.list(target)) {
                List<Path> list = stream.collect(Collectors.toList());
                for (Path f : list) {
                    if (largeFileAction == LargeFileAction.SPLIT) {
                        splitFile(f);
                    }
                }
            }
        }
        return super.postVisitDirectory(dir, exc);
    }

    private void mergeFiles(Path splitDir) throws IOException {
        String baseFileName = getFileNameFromSplitDirName(splitDir);
        Path mergedTargetFile = splitDir.getParent().resolve(baseFileName);
        System.out.println("Merging split dir " + splitDir + " to " + mergedTargetFile);

        try (RandomAccessFile toFile = new RandomAccessFile(mergedTargetFile.toFile(), "rw");
                FileChannel toChannel = toFile.getChannel()) {

            int position = 0;

            for (int index = 0 ; ; index++) {
                Path path = splitDir.resolve(createFileNameForIndex(index));
                if (!Files.exists(path)) {
                    break;
                }

                try (RandomAccessFile part = new RandomAccessFile(path.toFile(), "r");
                        FileChannel fromChannel = part.getChannel()) {
                    long size = Files.size(path);
                    toChannel.transferFrom(fromChannel, position, size);
                    Files.delete(path);
                    position += size;
                }
            }

            Files.walkFileTree(splitDir, new DeleteFilesVisitor());
        }
    }

    private void splitFile(Path file) throws IOException {
        long sourceSize = Files.size(file);
        long bytesPerSplit = MAX_SIZE_BYTES;
        if (sourceSize > bytesPerSplit) {
            long numSplits = sourceSize / bytesPerSplit;
            long remainingBytes = sourceSize % bytesPerSplit;

            Path splitDir = file.getParent().resolve(file.getFileName().toString() + SPLIT_FILE_DIRECTORY_SUFFIX);
            if (Files.exists(splitDir)) {
                Files.walkFileTree(splitDir.toAbsolutePath(), new DeleteFilesVisitor());
            }
            Files.createDirectories(splitDir);
            createReassembleScript(splitDir);

            try (RandomAccessFile sourceFile = new RandomAccessFile(file.toFile(), "r");
                    FileChannel sourceChannel = sourceFile.getChannel()) {


                int position = 0;
                long sourcePosition = 0;
                for (; position < numSplits; position++) {
                    writePartToFile(splitDir, bytesPerSplit, position, sourceChannel, sourcePosition);
                    sourcePosition += bytesPerSplit;
                }

                if (remainingBytes > 0) {
                    writePartToFile(splitDir, remainingBytes, position, sourceChannel, sourcePosition);
                }
            }

            Files.delete(file);
        }
    }

    private Path writePartToFile(Path splitDir, long byteSize, int index, FileChannel sourceChannel, long sourcePosition) throws IOException {
        Path fileName = splitDir.resolve(createFileNameForIndex(index));
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");
             FileChannel toChannel = toFile.getChannel()) {

            sourceChannel.position(sourcePosition);
            toChannel.transferFrom(sourceChannel, 0, byteSize);
        }
        return fileName;
    }

    private String createFileNameForIndex(int i) {
        // If we change the length from 2 digits we also need to change the reassemble script
        StringBuilder sb = new StringBuilder("xx.");
        if (i < 10) {
            sb.append("0");
        }
        if (i >= 100) {
            // Very unlikely so I am not going to try to guard against it here
            // (9GB should be more than enough, considering the size of the VMs)
            throw new IllegalStateException("File is way too large!");
        }
        sb.append(i);
        return sb.toString();
    }

    private void createReassembleScript(Path dir) throws IOException {
        Path reassembleInstructions = dir.resolve("reassemble.sh");
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/sh");
        sb.append("# The file has been split due to its large size. Run this script to reassemble it.\n");
        // Suffix length set by the format method is 2
        sb.append("WORKING_DIR=$(dirname $0)\n");
        sb.append("cat ${WORKING_DIR}/xx.?? > ${WORKING_DIR}/" + getFileNameFromSplitDirName(dir) + "\n");
        sb.append("echo Reassembled ${WORKING_DIR}/" + getFileNameFromSplitDirName(dir) + "\n");
        Files.createFile(reassembleInstructions);
        Files.write(reassembleInstructions, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

        String perm = "rwxr-xr-x";
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(perm);
        Files.setPosixFilePermissions(reassembleInstructions, permissions);
    }

    private String getFileNameFromSplitDirName(Path splitDir) {
        String filename = splitDir.getFileName().toString();
        String baseFileName = filename.substring(0, filename.indexOf(SPLIT_FILE_DIRECTORY_SUFFIX));
        return baseFileName;
    }

    private static class DeleteFilesVisitor extends SimpleFileVisitor<Path> {
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


    enum LargeFileAction {
        NONE,
        SPLIT,
        MERGE
    }
}
