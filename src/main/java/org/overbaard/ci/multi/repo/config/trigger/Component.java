package org.overbaard.ci.multi.repo.config.trigger;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Component {
    private final String name;
    private final String org;
    private final String branch;
    private final String mavenOpts;
    private final boolean debug;
    private final List<Dependency> dependencies;

    Component(String name, String org, String branch, String mavenOpts, boolean debug, List<Dependency> dependencies) {
        this.name = name;
        this.org = org;
        this.branch = branch;
        this.mavenOpts = mavenOpts;
        this.debug = debug;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public String getOrg() {
        return org;
    }

    public String getBranch() {
        return branch;
    }

    public String getMavenOpts() {
        return mavenOpts;
    }

    public boolean isDebug() {
        return debug;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
