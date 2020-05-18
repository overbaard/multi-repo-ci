package org.overbaard.ci.multi.repo.config.trigger;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface TriggerConfig {
    String getName();
    Map<String, String> getEnv();
    List<Component> getComponents();
}
