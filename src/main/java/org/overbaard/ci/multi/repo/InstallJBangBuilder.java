package org.overbaard.ci.multi.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class InstallJBangBuilder {
    private IfCondition ifCondition;


    public InstallJBangBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> runCommand = new LinkedHashMap<>();
        runCommand.put("name", "Build with Maven");
        StringBuilder command = new StringBuilder("echo Installing SDKMAN\n");
        command.append("curl -s \"https://get.sdkman.io\" | bash\n");
        command.append("source ~/.sdkman/bin/sdkman-init.sh\n");
        command.append("sdk install jbang");

        runCommand.put("name", "Install JBang");
        if (ifCondition != null) {
            runCommand.put("if", ifCondition.getValue());
        }
        runCommand.put("run", command.toString());
        return runCommand;
    }
}
