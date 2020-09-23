package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Ipv6LocalhostStepBuilder {

    //
    Map<String, Object> build() {
        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("name", "Add '::1 localhost' to hosts file");
        steps.put("run", "sudo bash -c 'echo ::1 localhost >> /etc/hosts'");
        return steps;
    }
}
