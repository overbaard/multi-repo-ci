package org.overbaard.ci.multi.repo.generator;

import org.overbaard.ci.multi.repo.ToolCommand;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitHubActionGeneratorToolCommand implements ToolCommand {
    @Override
    public String getDescription() {
        return "Generates a GitHub workflow YAML from the trigger issue input";
    }

    @Override
    public void invoke(String[] args) throws Exception {
        GitHubActionGenerator.generate(args);
    }
}
