package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class CheckoutBuilder {
    private String repo;
    private String branch;
    private String path;

    CheckoutBuilder setRepo(String org, String repo) {
        this.repo = org + "/" + repo;
        return this;
    }

    CheckoutBuilder setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    CheckoutBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> checkout = new LinkedHashMap<>();
        checkout.put("uses", "actions/checkout@v2");
        Map<String, Object> with = buildWith();
        if (with.size() != 0) {
            checkout.put("with", with);
        }
        return checkout;
    }

    private Map<String, Object> buildWith() {
        Map<String, Object> with = new LinkedHashMap<>();

        if (repo != null) {
            with.put("repository", repo);
        }
        if (branch != null) {
            with.put("ref", branch);
        }
        if (path != null) {
            with.put("path", path);
        }
        with.put("fetch-depth", 0);

        return with;
    }
}
