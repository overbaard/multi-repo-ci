package org.overbaard.ci.multi.repo.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class IssueStatusReportJobBuilder {
    private final int issueNumber;
    private Set<String> needs;
    private String successLabel;
    private String successMessage;
    private String failureLabel;
    private String failureMessage;

    public IssueStatusReportJobBuilder(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    public IssueStatusReportJobBuilder setNeeds(Set<String> needs) {
        this.needs = needs;
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
        List<Object> steps = new ArrayList<>();
        job.put("steps", steps);

        if (successLabel != null || successMessage != null) {
            steps.add(createScriptStep(IfCondition.SUCCESS, successLabel, failureLabel, successMessage));
        }
        if (failureLabel != null || failureMessage != null) {
            steps.add(createScriptStep(IfCondition.FAILURE, failureLabel, successLabel, failureMessage));
        }

        return job;
    }

    private Map<String, Object> createScriptStep(IfCondition ifCondition, String addLabel, String removeLabel, String issueComment) {
        GitScriptStepBuilder script = new GitScriptStepBuilder(issueNumber);
        script.setName(ifCondition == IfCondition.SUCCESS ? "report-success" : "report-failure");
        script.setIfCondition(ifCondition);
        if (addLabel != null) {
            script.setAddIssueLabels(addLabel);
        }
        if (removeLabel != null) {
            script.setRemoveIssueLabels(removeLabel);
        }
        // TODO remove stuff
        return script.build();
    }
}
