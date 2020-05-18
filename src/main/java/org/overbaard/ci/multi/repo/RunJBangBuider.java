package org.overbaard.ci.multi.repo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunJBangBuider {
    private String script;
    private List<String> args = new ArrayList<>();
    private IfCondition ifCondition;

    public RunJBangBuider setScript(String script) {
        this.script = script;
        return this;
    }

    public RunJBangBuider addArgs(String...args) {
        for (String arg : args) {
            this.args.add(arg);
        }
        return this;
    }

    public RunJBangBuider setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    Map<String, Object> build() {
        StringBuilder sb = new StringBuilder();
        // Run the sdkman init step before trying to use it
        sb.append("source ~/.sdkman/bin/sdkman-init.sh\n");
        sb.append("jbang ");
        sb.append(script);
        for (String arg : args) {
            sb.append(" ");
            sb.append(arg);
        }
        sb.append("\n");

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "Run JBang");
        if (ifCondition != null) {
            command.put("if", ifCondition.getValue());
        }
        command.put("run", sb.toString());
        return command;
    }
}
