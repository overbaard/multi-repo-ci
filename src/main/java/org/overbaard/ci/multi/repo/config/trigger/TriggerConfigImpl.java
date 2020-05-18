package org.overbaard.ci.multi.repo.config.trigger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TriggerConfigImpl implements TriggerConfig {
    private static final Map<String, String> DEFAULT_ENV = Collections.singletonMap("MAVEN_OPTS", "-Xms756M -Xmx1g");
    private final String name;
    private final Map<String, String> env;
    private final List<Component> components;

    public TriggerConfigImpl(String name, Map<String, String> env, List<Component> components) {
        this.name = name;
        this.env = env == null ? DEFAULT_ENV : env;
        this.components = Collections.unmodifiableList(components);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getEnv() {
        return env;
    }

    @Override
    public List<Component> getComponents() {
        return components;
    }
}
