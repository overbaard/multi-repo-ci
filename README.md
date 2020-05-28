# multi-repo-ci
Tooling for GitHub Actions to run CI across several interdependent repositories. It currently assumes Java projects, and Maven as the build tool.

On a large project, consisting of several components all from separate repositories, you might have several feature branches in several repositories to deliver the feature. The idea is to be able to test this feature as a whole from snapshots of each component involved.

A testsuite run is initiated via a GitHub issue in repositories for which this has been set up. This in turn results in a generated workflow to run the testsuite.


## User Guide
To have CI run for your feature, you create a GitHub issue with YAML to define the components involved. The repository in which you create the issue, must have had this set up. Here is an example for the [WildFly](http://github.com/wildfly/wildfly) project:

```
name: XNIO and Remoting
env:
  # We can set env vars which will get used by the resulting workflow
  MY_VAR: Hello!!
components:
  - name: xnio
    org: kabir
    branch: '3.8'
    mavenOpts: -DskipTests
  - name: jboss-remoting
    org: jboss-remoting
    branch: '5.0'
    mavenOpts: -DskipTests
    java-version: 8
    dependencies:
      - name: xnio
        property: xnio.version
  - name: wildfly-core
    org: wildfly
    branch: master
    dependencies:
      - name: xnio
        property: version.org.jboss.xnio
      - name: jboss-remoting
        property: version.org.jboss.remoting
  - name: wildfly
    org: wildfly
    branch: master
    dependencies:
      - name: wildfly-core
        property: version.org.wildfly.core
```
This yaml defines a CI run for a job spanning four components. For each component we define:
* The name of the component repository
* The organisation, or the user account to build it from
* The name of the branch containing the feature
* An optional `java-version` to use as a parameter to the generated [setup-java action](https://github.com/actions/setup-java). 
If not specified, whatever the administrator set up as the default will be used. This should only be specified for
components that have a different java version than normal. 

Although I have used the upstream organisation for each of the components, they could point to branches in your (or your team mates') repositories. In addition, each component may have dependencies on a SNAPSHOT from another build. In the above example, we can see that:
* `jboss-remoting` depends on `xnio`
* `wildfly-core` depends on `xnio` and `jboss-remoting`
* `wildfly` depends on `wildfly-core`

Each component build is run in order according to the dependencies, and for each component build the tooling will determine the version of the component built and share it so it can be used by later components.

Each dependency has a `property` entry. This property is used to override the version for a given dependency. It assumes you are using properties to define versions in your POM files (and you really [should](https://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-best-practice.html)!).

Say that we ended up with the `3.8.4.Final-SHAPSHOT` as the version for the xnio built component. When running the jboss-remoting build the tooling will use the property name defined, so the maven command gets `-Dxnio.version=3.8.4.Final-SHAPSHOT` appended. This will override the value of that property set in jboss-remoting's [pom.xml](https://github.com/jboss-remoting/jboss-remoting/blob/master/pom.xml#L47).

For the wildfly-core build the pom property has a different name, so there `-Dversion.org.wildfly.core=3.8.4.Final-SHAPSHOT` gets appended to the maven command to include the xnio SNAPSHOT. The same happpens for each dependency for each component.

In addition to the dependencies, you can tweak the maven build further by using the `mavenOpts` attribute. For xnio and jboss-remoting we have passed in `-DskipTests`. In those builds, that means that tests will not be run.

Once GitHub Actions has picked up the issue, it will run a generator job (implemented by the code in this repository) which will generate a workflow YAML. This workflow YAML is pushed using the name `ci-<issue id>.yaml` to a branch called `branch-<issue id>` in the same repository as the issue was opened.

TODO
* [Label trigger issue to show if resulting run passed or failed](https://github.com/overbaard/multi-repo-ci/issues/1)
* [Add comment on trigger issue with link to test run](https://github.com/overbaard/multi-repo-ci/issues/2)

### Viewing failures
Once the workflow has completed there will be a downloadable artifact attached to the job which contains the following for the jobs that have failed:
* All the files with a .log suffix.
* The surefire xml reports for the tests that had failures or errors.

In addition when viewing the workflow, the build logs for each job are available via the GitHub Actions UI.

## Install Guide
This section outlines how an organisation administrator would prepare a repository for use with this tooling.

You can either create a new repository in your organisation just to trigger this, or you can use your main repository if you don't mind adding the small number of files we discuss below.

Also, to save on GitHub Actions minutes in the GitHub organisation, you may choose to set this up in your personal repository.


### PAT/Secret
The first thing you need is to create a [GitHub Personal Access Token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line). I am using the following permissions for my token (it is currently a bit of a guess, and I might have some unneeded permissions there):
* repo
* admin:repo_hook
* user
* workflow

Then copy the PAT and store it in a [GitHub Secret](https://help.github.com/en/actions/configuring-and-managing-workflows/creating-and-storing-encrypted-secrets) in the organisation where you want the tool to run. You can also do this for your user account. The name of this secret must be `OB_MULTI_CI_PAT`.

### Enable the CI job
Copy [ci.yml](./ci.yml) to `.github/workflows/ci.yml` (you may change the name of the file as you like) in your repository.

### Configuring the repository
Create a `.repo-config/config.yml` file. An example:
```
env:
  MAVEN_OPTS: -Xmx1g -Xmx1g
java-version: 11
```
Here we have set the maven opts environment variable to have the right size for our build. In addition we have set
the java-version to use for the generated [setup-java action](https://github.com/actions/setup-java). This can be 
overridden by the user in the issue YAML. Also, in the next section we can set up the default java version to use when 
tailoring component builds.

### Tailoring build
By default with what we have seen so far, the tool will generate a workflow file which simply does the following steps for each component:
* Check out the specified repo + branch
* Set up the Maven caching
* Set up Java
* Determine the version and upload an artifact containing that version
* Download the version artifacts for each dependency and set those in environment variables
* Run the Maven build. This is essentially a `mvn -B install` with adjustments made for the properties for the versions of our dependencies and what we specified in `mavenOpts`
* Grabbing the logs and surefire reports (but only if the job failed) and add to the shared log artifact

If a component you know you are going to build a lot has a large testsuite, you can benefit from splitting that up and running it in parallel. You do this by adding a `.repo-config/component-jobs/<component-name>.yaml` to your repository. When generating the workflow YAML, when the tooling encounters a component in the issue YAML, it looks for a YAML file with the same name in the `.repo-config/component-jobs/` folder. If that file is found, it customises the build for that components.

As an example here is a snippet of the `.repo-config/component-jobs/wildfly-core.yaml` that I use when building `wildfly-core` in the above example:

```
env:
  # Unfortunately GitHub Actions does not allow env vars to reference others so we need to duplicate the content
  MAVEN_SMOKE_TEST_PARAMS: -DfailIfNoTests=false -Dipv6 -Djboss.test.transformers.eap -Dci-cleanup=true -fae
  MAVEN_TEST_PARAMS: -DfailIfNoTests=false -Dipv6 -Djboss.test.transformers.eap -Dci-cleanup=true -fae -DallTests
```
As mentioned in the comments an env entry in GitHub Actions does not get substituted into other env entries.
```
java-version: 8
```
Here we say to use `8` as the version of java for all jobs in this component (note we can still override this again 
for each job). 
```
# exported jobs are the jobs that the components builds in the issue yaml depending on this component will depend upon
exported-jobs: [build]
```
Jobs for components depending on this component will depend on the jobs listed in the `exported-jobs` entry. This way
in our example, the `wildfly` component build only waits for the `build` job of `wildfly-core`, and **not** the full wildfly-core testsuite.
```
jobs:
  # Build the server skipping tests for speed since other jobs depend on this. The maven repo is cached
  build:
```
This and all other build steps get prefixed with the component name in the generated workflow YAML. So this will show up as `wildfly-core-build` in the final generated workflow YAML.
```
    env:
      MAVEN_BUILD_EXTRA_PARAMS: -DlegacyBuild -DlegacyRelease -DskipTests
    run:
      # Do not quote the usage of the env vars, or the quotes get wrapped in single quutes,
      # which means they don't take effect (e.g. -DskipTests from MAVEN_BUILD_EXTRA_PARAMS will not work!)
      - mvn: install ${MAVEN_TEST_PARAMS} ${MAVEN_BUILD_EXTRA_PARAMS}
      - shell: echo "build done!"
```
A few things here.

Since the tooling 'massages' the `mvn` commands to adjust the properties to include snapshots of other components, any `mvn` command must happen in a `mvn` entry. Other shell commands happen in a `shell` entry. The tool will give an error if you try to use mvn from a `shell` entry.

Next the mvn command ends up having `-DskipTests` passed in (from the `MAVEN_BUILD_EXTRA_PARAMS` env entry), which in this component's build means it will skip all unit tests and integration tests. This results in a build that is as fast as possible.
```
  #####################################
  # Parallel tests - depend on wildfly-core-build

  # Build the server again and run all the unit and smoke tests
  # it is different from the other testsuite tests in that we want to build everything up to and including
  # the smoke tests

  ts-smoke:
    # This does the unit tests for all the components and the testsuite
    # moduled which are run when not passing in -DallTests, i.e.
    # - standalone
    # - elytron
    # - embedded
    # - scripts
    needs: [build]
```
We depend on the above `build` job, so that will have completed before this job runs.
```
    java-version: 11
```
We can override the java version that we set up for the whole component for this particular job.
```
    run:
      - mvn: package ${MAVEN_SMOKE_TEST_PARAMS}
```
This will use the cached maven repository to get the artifacts created by the `build` job. Since `-DskipTests` is not
passed in now, all unit and smoke integration tests are run.

Note that we use `package` here to not pollute the cached maven repository.
```

  ts-domain:
    needs: [build]
    run:
      - mvn: package ${MAVEN_TEST_PARAMS} -pl testsuite/domain
```
This job runs a sub-section of the testsuite in the testsuite/domain folder. We would do the same for other testsuite modules we want to run in parallel. Both `ts-smoke` and `ts-domain` wait for the `build` job to complete before they are run. But the nice thing is they can run in parallel, which speeds testing up a lot.




