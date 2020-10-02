package org.overbaard.ci.multi.repo.directory.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.overbaard.ci.multi.repo.ToolCommand;
import org.overbaard.ci.multi.repo.Util;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OverlayBackedUpMavenArtifacts {

    private final Path mavenRepoRoot;
    private final Path backupsFolder;

    public OverlayBackedUpMavenArtifacts(Path mavenRepoRoot, Path backupsFolder) {
        this.mavenRepoRoot = mavenRepoRoot.toAbsolutePath();
        this.backupsFolder = backupsFolder.toAbsolutePath();
    }

    static void overlay(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalStateException("Needs: <maven repo> <backups folder>");
        }
        Path mavenRepoRoot = Paths.get(args[0]);
        Path backupsFolder = Paths.get(args[1]);

        if (!Files.exists(mavenRepoRoot) || !Files.isDirectory(mavenRepoRoot)) {
            throw new IllegalStateException("Maven repo root does not exist or is not a directory: " + mavenRepoRoot);
        }
        if (!Files.exists(backupsFolder) || !Files.isDirectory(backupsFolder)) {
            throw new IllegalStateException("Backups folder does not exist or is not a directory: " + backupsFolder);
        }

        OverlayBackedUpMavenArtifacts overlay = new OverlayBackedUpMavenArtifacts(mavenRepoRoot, backupsFolder);
        overlay.overlay();
    }

    private void overlay() throws Exception {
        try (Stream<Path> stream = Files.list(backupsFolder).filter(p -> Files.isDirectory(p))) {
            stream.forEach(componentBackup -> {
                try {
                    overlayComponent(componentBackup);
                } catch (IOException e) {
                    Util.rethrow(e);
                }
            });
        }
    }

    private void overlayComponent(Path componentBackup) throws IOException {
        deleteExistingPathsInMavenRepo(componentBackup);
        Files.walkFileTree(componentBackup, new CopyDirectoryVisitor(LargeFileAction.MERGE, componentBackup, mavenRepoRoot));
    }

    private void deleteExistingPathsInMavenRepo(Path componentBackup) throws IOException {
        Set<Path> deletePaths = new HashSet<>();
        Files.walkFileTree(componentBackup, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path backupFolder = file.getParent();
                Path relative = componentBackup.relativize(backupFolder);
                Path repoPath = mavenRepoRoot.resolve(relative);
                deletePaths.add(repoPath);
                return super.visitFile(file, attrs);
            }
        });

        for (Path repoDir : deletePaths) {
            if (!Files.exists(repoDir)) {
                break;
            }
            Files.walkFileTree(repoDir, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    public static class Command implements ToolCommand {
        public static final String NAME = "overlay-backed-up-maven-artifacts";

        @Override
        public String getDescription() {
            return "Overlays the maven repository with backed up maven artifacts";
        }

        @Override
        public void invoke(String[] args) throws Exception {
            overlay(args);
        }
    }
}
