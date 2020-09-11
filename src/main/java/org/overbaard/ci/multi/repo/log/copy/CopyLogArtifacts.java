package org.overbaard.ci.multi.repo.log.copy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

public class CopyLogArtifacts {

    public static final String COPY_LOGS = "copy-logs";

    private final Path inputPath;
    private final Path outputPath;

    public CopyLogArtifacts(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    static void copy(String[] args) throws Exception {
        System.out.println(args.length);
        if (args.length != 2) {
            throw new Exception("Wrong number of args: " + Arrays.asList(args));
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);

        if (!Files.exists(input) || !Files.isDirectory(input)) {
            throw new IllegalStateException("Input path " + input + " does not exist or is not a directory");
        }
        if (!Files.exists(output)) {
            Files.createDirectories(output);
        }

        CopyLogArtifacts util = new CopyLogArtifacts(input, output);
        util.copyArtifacts();
    }

    private void copyArtifacts() throws Exception {
        Files.walkFileTree(inputPath.toAbsolutePath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".log")) {
                    copyFile(inputPath, file, outputPath);
                } else if (fileName.startsWith("TEST-") && fileName.endsWith(".xml")) {
                    if (file.getParent().getFileName().toString().equals("surefire-reports")) {
                        if (surefireFailed(file)) {
                            copyFile(inputPath, file, outputPath);
                        }
                    }
                }
                return super.visitFile(file, attrs);
            }
        });
    }

    private boolean surefireFailed(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("<testsuite ") && line.contains("surefire-test-report.xsd")) {
                if (line.contains("failures=\"0\"") && line.contains("errors=\"0\"")) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        System.out.println(path + " does not appear to be a valid surefire report. Adding it " +
                "to the list of copied artifacts just in case");
        return true;
    }

    private void copyFile(Path parent, Path path, Path outputDir) throws IOException {
        Path relative = parent.relativize(path);
        Path target = outputDir.resolve(relative);

        Files.createDirectories(target.getParent());
        System.out.println("Copying " + path + " to " + target);
        Files.copy(path, target);
    }
}