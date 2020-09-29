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

    private final String jobName;
    private final int issueNumber;
    private final boolean success;
    private final IfCondition ifCondition;
    private Set<String> needs;
    private String addLabel;
    private String removeLabel;
    private String comment;
    private Map<String, String> jobNamesAndVersionVariables = new LinkedHashMap<>();
    private String statusOutputVariableName;
    private String statusOutputOutputName;

    public IssueStatusReportJobBuilder(String jobName, int issueNumber, boolean success) {
        this.jobName = jobName;
        this.issueNumber = issueNumber;
        this.success = success;
        this.ifCondition = success ? IfCondition.SUCCESS : IfCondition.FAILURE;
    }

    public IssueStatusReportJobBuilder setNeeds(Set<String> needs) {
        this.needs = needs;
        return this;
    }

    public IssueStatusReportJobBuilder setJobNamesAndVersionVariables(Map<String, String> jobNamesAndVersionVariables) {
        this.jobNamesAndVersionVariables.putAll(jobNamesAndVersionVariables);
        return this;
    }

    public IssueStatusReportJobBuilder setAddLabel(String addLabel){
        this.addLabel = addLabel;
        return this;
    }

    public IssueStatusReportJobBuilder setRemoveLabel(String removeLabel){
        this.removeLabel = removeLabel;
        return this;
    }

    public IssueStatusReportJobBuilder setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public IssueStatusReportJobBuilder setStatusOutputVariableAndRef(String statusOutputVariableName, String statusOutputOutputName) {
        this.statusOutputVariableName = statusOutputVariableName;
        this.statusOutputOutputName = statusOutputOutputName;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("name", "Issue Status Report - " + ( success ? "Success" : "Failure"));
        job.put("runs-on", "ubuntu-latest");
        job.put("if", ifCondition.getValue());
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

        env.put(statusOutputVariableName, "${{" + statusOutputOutputName + "}}");
        job.put("env", env);

        List<Object> steps = new ArrayList<>();
        job.put("steps", steps);


        GitScriptStepBuilder script = new GitScriptStepBuilder(issueNumber);
        script.setName("report-status");
        if (addLabel != null) {
            script.setAddIssueLabels(addLabel);
        }
        if (removeLabel != null) {
            script.setRemoveIssueLabels(removeLabel);
        }
        if (comment != null) {
            StringBuilder formattedComment = new StringBuilder();
            // We need to format this for javascript, which is a bit odd
            formattedComment.append("'" + comment + "\\n\\n'\n");
            if (shaEnvVarsForJobs.size() > 0) {
                formattedComment.append("  + 'These are the job names and their respective SHA-1 hashes:\\n\\n'\n");
                for (Map.Entry<String, String> entry : shaEnvVarsForJobs.entrySet()) {
                    String component = entry.getKey();
                    // We need this prefix so node can find it
                    String shaEnvVar = NODE_ENV_PREFIX + entry.getValue();
                    formattedComment.append("  + '" + component + ": ' + " + shaEnvVar + " + '\\n'\n");
                }
            }
            if (statusOutputVariableName != null) {
                formattedComment.append("  + '\\n'\n");
                formattedComment.append("  + " + NODE_ENV_PREFIX + statusOutputVariableName + " + '\\n'\n");
            }
            script.setIssueComment(formattedComment.toString());
        }

        steps.add(script.build());

        return job;
    }

    private Map<String, Object> createScriptStep(Map<String, String> shaEnvVarsForJobs, String addLabel, String removeLabel, String issueComment) {
        GitScriptStepBuilder script = new GitScriptStepBuilder(issueNumber);
        script.setName("report-status");
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
            if (statusOutputVariableName != null) {
                formattedComment.append("  + '\\n'\n");
                formattedComment.append("  + " + NODE_ENV_PREFIX + statusOutputVariableName + " + '\\n'\n");
            }
            script.setIssueComment(formattedComment.toString());
        }

        return script.build();
    }

}
