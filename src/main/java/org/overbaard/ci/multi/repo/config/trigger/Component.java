package org.overbaard.ci.multi.repo.config.trigger;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface Component {
    String getName();

    String getOrg();

    String getBranch();

    String getMavenOpts();

    boolean isDebug();

    List<Dependency> getDependencies();
}
