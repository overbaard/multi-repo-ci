package org.overbaard.ci.multi.repo.config.trigger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Dependency {
    private final String name;
    private final String property;

    Dependency(String name, String property) {
        this.name = name;
        this.property = property;
    }

    public String getName() {
        return name;
    }

    public String getProperty() {
        return property;
    }
}
