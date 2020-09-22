package org.overbaard.ci.multi.repo.directory.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SplitFileUtil {
    private static final String SPLIT_FILE_DIRECTORY_SUFFIX = ".split.file.dir";

    // GitHub's max file size is 100Mb. It recommends max 50.
    // Set it to 49 just to have some leeway
    private static final long MAX_SIZE_BYTES = 49 * 1024 * 1024;

    static boolean isSplitFilesDirectory(Path dir) {
        if (Files.isDirectory(dir) && dir.getFileName().toString().endsWith(SplitFileUtil.SPLIT_FILE_DIRECTORY_SUFFIX)) {
            return true;
        }
        return false;
    }

    void mergeFiles(Path splitDir) throws IOException {
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

            Files.walkFileTree(splitDir, new CopyDirectoryVisitor.DeleteFilesVisitor());
        }
    }

    void splitFile(Path file) throws IOException {
        long sourceSize = Files.size(file);
        long bytesPerSplit = MAX_SIZE_BYTES;
        if (sourceSize > bytesPerSplit) {
            long numSplits = sourceSize / bytesPerSplit;
            long remainingBytes = sourceSize % bytesPerSplit;

            Path splitDir = file.getParent().resolve(file.getFileName().toString() + SPLIT_FILE_DIRECTORY_SUFFIX);
            System.out.println("Splitting " + file + " to " + splitDir);
            if (Files.exists(splitDir)) {
                Files.walkFileTree(splitDir.toAbsolutePath(), new CopyDirectoryVisitor.DeleteFilesVisitor());
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

}
