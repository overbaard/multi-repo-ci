package org.overbaard.ci.multi.repo.config.trigger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DependencyImpl implements Dependency {
    private final String name;
    private final String property;

    DependencyImpl(String name, String property) {
        this.name = name;
        this.property = property;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProperty() {
        return property;
    }
}
