package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TmateDebugStepBuilder {

    Map<String, Object> build() {
        Map<String, Object> setup = new LinkedHashMap<>();
        setup.put("name", "Set up TMate for SSH debugging");
        setup.put("uses", "mxschmitt/action-tmate@v2");
        return setup;
    }

}
