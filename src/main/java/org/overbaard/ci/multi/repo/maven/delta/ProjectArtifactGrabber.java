package org.overbaard.ci.multi.repo.maven.delta;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class ProjectArtifactGrabber {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private final List<ProjectArtifactInfo> artifactInfos = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // TODO use args
        Path path = Paths.get("/Users/kabir/sourcecontrol/wildfly/wildfly-core/pom.xml");

        ProjectArtifactGrabber grabber = new ProjectArtifactGrabber();
        grabber.recordArtifacts(path);
    }

    private void recordArtifacts(Path rootPom) throws Exception {
        Model model = readModel(rootPom);

        if (model.getModules().size() > 0) {
            Path dir = rootPom.getParent();
            for (String module : model.getModules()) {
                Path childPom = dir.resolve(module).resolve("pom.xml");
                readModel(childPom);
            }
        }
    }

    private Model readModel(Path pomXml) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(pomXml, getEncoding(pomXml))) {
            final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            final Model model = xpp3Reader.read(reader);
            model.setPomFile(pomXml.toFile());

            artifactInfos.add(ProjectArtifactInfo.create(model));

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
}