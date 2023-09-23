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
    private final String mavenSetup;
    private final boolean debug;
    private final String javaVersion;
    private final List<Dependency> dependencies;

    Component(String name, String org, String branch, String mavenOpts, String mavenSetup, boolean debug, String javaVersion, List<Dependency> dependencies) {
        this.name = name;
        this.org = org;
        this.branch = branch;
        this.mavenOpts = mavenOpts;
        this.mavenSetup = mavenSetup;
        this.debug = debug;
        this.javaVersion = javaVersion;
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

    public String getMavenSetup() {
        return mavenSetup;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
