package org.overbaard.ci.multi.repo.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunMultiRepoCiToolCommandBuilder {
    private String jar;
    private List<String> args = new ArrayList<>();
    private IfCondition ifCondition;
    private String command;

    public RunMultiRepoCiToolCommandBuilder setJar(String jar) {
        this.jar = jar;
        return this;
    }

    RunMultiRepoCiToolCommandBuilder setCommand(String command) {
        this.command = command;
        return this;
    }

    public RunMultiRepoCiToolCommandBuilder addArgs(String...args) {
        for (String arg : args) {
            this.args.add(arg);
        }
        return this;
    }

    public RunMultiRepoCiToolCommandBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    Map<String, Object> build() {
        StringBuilder sb = new StringBuilder();
        // Run the sdkman init step before trying to use it
        sb.append("java -jar ");
        sb.append(jar);
        sb.append(" ");
        sb.append(command);
        for (String arg : args) {
            sb.append(" ");
            sb.append(arg);
        }
        sb.append("\n");

        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("name", "Run multi-repo-ci-tool '" + steps + "' command");
        if (ifCondition != null) {
            steps.put("if", ifCondition.getValue());
        }
        steps.put("run", sb.toString());
        return steps;
    }

}
