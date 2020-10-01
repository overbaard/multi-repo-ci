package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitCommandStepBuilder {
    private String workingDirectory;
    private String gitUser;
    private String gitEmail;
    private String[] addFiles = new String[0];
    private String[] removeFiles = new String[0];
    private String commitMessage;
    private boolean push;
    private boolean deleteRemoteBranch;
    private boolean rebase;
    private IfCondition ifCondition;

    GitCommandStepBuilder setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    GitCommandStepBuilder setStandardUserAndEmail() {
        this.gitUser = "CI Action";
        this.gitEmail = "ci@example.com";
        return this;
    }

    GitCommandStepBuilder addFiles(String... addFiles) {
        this.addFiles = addFiles;
        return this;
    }

    GitCommandStepBuilder removeFiles(String... removeFiles) {
        this.removeFiles = removeFiles;
        return this;
    }

    GitCommandStepBuilder setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
        return this;
    }

    GitCommandStepBuilder setRebase() {
        this.rebase = true;
        return this;
    }

    GitCommandStepBuilder setPush() {
        this.push = true;
        return this;
    }

    public GitCommandStepBuilder setDeleteRemoteBranch() {
        this.deleteRemoteBranch = true;
        return this;
    }

    public GitCommandStepBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "Git command-line work");
        if (workingDirectory != null) {
            command.put("working-directory", workingDirectory);
        }
        if (ifCondition != null) {
            command.put("if", ifCondition.getValue());
        }

        StringBuilder run = new StringBuilder();
        if (gitUser != null) {
            run.append("git config --local user.name \"" + gitUser + "\"\n");
        }
        if (gitEmail != null) {
            run.append("git config --local user.email \"" + gitEmail + "\"\n");
        }
        if (addFiles.length > 0) {
            run.append("git add");
            for (String file : addFiles) {
                run.append(" ");
                run.append(file);
            }
            run.append("\n");
        }
        if (removeFiles.length > 0) {
            run.append("git rm ");
            for (String file : removeFiles) {
                run.append(file);
                run.append(" ");
            }
            run.append("\n");
        }
        if (commitMessage != null) {
            run.append("branch_status=$(git status --porcelain)\n");
            run.append("[[ ! -z \"${branch_status}}\" ]] && git commit -m \"" + commitMessage + "\" || echo \"No changes\"\n");
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

        if (deleteRemoteBranch) {
            run.append("TMP=$(git branch | sed -n -e 's/^\\* \\(.*\\)/\\1/p')\n");
            run.append("git push origin :${TMP}\n");
        }

        command.put("run", run.toString());


        return command;
    }

}
