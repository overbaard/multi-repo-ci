package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitCommandBuilder {
    private String workingDirectory;
    private String gitUser;
    private String gitEmail;
    private String[] addFiles = new String[0];
    private String commitMessage;
    private boolean push;
    private boolean rebase;
    private IfCondition ifCondition;

    GitCommandBuilder setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    GitCommandBuilder setUserAndEmail(String gitUser, String gitEmail) {
        this.gitUser = gitUser;
        this.gitEmail = gitEmail;
        return this;
    }

    GitCommandBuilder addFiles(String... addFiles) {
        this.addFiles = addFiles;
        return this;
    }

    GitCommandBuilder setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
        return this;
    }

    GitCommandBuilder setRebase() {
        this.rebase = true;
        return this;
    }

    GitCommandBuilder setPush() {
        this.push = true;
        return this;
    }

    public GitCommandBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "Git command-line work");
        if (workingDirectory != null) {
            command.put("working-directory", workingDirectory);
        }

        StringBuilder run = new StringBuilder();
        if (gitUser != null) {
            run.append("git config --global user.name \"" + gitUser + "\"\n");
        }
        if (gitEmail != null) {
            run.append("git config --global user.email \"" + gitEmail + "\"\n");
        }
        if (addFiles.length > 0) {
            run.append("git add");
            for (String file : addFiles) {
                run.append(" ");
                run.append(file);
            }
            run.append("\n");
        }
        if (commitMessage != null) {
            run.append("git commit -m \"" + commitMessage + "\"\n");
        }

        if (push || rebase) {
            run.append("TMP=$(git branch | sed -n -e 's/^\\* \\(.*\\)/\\1/p')\n");
            //Rebase before doing the push
            run.append("git fetch origin ${TMP}\n");
            run.append("git rebase origin/${TMP}\n");
        }

        if (push) {
            run.append("git push origin ${TMP}\n");
        }

        command.put("run", run.toString());

        if (ifCondition != null) {
            command.put("if", ifCondition.getValue());
        }

        return command;
    }
}
