package org.overbaard.ci.multi.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadFileIntoEnvVarBuilder {
    private String path;
    private String envVarName;

    public ReadFileIntoEnvVarBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public ReadFileIntoEnvVarBuilder setEnvVarName(String envVarName) {
        this.envVarName = envVarName;
        return this;
    }

    Map<String, Object> build() {
        StringBuilder bash = new StringBuilder();
        bash.append("TMP=`cat " + path +"`\n");
        bash.append(String.format("echo \"::set-env name=%s::${TMP}\"\n", envVarName));
        bash.append("echo \"Stored ${TMP} into " + envVarName + "\"\n");

        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("name", "Read $" + envVarName);
        cmd.put("run", bash.toString());
        return cmd;
    }
}
