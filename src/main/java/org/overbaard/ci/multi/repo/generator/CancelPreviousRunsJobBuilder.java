package org.overbaard.ci.multi.repo.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CancelPreviousRunsJobBuilder {

    private final String branchName;

    public CancelPreviousRunsJobBuilder(String branchName) {
        this.branchName = branchName;
    }

    public Map<String, Object> build() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");

        List<Object> steps = new ArrayList<>();
        job.put("steps", steps);

        steps.add(new CheckoutBuilder()
                    .setBranch(branchName)
                    .build());

        Map<String, Object> cancelStep = new LinkedHashMap<>();
        steps.add(cancelStep);
        cancelStep.put("uses", "n1hility/cancel-previous-runs@v2");
        // Ok to use GITHUB_TOKEN here since it happens early in the workflow, before it can time out
        cancelStep.put("with", Collections.singletonMap("token", "${{ secrets.GITHUB_TOKEN }}"));

        return job;
    }

}
