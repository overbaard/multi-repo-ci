package org.overbaard.ci.multi.repo.config.trigger;

import static org.overbaard.ci.multi.repo.generator.GitHubActionGenerator.ISSUE_DATA_JSON_PATH;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.overbaard.ci.multi.repo.Util;
import org.overbaard.ci.multi.repo.config.BaseParser;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TriggerConfigParser extends BaseParser {
    private final Path yamlFile;
    private static final Path outputFile = ISSUE_DATA_JSON_PATH.toAbsolutePath();

    private TriggerConfigParser(Path yamlFile) {
        this.yamlFile = yamlFile;
    }

    public static TriggerConfigParser create(Path yamlFile) {
        return new TriggerConfigParser(yamlFile);
    }

    public TriggerConfig parse()  {
        StringBuilder sanitizedInput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(yamlFile.toFile()))) {
            String line = reader.readLine();
            boolean foundStart = false;
            boolean errorStart = false;
            while (line != null) {
                if (!foundStart && !errorStart) {
                    if (line.trim().length() > 0) {
                        if (line.startsWith("```")) {
                            foundStart = true;
                        } else {
                            System.err.println("The yaml in the issue must be in a code block.");
                            System.err.println("The format is:");
                            System.err.println("```");
                            System.err.println("<your yaml>");
                            System.err.println("```");
                            System.err.println();
                            System.err.println("Note that the ``` occurrences should be all the way left. " +
                                    "When it looks nice in the GitHub issue you have it!");
                            System.exit(1);
                        }
                    }
                } else {
                    if (line.startsWith("```")) {
                        break;
                    }
                    sanitizedInput.append(line + '\n');

                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(sanitizedInput.toString());

        Object name = map.remove("name");
        Object envInput = map.remove("env");
        Object componentsInput = map.remove("components");

        if (map.keySet().size() > 0) {
            throw new IllegalStateException("Unknown in components entries: " + map.keySet());
        }

        validateNotNullType(String.class, name, "name", " workflow trigger");
        Map<String, String> env = parseEnv(envInput);

        List<Component> components = parseComponents(componentsInput);

        TriggerConfig triggerConfig = new TriggerConfig((String) name, env, components);
        writeOutputFile(triggerConfig);
        return triggerConfig;
    }

    private List<Component> parseComponents(Object input) {
        if (input instanceof List == false) {
            throw new IllegalStateException("'components' in workflow trigger yaml is not a list");
        }
        List<Object> list = (List) input;
        Map<String, Component> components = new LinkedHashMap<>();
        for (Object o : list) {
            parseComponent(components, o);
        }
        return new ArrayList<>(components.values());
    }

    private void parseComponent(Map<String, Component> components, Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, Object> map = (Map)input;
        Object name = map.remove("name");
        validateComponentName(name);
        Object org = map.remove("org");
        Object branch = map.remove("branch");
        Object mavenOpts = map.remove("mavenOpts");
        Object debugInput = map.remove("debug");
        Object javaVersionInput = map.remove("java-version");
        validateNotNullType(String.class, name, "name", "a components entry");
        validateNotNullType(String.class, org, "org", "a components entry");
        validateNotNullType(String.class, branch, "branch", "a components entry");
        if (mavenOpts != null) {
            validateType(String.class, mavenOpts, "mavenOpts", "a components entry");
        }
        boolean debug = false;
        if (debugInput != null) {
            validateType(Boolean.class, debugInput, "debug", "a components entry");
            debug = (Boolean) debugInput;
        }

        String javaVersion = parseJavaVersion(javaVersionInput);

        Object depsInput = map.remove("dependencies");
        if (map.keySet().size() > 0) {
            throw new IllegalStateException("Unknown in components entries: " + map.keySet());
        }

        List<Dependency> dependencies = parseDependencies(depsInput);
        for (Dependency dependency : dependencies) {
            if (!components.containsKey(dependency.getName())) {
                String msg = String.format("Component '%s' has a dependency on an unseen component '%s'", name, dependency.getName());
                throw new IllegalStateException(msg);
            }
        }
        Component component =  new Component(
                (String) name,
                (String) org,
                (String) branch,
                (String) mavenOpts,
                debug,
                javaVersion,
                Collections.unmodifiableList(dependencies));
        components.put(component.getName(), component);
    }

    private List<Dependency> parseDependencies(Object input) {
        if (input == null) {
            return Collections.emptyList();
        }
        if (input instanceof List == false) {
            throw new IllegalStateException("'dependencies' is not an instance of List");
        }
        List<Object> list = (List) input;
        List<Dependency> dependencies = new ArrayList<>();
        for (Object o : list) {
            dependencies.add(parseDependency(o));
        }
        return Collections.unmodifiableList(dependencies);
    }

    private Dependency parseDependency(Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, Object> map = (Map)input;

        Object name = map.remove("name");
        Object property = map.remove("property");
        validateNotNullType(String.class, name, "name", "a dependencies entry");
        validateNotNullType(String.class, property, "property", "a dependencies entry");

        if (map.keySet().size() > 0) {
            throw new IllegalStateException("Unknown in dependencies entries: " + map.keySet());
        }

        return new Dependency((String) name, (String) property);
    }

    private void validateNotNullType(Class<?> clazz, Object value, String name, String description) {
        validateNotNull(value, name, description);
        validateType(clazz, value, name, description);
    }

    private void validateNotNull(Object value, String name, String description) {
        if (value == null) {
            String msg = String.format("Null '%s' for %s", name, description);
            throw new IllegalStateException(msg);
        }
    }

    private void validateType(Class<?> clazz, Object value, String name, String description) {
        if (!clazz.isAssignableFrom(value.getClass())) {
            String msg = String.format("'%s' for %s was not a %s: %s", name, description, clazz.getSimpleName(), value);
            throw new IllegalStateException(msg);
        }
    }

    private void validateComponentName(Object name) {
        if (!(name instanceof String)) {
            String msg = String.format("Illegal component name '%s'. It must be a String.", name);
            throw new IllegalStateException(msg);
        }
        for (char c : ((String)name).toCharArray()) {
            if (!Character.isAlphabetic(c) && c != '-') {
                String msg = String.format("Illegal component name '%s'. Only alphabetic characters or '-' are allowed", name);
                throw new IllegalStateException(msg);
            }
        }
    }

    private void writeOutputFile(TriggerConfig triggerConfig) {

        try {
            if (Files.exists(outputFile)) {
                Files.delete(outputFile);
            }
            if (!Files.exists(outputFile.getParent())) {
                Files.createDirectories(outputFile.getParent());
            }
            if (!Files.isDirectory(outputFile.getParent())) {
                throw new IllegalArgumentException(outputFile.getParent() + " is not a directory");
            }

            JSONObject jo = new JSONObject();
            jo.put("name", triggerConfig.getName());
            jo.put("env", triggerConfig.getEnv());

            JSONObject components = new JSONObject();
            for (Component c : triggerConfig.getComponents()) {
                JSONObject co = new JSONObject();
                co.put("name", c.getName());
                co.put("org", c.getOrg());
                co.put("branch", c.getBranch());

                if (c.getDependencies().size() > 0) {
                    JSONArray deps = new JSONArray();
                    for (Dependency d : c.getDependencies()) {
                        JSONObject dep = new JSONObject();
                        dep.put("name", d.getName());
                        dep.put("property", d.getProperty());
                        deps.put(dep);
                    }
                    co.put("dependencies", deps);
                }
                components.put(c.getName(), co);
            }
            jo.put("components", components);
            try (Writer writer = new BufferedWriter(new FileWriter(outputFile.toFile()))){
                jo.write(writer, 1, 1);
            }
        } catch (Exception e) {
            Util.rethrow(e);
        }
    }

}
