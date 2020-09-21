package org.overbaard.ci.multi.repo.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.repo.RepoConfigParser;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BaseParser {
    protected Map<String, String> parseEnv(Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, Object> map = (Map<String, Object>)input;
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof String == false && value instanceof Number == false) {
                throw new IllegalStateException("Value under key '" + key + "' is not a String or a Number");
            }
            result.put(key, value.toString());
        }
        return Collections.unmodifiableMap(result);
    }

    protected String parseJavaVersion(Object input) {
        if (input == null) {
            return null;
        }

        if (input instanceof String == false && input instanceof Number == false) {
            throw new IllegalStateException("'java-version' is not a String or a Number");
        }

        return input.toString();
    }

    // TODO I don't think we need this any more
    protected Map<String, Object> preParseEndJob(Object input) {
        if (!(this instanceof RepoConfigParser) && !(this instanceof ComponentJobsConfigParser)) {
            throw new IllegalStateException("Cannot parse end job from " + this.getClass());
        }

        if (input == null) {
            return null;
        }
        if (!(input instanceof Map)) {
            throw new IllegalStateException("end-job must be an object");
        }
        Map<String, Object> endJob = (Map<String, Object>) input;
        if (endJob.get("name") != null) {
            throw new IllegalStateException("end-job should not have 'name'");
        }
        if (endJob.get("runs-on") != null) {
            throw new IllegalStateException("end-job should not have 'runs-on'");
        }
        if (endJob.get("needs") != null) {
            throw new IllegalStateException("end-job should not have 'needs'");
        }
        if (endJob.get("env") == null) {
            endJob.put("env", new LinkedHashMap<>());
        }
        if (endJob.get("steps") == null) {
            throw new IllegalStateException("end-job should have a 'steps'");
        }
        Object steps = endJob.get("steps");
        if (!(steps instanceof List)) {
            throw new IllegalStateException("end-job 'steps' should be a list");
        }
        return endJob;
    }
}
