package org.overbaard.ci.multi.repo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SetupJavaBuilder {
    private String version;

    public SetupJavaBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    Map<String, Object> build() {
        if (version == null) {
            throw new IllegalStateException("No version set!");
        }
        Map<String, Object> setup = new LinkedHashMap<>();
        setup.put("uses", "actions/setup-java@v1");
        setup.put("with", Collections.singletonMap("java-version", version));
        return setup;
    }
}
