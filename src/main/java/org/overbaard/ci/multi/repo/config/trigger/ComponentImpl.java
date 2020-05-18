package org.overbaard.ci.multi.repo.config.trigger;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentImpl implements Component {
    private final String name;
    private final String org;
    private final String branch;
    private final String mavenOpts;
    private final List<Dependency> dependencies;

    public ComponentImpl(String name, String org, String branch, String mavenOpts, List<Dependency> dependencies) {
        this.name = name;
        this.org = org;
        this.branch = branch;
        this.mavenOpts = mavenOpts;
        this.dependencies = dependencies;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOrg() {
        return org;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public String getMavenOpts() {
        return mavenOpts;
    }

    @Override
    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
