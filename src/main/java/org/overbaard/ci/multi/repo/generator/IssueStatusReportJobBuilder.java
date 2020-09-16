package org.overbaard.ci.multi.repo.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class IssueStatusReportJobBuilder {

    private static final String NODE_ENV_PREFIX = "process.env.";

    private final int issueNumber;
    private Set<String> needs;
    private String successLabel;
    private String successMessage;
    private String failureLabel;
    private String failureMessage;
    private Map<String, String> jobNamesAndVersionVariables = new LinkedHashMap<>();

    public IssueStatusReportJobBuilder(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    public IssueStatusReportJobBuilder setNeeds(Set<String> needs) {
        this.needs = needs;
        return this;
    }

    public IssueStatusReportJobBuilder setJobNamesAndVersionVariables(Map<String, String> jobNamesAndVersionVariables) {
        this.jobNamesAndVersionVariables.putAll(jobNamesAndVersionVariables);
        return this;
    }

    public IssueStatusReportJobBuilder setSuccessLabel(String successLabel){
        this.successLabel = successLabel;
        return this;
    }

    public IssueStatusReportJobBuilder setFailureLabel(String failureLabel) {
        this.failureLabel = failureLabel;
        return this;
    }

    public IssueStatusReportJobBuilder setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
        return this;
    }


    public IssueStatusReportJobBuilder setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("name", "Issue Status Report");
        job.put("runs-on", "ubuntu-latest");
        if (needs != null) {
            job.put("needs", new ArrayList<>(needs));
        }
        // 'Translate' the variables containing the SHAs since they contain hyphens
        // and those don't work in the JavaScript world.
        Map<String, Object> env = new LinkedHashMap<>();
        Map<String, String> shaEnvVarsForJobs = new HashMap<>();
        for (Map.Entry<String, String> entry : jobNamesAndVersionVariables.entrySet()) {
            String envVar = entry.getKey().replace('-', '_') + "_git_sha";
            env.put(envVar, "${{" + entry.getValue() + "}}");
            shaEnvVarsForJobs.put(entry.getKey(), envVar);
        }
        job.put("env", env);


        List<Object> steps = new ArrayList<>();
        job.put("steps", steps);

        if (successLabel != null || successMessage != null) {
            steps.add(createScriptStep(IfCondition.SUCCESS, shaEnvVarsForJobs, successLabel, failureLabel, successMessage));
        }
        if (failureLabel != null || failureMessage != null) {
            steps.add(createScriptStep(IfCondition.FAILURE, shaEnvVarsForJobs, failureLabel, successLabel, failureMessage));
        }

        return job;
    }

    private Map<String, Object> createScriptStep(IfCondition ifCondition, Map<String, String> shaEnvVarsForJobs, String addLabel, String removeLabel, String issueComment) {
        GitScriptStepBuilder script = new GitScriptStepBuilder(issueNumber);
        script.setName(ifCondition == IfCondition.SUCCESS ? "report-success" : "report-failure");
        script.setIfCondition(ifCondition);
        if (addLabel != null) {
            script.setAddIssueLabels(addLabel);
        }
        if (removeLabel != null) {
            script.setRemoveIssueLabels(removeLabel);
        }
        if (issueComment != null) {
            StringBuilder formattedComment = new StringBuilder();
            // We need to format this for javascript, which is a bit odd
            formattedComment.append("'" + issueComment + "\\n\\n'\n");
            if (shaEnvVarsForJobs.size() > 0) {
                formattedComment.append("  + 'These are the job names and their respective SHA-1 hashes:\\n\\n'\n");
                for (Map.Entry<String, String> entry : shaEnvVarsForJobs.entrySet()) {
                    String component = entry.getKey();
                    // We need this prefix so
                    String env = NODE_ENV_PREFIX + entry.getValue();
                    formattedComment.append("  + '" + component + ": ' + " + env + " + '\\n'\n");
                }
            }
            script.setIssueComment(formattedComment.toString());
        }

        return script.build();
    }

}
