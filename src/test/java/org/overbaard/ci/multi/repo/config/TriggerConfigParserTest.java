package org.overbaard.ci.multi.repo.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import org.overbaard.ci.multi.repo.config.trigger.Component;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfig;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfigParser;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TriggerConfigParserTest {
    @Test
    public void testParseYaml() throws Exception {
        URL url = this.getClass().getResource("issue-test.yml");
        Path path = Paths.get(url.toURI());
        TriggerConfig triggerConfig = TriggerConfigParser.create(path).parse();
        Assert.assertNotNull(triggerConfig);

        Map<String, String> env = triggerConfig.getEnv();
        Assert.assertEquals(2, env.size());
        Assert.assertEquals("-Xms756M -Xmx1g", env.get("MAVEN_OPTS"));
        Assert.assertEquals("value1", env.get("VALUE1"));

        List<Component> components = triggerConfig.getComponents();
        Assert.assertNotNull(components);
        Assert.assertEquals(2, components.size());

        Component wfCommon = components.get(0);
        Assert.assertEquals("wildfly-common", wfCommon.getName());
        Assert.assertEquals("wildfly", wfCommon.getOrg());
        Assert.assertEquals("master", wfCommon.getBranch());
        Assert.assertNull(wfCommon.getMavenOpts());
        Assert.assertEquals("versions:update-parent -DallowSnapshots=true", wfCommon.getMavenSetup());
        Assert.assertNotNull(wfCommon.getDependencies());
        Assert.assertTrue(wfCommon.isDebug());
        Assert.assertEquals("15", wfCommon.getJavaVersion());
        Assert.assertEquals(0, wfCommon.getDependencies().size());

        Component wfElytron = components.get(1);
        Assert.assertEquals("wildfly-elytron", wfElytron.getName());
        Assert.assertEquals("kabir", wfElytron.getOrg());
        Assert.assertEquals("feature", wfElytron.getBranch());
        Assert.assertEquals("-DskipTests -Dhello=true", wfElytron.getMavenOpts());
        Assert.assertNull(wfElytron.getMavenSetup());
        Assert.assertFalse(wfElytron.isDebug());
        Assert.assertNull(wfElytron.getJavaVersion());
        Assert.assertNotNull(wfElytron.getDependencies());
        Assert.assertEquals(1, wfElytron.getDependencies().size());
        Assert.assertEquals("wildfly-common", wfElytron.getDependencies().get(0).getName());
        Assert.assertEquals("version.wildfly.common", wfElytron.getDependencies().get(0).getProperty());

        JSONObject expectedJson = readJson(Paths.get(this.getClass().getResource("expected-issue-data.json").toURI()));
        JSONObject writtenJson = readJson(Paths.get("issue-data.json"));
        compareJsonObjects(expectedJson, writtenJson);
    }

    private JSONObject readJson(Path path) throws IOException  {
        try (Reader reader = new BufferedReader(new FileReader(path.toFile()))) {
            JSONTokener jt = new JSONTokener(reader);
            return new JSONObject(jt);
        }
    }


    private void compareJsonObjects(JSONObject expected, JSONObject actual) {
        Set<String> expectedKeys = expected.keySet();
        Set<String> actualKeys = actual.keySet();

        Assert.assertEquals(expectedKeys, actualKeys);

        for (String key : expectedKeys) {
            Object expectedObject = expected.get(key);
            Object actualObject = actual.get(key);
            compareReadJsonObjects(expectedObject, actualObject);
        }
    }

    private void compareJsonArrays(JSONArray expectedObject, JSONArray actualObject) {
        Assert.assertEquals(expectedObject.length(), actualObject.length());
        List<Object> expectedList = expectedObject.toList();
        List<Object> actualList = actualObject.toList();
        for (int i = 0; i < expectedList.size(); i++) {
            compareReadJsonObjects(expectedList.get(i), actualList.get(i));
        }
    }

    private void compareReadJsonObjects(Object expectedObject, Object actualObject) {
        if (expectedObject instanceof JSONObject && actualObject instanceof JSONObject) {
            compareJsonObjects((JSONObject)expectedObject, (JSONObject)actualObject);
        } else if (expectedObject instanceof JSONArray && actualObject instanceof JSONArray) {
            compareJsonArrays((JSONArray)expectedObject, (JSONArray)actualObject);
        } else {
            Assert.assertEquals(expectedObject, actualObject);
        }
    }

}