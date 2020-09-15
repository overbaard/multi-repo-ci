package org.overbaard.ci.multi.repo.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitScriptStepBuilder {

    private static final String SCRIPT_NAME = "actions/github-script@v3";

    private final int issueId;
    private IfCondition ifCondition;
    private String name;

    Set<String> addIssueLabels = new HashSet<>();
    Set<String> removeIssueLabels = new HashSet<>();

    public GitScriptStepBuilder(int issueId) {
        this.issueId = issueId;
    }

    public GitScriptStepBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public GitScriptStepBuilder setAddIssueLabels(String... labels) {
        this.addIssueLabels.addAll(Arrays.asList(labels).stream().filter(l -> l != null).collect(Collectors.toList()));
        return this;
    }

    public GitScriptStepBuilder setRemoveIssueLabels(String... labels) {
        this.removeIssueLabels.addAll(Arrays.asList(labels).stream().filter(l -> l != null).collect(Collectors.toList()));
        return this;
    }

    public GitScriptStepBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> step = new LinkedHashMap<>();
        if (name != null) {
            step.put("name", name);
        }
        step.put("uses", SCRIPT_NAME);
        if (ifCondition != null) {
            step.put("if", ifCondition.getValue());
        }

        Map<String, Object> with = new LinkedHashMap<>();
        step.put("with", with);
        with.put("github-token", "${{ secrets.OB_MULTI_CI_PAT }}");



        StringBuilder script = new StringBuilder();
        addIssueLabels(script);
        removeIssueLabels(script);


        with.put("script", script.toString());
        return step;
    }

    private void addIssueLabels(StringBuilder script) {
        if (addIssueLabels.size() > 0) {
            script.append("await github.issues.addLabels({\n");
            script.append("  issue_number: " + issueId + ",\n");
            script.append("  owner: context.repo.owner,\n");
            script.append("  repo: context.repo.repo,\n");
            script.append("  labels: " + createScriptArray(addIssueLabels) + "\n");
            script.append("})\n");
        }
    }

    private String createScriptArray(Collection<String> input) {
        StringBuilder array = new StringBuilder();
        array.append("[");
        for (String s : input) {
            if (array.length() > 1) {
                array.append(", ");
            }
            array.append("'" + s + "'");
        }
        array.append("]");
        return array.toString();
    }

    private void removeIssueLabels(StringBuilder script) {
        if (removeIssueLabels.size() == 0) {
            return;
        }
        script.append("const labelsOnIssueRet = await github.issues.listLabelsOnIssue({\n");
        script.append("  issue_number: " + issueId + ",\n");
        script.append("  owner: context.repo.owner,\n");
        script.append("  repo: context.repo.repo,\n");
        script.append("})\n");
        script.append("const labelsOnIssue = await github.paginate(labelsOnIssueRet)\n");
        script.append("const labelsToRemoveFromIssue = " + createScriptArray(removeIssueLabels) + "\n");
        script.append("for (const labelToRemove of labelsToRemoveFromIssue) {\n");
        script.append("  console.log('Want to remove label: ' + labelToRemove)\n");
        script.append("  for (const label of labelsOnIssue) {\n");
        script.append("    console.log('Match ' + label['name'] + '?')\n");
        script.append("    if (label['name'] === labelToRemove) {\n");
        script.append("      console.log('Removing label ' + label['name'])\n");
        script.append("      await github.issues.removeLabel({\n");
        script.append("        issue_number: " + issueId + ",\n");
        script.append("        owner: context.repo.owner,\n");
        script.append("        repo: context.repo.repo,\n");
        script.append("        name: label['name']\n");
        script.append("      })\n");
        script.append("      break;\n");
        script.append("    }\n");
        script.append("  }\n");
        script.append("}\n");
    }
}
