package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CacheMavenRepoBuilder {
    Map<String, Object> build() {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("uses", "actions/cache@v1");

        Map<String, Object> with = new LinkedHashMap<>();
        with.put("path", "~/.m2/repository");
        with.put("key", "${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}");
        with.put("restore-keys", "${{ runner.os }}-maven-");
        cache.put("with", with);

        return cache;
    }

}
