package org.overbaard.ci.multi.repo.config.trigger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TriggerConfig {
    private static final Map<String, String> DEFAULT_ENV = Collections.singletonMap("MAVEN_OPTS", "-Xms756M -Xmx1g");
    private final String name;
    private final Map<String, String> env;
    private final List<Component> components;

    TriggerConfig(String name, Map<String, String> env, List<Component> components) {
        this.name = name;
        this.env = env == null ? DEFAULT_ENV : env;
        this.components = Collections.unmodifiableList(components);
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public List<Component> getComponents() {
        return components;
    }
}
