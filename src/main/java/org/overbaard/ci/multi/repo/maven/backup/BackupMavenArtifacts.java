package org.overbaard.ci.multi.repo.maven.backup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BackupMavenArtifacts {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    public static final String BACKUP_MAVEN_ARTIFACTS = "backup-maven-artifacts";

    private final List<ProjectArtifactInfo> artifactInfos = new ArrayList<>();

    private final Path rootPom;
    private final Path mavenRepo;
    private final Path backupLocation;

    private BackupMavenArtifacts(Path rootPom, Path mavenRepo, Path backupLocation) {
        this.rootPom = rootPom;
        this.mavenRepo = mavenRepo;
        this.backupLocation = backupLocation;
    }

    public static void backup(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalStateException("Need the following three args: <root pom path> <maven repo root> <backupLocation>");
        }

        Path rootPom = Paths.get(args[0]);
        if (!Files.exists(rootPom)) {
            throw new IllegalStateException("Root pom path does not exist: " + rootPom);
        }
        Path mavenRepo = Paths.get(args[1]);
        if (!Files.exists(mavenRepo)) {
            throw new IllegalStateException("Maven repo does not exist: " + rootPom);
        }
        Path backupLocation = Paths.get(args[2]);

        BackupMavenArtifacts grabber = new BackupMavenArtifacts(rootPom, mavenRepo, backupLocation);
        grabber.recordModules(rootPom);
        grabber.copyArtifacts();
    }

    private void recordModules(Path path) throws Exception {
        Model model = readModel(path);
        artifactInfos.add(ProjectArtifactInfo.create(model));

        if (model.getModules().size() > 0) {
            Path dir = path.getParent();
            for (String module : model.getModules()) {
                Path childPom = dir.resolve(module).resolve("pom.xml");
                recordModules(childPom);
            }
        }
    }

    private Model readModel(Path pomXml) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(pomXml, getEncoding(pomXml))) {
            final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            final Model model = xpp3Reader.read(reader);
            model.setPomFile(pomXml.toFile());

            return model;
        } catch (org.codehaus.plexus.util.xml.pull.XmlPullParserException ex) {
            throw new IOException("Failed to parse artifact POM model", ex);
        }
    }

    private Charset getEncoding(Path pomXml) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        try (Reader reader = new BufferedReader(new FileReader(pomXml.toFile()))) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);
            try {
                String encoding = xmlReader.getCharacterEncodingScheme();
                if (encoding != null) {
                    charset = Charset.forName(encoding);
                }
            } finally {
                xmlReader.close();
            }
        } catch (XMLStreamException ex) {
            throw new IOException("Failed to retrieve encoding for " + pomXml, ex);
        }
        return charset;
    }

    private void copyArtifacts() throws Exception {
        if (Files.exists(backupLocation)) {
            // Delete the backup director
            Files.walkFileTree(backupLocation, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.createDirectories(backupLocation);

        for (ProjectArtifactInfo info : artifactInfos) {
            copyArtifact(info);
        }
    }

    private void copyArtifact(ProjectArtifactInfo info) throws Exception {
        Path sourceDir = mavenRepo.resolve(info.getRelativePath());
        Path targetDir = backupLocation.resolve(info.getRelativePath());

        if (!Files.exists(sourceDir)) {
            System.out.println("WARN - cannot find module directory, skipping: " + sourceDir);
            return;
        }

        Files.createDirectories(targetDir);
        Files.walkFileTree(sourceDir, new CopyDirectoryVisitor(sourceDir, targetDir));
    }
}