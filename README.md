# Overbård Multi Repo CI
Tooling for GitHub Actions to run CI across several interdependent repositories. 
It currently assumes Java projects, and Maven as the build tool.

On a large project, consisting of several components all from separate repositories, 
you might have several feature branches in several repositories to deliver the 
feature. The idea is to be able to test this feature as a whole from snapshots 
of each component involved.

A testsuite run is initiated via a GitHub issue in repositories for which this 
has been set up. This in turn results in a generated workflow to run the testsuite.

----------

# User Guide

## Triggering a CI Run

To have CI run for your feature, you create a GitHub issue with YAML to define 
the components involved. The repository in which you create the issue, must have 
had this set up. Here is an example for the 
[WildFly](http://github.com/wildfly/wildfly) project:

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


This yaml defines a CI run for a job spanning four components. For each component 
we define:
* The name of the component repository
* The organisation, or the user account to build it from
* The name of the branch containing the feature
* An optional `java-version` to use as a parameter to the generated [setup-java action](https://github.com/actions/setup-java). 
If not specified, whatever the administrator set up as the default will be used. 
This should only be specified for components that have a different java version 
than normal. 

Note that the organisation, may be any so you can use the upstream one, or your own, or your colleague's repositories. 
In addition, each component may have dependencies on a SNAPSHOT from another build. In the above 
example, we can see that:
* `jboss-remoting` depends on `xnio`
* `wildfly-core` depends on `xnio` and `jboss-remoting`
* `wildfly` depends on `wildfly-core`

Each component build is run in order according to the dependencies, and for each 
component build the tooling will determine the version of the component built and 
share it so it can be used by later components.

Each dependency has a `property` entry. This property is used to override the 
version for a given dependency. It assumes you are using properties to define 
versions in your POM files (and you really 
[should](https://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-best-practice.html)!).

Say that we ended up with the `3.8.4.Final-SHAPSHOT` as the version for the xnio 
built component. When running the jboss-remoting build the tooling will use the 
property name defined, so the maven command gets 
`-Dxnio.version=3.8.4.Final-SHAPSHOT` appended. This will override the value of 
that property set in jboss-remoting's 
[pom.xml](https://github.com/jboss-remoting/jboss-remoting/blob/master/pom.xml#L47).

For the wildfly-core build the pom property has a different name, so there 
`-Dversion.org.jboss.xnio=3.8.4.Final-SHAPSHOT` gets appended to the maven 
command to include the xnio SNAPSHOT. The same happpens for each snapshot dependency for 
each component.

In addition to the dependencies, you can tweak the maven build further by using 
the `mavenOpts` attribute. For xnio and jboss-remoting we have passed 
in `-DskipTests`. In those builds, that means that tests will not be run.

The version property-based mechanism described previously cannot work for testing an updated version of a project's parent, as a project parent must be statically defined in the project pom, and cannot be based on a maven property. To allow testing with updated versions of a parent, in the target component's definition you can use the `parent-version` property, set to the desired parent version. For example:

    ```
    name: JBoss Parent
    components:
      - name: jboss-parent-pom
        org: jboss
        branch: 'main'
        mavenOpts: -DskipTests
      - name: xnio
        org: kabir
        branch: '3.8'
        mavenOpts: -DskipTests
        parent-version: 40-SNAPSHOT
        dependencies:
          - name: jboss-parent-pom
            property: unused.placeholder
      - name: jboss-remoting
        org: jboss-remoting
        branch: '5.0'
        mavenOpts: -DskipTests
        java-version: 8
        parent-version: 40-SNAPSHOT
        dependencies:
          - name: xnio
            property: xnio.version
    ```

Here we first build a snapshot of the parent project `jboss-parent-pom`, and then in the `xnio` and `jboss-remoting` components we use `parent-version: 40-SNAPSHOT` to indicate we want those component's parent updated the `40-SNAPSHOT` before any maven task is executed.

Note the inelegant declaration in the `xnio` component of a dependency on the `jboss-parent-pom` component. This, like other dependency declarations, ensures that `jboss-parent-pom` is built first, making `40-SNAPSHOT` available. The inelegant bit is the `property: unused.placeholder` attribute, which is declared because `property` must be set to something. A meaningless value is used, which will result in the inclusion of a harmless but pointless`-Dunused.placeholder=40-SNAPSHOT` being passed to the maven build of `xnio`.

## Execution of the CI Run

Once GitHub Actions has picked up the issue, it will run a generator job 
(implemented by the code in the repository it has been set up for) which will generate a workflow YAML. 
This workflow YAML is pushed using the name `.github/workflows/ci-<issue id>.yaml` to a branch 
called `multi-repo-ci-branch-<issue id>` in the same repository as the issue 
was opened.

This `multi-repo-ci-branch-<issue id>` branch is also used to store snapshots of the 
artifacts created by each each build job. Those are pushed to this branch by the build job 
and overlaid onto the maven repo by each consuming job. This branch is deleted after 
running it so if you have any problems with your workflow, grab a copy of the 
`ci-<issue id>.yaml` file while it is still running!

If your repository has been configured to do so by the admin, issues will be labelled indicating CI 
success/failure and a comment will mention the SHAs of the branches used in this build.

## Viewing failures
Once the workflow has completed there will be a downloadable artifact attached to 
the job which contains the following for the jobs that have failed:
* All the files with a .log suffix.
* The surefire xml reports for the tests that had failures or errors.

In addition when viewing the workflow, the build logs for each job are available 
via the GitHub Actions UI.

---------
# Installation Guide
This section outlines how an organisation administrator would prepare a repository 
for use with this tooling.

You can either create a new repository in your organisation just to trigger 
this, or you can use your main repository if you don't mind adding the small 
number of files we discuss below.

Also, to save on GitHub Actions minutes in the GitHub organisation, you may 
choose to set this up in your personal repository.


## PAT/Secret
The first thing you need is to create a 
[GitHub Personal Access Token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line). 
I am using the following permissions for my token (it is currently a bit of a 
guess, and I might have some unneeded permissions there):
* repo
* admin:repo_hook
* user
* workflow

Then copy the PAT and store it in a 
[GitHub Secret](https://help.github.com/en/actions/configuring-and-managing-workflows/creating-and-storing-encrypted-secrets) 
in the organisation where you want the tool to run. You can also do this for your 
user account. The name of this secret must be `OB_MULTI_CI_PAT`.

## Enable the CI job
Copy [ci.yml](./ci.yml) to `.github/workflows/ci.yml` (you may change the name of 
the file as you like) in your repository.

## Configuring the repository
Create a `.repo-config/config.yml` file. An example:
```
env:
  MAVEN_OPTS: -Xmx1g -Xmx1g
  java-version: 11
```
Here we have set the maven opts environment variable to have the right size for 
our build. In addition we have set the java-version to use for the 
generated [setup-java action](https://github.com/actions/setup-java). This can be 
overridden by the user in the issue YAML. Also, in the next section we can set 
up the default java version to use when tailoring component builds.

### Status reporting
By default once the resulting workflow has terminated, a comment will be left on 
the issue indicating success or failure. If you want to turn this off, or 
add the ability to configure labels to be added to the issue 
indicating success or failure, add an `issue-reporting` section to the 
`.repo-config/config.yml`. The following turns off comments, and adds the label 
`Passed` on success, and the `Failure` label on failure.

```
issue-reporting:
  comments: false
  labels:
   success: Passed
   failure: Failure
```

From custom component builds and end jobs, you can write to a file indicated by the `${$OB_STATUS_TEXT}`
environment variable. If you have enabled comments when the job completes, the contents of this file
will be appended to the issue comment. This can be useful e.g.: 
* if you decide to upload resulting files from your workflow somewhere, and include links to those in 
the issue comment
* to include a link to the workflow run

## Custom component builds
By default with what we have seen so far, the tool will generate a workflow file 
which simply does the following steps for each component:
* Check out the specified repo + branch
* Set up the Maven caching
* Get the branch containing the workflow file and snapshots and overlay those onto the Maven repo 
* Set up Java
* Determine the version and store in a job output variable
* Inspect the other jobs' output variables to get the version of each dependency
* Run the Maven build. This is essentially a `mvn -B install` with adjustments made 
for the properties for the versions of our dependencies, as well as what we specified in 
`mavenOpts`
* If it is defined as the `build-job`, push the snapshots built by this build to the branch driving the build
* Grabbing the logs and surefire reports (but only if the job failed) and add to 
the shared log artifact

If a component you know you are going to build a lot has a large testsuite, 
you can benefit from splitting that up and running it in parallel. You do this 
by adding a `.repo-config/component-jobs/<component-name>.yml` to your repository. 
When generating the workflow YAML, when the tooling encounters a component in the
 issue YAML, it looks for a YAML file with the same name in the 
 `.repo-config/component-jobs/` folder. If that file is found, it customises 
 the build for that components.

As an example here is a snippet of the 
`.repo-config/component-jobs/wildfly-core.yml` that I use when building 
`wildfly-core` in the above example:

```
env:
  # Unfortunately GitHub Actions does not allow env vars to reference others so we need to duplicate the content
  MAVEN_SMOKE_TEST_PARAMS: -DfailIfNoTests=false -Dipv6 -Djboss.test.transformers.eap -Dci-cleanup=true -fae
  MAVEN_TEST_PARAMS: -DfailIfNoTests=false -Dipv6 -Djboss.test.transformers.eap -Dci-cleanup=true -fae -DallTests
```
As mentioned in the comments an env entry in GitHub Actions does not get 
substituted into other env entries.
```
java-version: 8
```
Here we say to use `8` as the version of java for all jobs in this component 
(note we can still override this again for each job). 
```
# the build job is the job that builds the component and determines its version. Other components depend on this job.
build-job: build
```
Jobs for components depending on this component will depend on the jobs indicated 
in the `build-step` entry. This way in our example, the `wildfly` component build 
only waits for the `build` job of `wildfly-core`, and **not** the full 
wildfly-core testsuite. Also, this job is what determines the version 
of `wildfly-core`.
```
jobs:
  # Build the server skipping tests for speed since other jobs depend on this. The maven repo is cached
  build:
```
This and all other build steps get prefixed with the component name in the 
generated workflow YAML. So this will show up as `wildfly-core-build` in the final 
generated workflow YAML.
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

Since the tooling 'massages' the `mvn` commands to adjust the properties to include 
snapshots of other components, any `mvn` command must happen in a `mvn` entry. 
Other shell commands happen in a `shell` entry. The tool will give an error if 
you try to use `mvn` from a `shell` entry.

Next the `mvn` command ends up having `-DskipTests` passed in
(from the `MAVEN_BUILD_EXTRA_PARAMS` env entry), which in this component's build 
means it will skip all unit tests and integration tests. This results in a build 
that is as fast as possible.
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
We depend on the above `build` job, so that will have completed before this job 
runs.
```
    java-version: 11
```
We can override the java version that we set up for the whole component for this 
particular job.
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
This job runs a sub-section of the testsuite in the testsuite/domain folder. 
We would do the same for other testsuite modules we want to run in parallel. 
Both `ts-smoke` and `ts-domain` wait for the `build` job to complete before 
they are run. But the nice thing is they can run in parallel, which speeds 
testing up a lot.

<a name="custom-component-build-vars"></a>
Each of the steps in a custom component build has access to the following env vars:
* `${OB_ISSUE_ID}` - contains the id of the issue that triggered the workflow
* `${$OB_PROJECT_VERSION}` - contains the captured version of the component being built
* `${OB_ARTIFACTS_DIR}` - contains the absoulute path of a directory that can be used to 
share files between jobs. This is part of the `multi-repo-ci-branch-<issue id>` branch 
used to drive the workflow. Essentially you can copy things into here, and read them from 
a later job in the workflow. Note that Git isn't ideal for sharing large files, so if you
have any problems you should find some other mechanism for external storage.
  * Behind the scenes if files put in here are bigger than 49MB they will be split. 
  So if you added the 200MB file `my-large-file.zip` you will end up with a directory called 
  `my-large-file.zip.split.file.dir`. That directory will contain files 
  created by splitting the original file, and a script called `reassemble.sh` which can
  be used to reassemble the file. However, these files will be merged into their original
  state before you can use them from any end jobs or custom component build jobs. It is only
  important if you intend to access the `multi-repo-ci-branch-<issue id>` branch directly.
* `${OB_VERSION_<COMPONENT_NAME>}` - There will be one of these for each component built 
before this component. So, e.g if the component was configured to depend on `my-component`, 
the resulting variable name will be `OB_VERSION_MY_COMPONENT`.
* `${$OB_STATUS_TEXT}`- Location of a file whose contents will be appended to the issue
comment once the workflow is done (if you configured issue comments to be made).
* `${OB_ISSUE_DATA_JSON}` - This initially contains the information from the issue body
that was used to trigger the job. It is a json file, which later jobs may modify, e.g. by
using `jq` from the command-line. Each component build job will add to this file so that
each component additionally contains `version` (showing the determined SNAPSHOT version)
and `sha` (showing the determined SHA-1 of the branch used for the component) entries.
Additionally, the example [ci.yml](./ci.yml) adds information about the url of the issue
that triggered the workflow, as well as the user who made the change.

## End jobs
In both the custom component build configuration files (e.g. `.repo-config/component-jobs/<component-name>.yml`) 
and the main repo config (`.repo-config/config.yml`) you can define end jobs. 

### Custom component build end job
The one in the custom component build configuration file will run after all the other jobs in the
custom component build configuration. They are set up by adding the following at the end of the 
`.repo-config/component-jobs/<component-name>.yml`:

```
end-job:
  if: ${{ always() }}
```
This can be `$${{ always() }}`, `$${{ sucess() }}` or `$${{ failure() }}`. If omitted the default
is `$${{ failure() }}` as in standard GitHub Actions.

We can also override the Java version to be used by using `java-version`:
```
  java-version: 11

```
If we don't specify the Java version, the one set up for the component build job is used, and if that
is not set up either we use the one set up as the default for the repository.

As before we can specify env entries:

```
  env: 
    MY_VAR: Hello
    OTHER: World
```

Then the rest is a `steps` entry which takes a set of steps defined in the normal GitHub Actions
syntax. You can use other actions as you would do if you supplied your own workflow:
```
  steps:
    - uses: actions/checkout@v2
      with:
        repository: some/other-tool
        path: .maven-repo-generator
```

If you want to run `mvn` commands you may do so. Note that this differs from the `mvn` vs `shell`
commands for the previous jobs we defined in this file. If you want to run mvn it is
***very important to remember to use the `${OB_MAVEN_DEPENDENCY_VERSIONS}` variable***. Otherwise,
it will ***not*** pull in the snapshots from the other jobs.

Here we run the maven command to assemble the full WildFly server zip and to move it to
`${OB_ARTIFACTS_DIR}` so it can be used by later jobs:
```
    - run: |
        mvn install -pl dist -Prelease ${OB_MAVEN_DEPENDENCY_VERSIONS}
        mv dist/target/wildfly-${OB_PROJECT_VERSION}.zip ${OB_ARTIFACTS_DIR}/wildfly.zip
```

Before the `steps` are run, the same boilerplate as before is run for these component end jobs. So 
we will have a clone of the project, Java set up, the snapshots from other jobs copied into the
cached maven repository etc.

Available environment variables are the same as [mentioned above](#custom-component-build-vars) for 
the other component build jobs so you have access to `OB_ARTIFACTS_DIR`, `OB_VERSION_<COMPONENT_NAME>` 
and `OB_PROJECT_VERSION`. They all have the same meaning. In addition as we have seen we have the 
`${OB_MAVEN_DEPENDENCY_VERSIONS}` variable. `${OB_MAVEN_DEPENDENCY_VERSIONS}` contains the 
system properties to override the versions for the component so it ***must*** be appended onto
any `mvn` commands you do as part of a custom component build end job.

### Workflow end job
To add a workflow end job, you define it in the same way as you would define a custom component build
end job, but this time you define it in `.repo-config/config.yml`. You end up with a lighter 
environment than you did before:
* Java is set up
* You have access to the `OB_ISSUE_ID`, OB_ARTIFACTS_DIR`, `$OB_STATUS_TEXT`, `OB_ISSUE_DATA_JSON` and the
`OB_VERSION_<COMPONENT_NAME>` environment variables as [mentioned previously](#custom-component-build-vars)
  * The `$OB_ARTIFACTS_DIR` is populated with whatever files we put into there earlier. Note 
  that copying anything into `$OB_ARTIFACTS_DIR` at this stage is possible but has no effect
  on things happening later in the generated workflow.
  
A good use for a workflow end job could be to upload things from the `OB_ARTIFACTS_DIR` to more 
permanent storage, e.g here we access it to :
```
end-job:
  # You can set env vars if you wish:
  # env:
  #  MY_VAR: Test  
  #
  # 'java-version' is optional.
  # java-version: 11
  #
  # See 'Self-hosted runners' for description of 'runs-on'
  # runs-on: ["ubuntu-latest"]
  steps:
    - name: Upload file
      run: |-
        echo Uploading ${OB_ARTIFACTS_DIR}/wildfly.zip
        # Curl stuff to upload ${OB_ARTIFACTS_DIR}/wildfly.zip somewhere
```
Similar to the custom component build end jobs, you can also add other actions.
  
### Self-hosted runners
By default, a value of 
```
runs-on: 
  - ubuntu-latest
```
is used for all the generated jobs.

You can override this:
* in `.repo-config/config.yml` both at:
  * the 'global' level
  * the workflow end job
* in the `.repo-config/component-jobs/<component-name>.yml` files:
  * on the top level
  * in the custom component build jobs
  * in the custom component build end job
  
Note that the intent of this mechanism is to enable you to run parts of the workflow on 
your self-hosted runners. If you are on a public repository, you need to be careful!
An incomplete list of ideas to make this safer are:
* To set up firewall rules on the machines/VMs you are hosting the runner on so they cannot wreak
havoc on your internal network
* Only allow users who are trusted to open the issues triggering these jobs in your repository.

> :warning: There is not anything stopping you from passing in e.g `windows-latest` but the ***generated 
workflow currently only works with Linux!*** An `macos-xxx` runner may work, but this is untested.
