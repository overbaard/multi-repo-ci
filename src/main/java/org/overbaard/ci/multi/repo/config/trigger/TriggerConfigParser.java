package org.overbaard.ci.multi.repo.config.trigger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.overbaard.ci.multi.repo.config.BaseParser;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TriggerConfigParser extends BaseParser {
    private final Path yamlFile;

    private TriggerConfigParser(Path yamlFile) {
        this.yamlFile = yamlFile;
    }

    public static TriggerConfigParser create(Path yamlFile) {
        return new TriggerConfigParser(yamlFile);
    }

    public TriggerConfig parse()  {
        Map<String, Object> map = null;
        try {
            Yaml yaml = new Yaml();
            map = yaml.load(new BufferedInputStream(new FileInputStream(yamlFile.toFile())));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Object name = map.remove("name");
        Object envInput = map.remove("env");
        Object componentsInput = map.remove("components");

        if (map.keySet().size() > 0) {
            throw new IllegalStateException("Unknown in components entries: " + map.keySet());
        }

        validateNotNullType(String.class, name, "name", " workflow trigger");
        Map<String, String> env = parseEnv(envInput);

        List<Component> components = parseComponents(componentsInput);

        return new TriggerConfig((String) name, env, components);
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
}
