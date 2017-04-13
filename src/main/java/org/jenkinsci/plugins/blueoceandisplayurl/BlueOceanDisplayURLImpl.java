package org.jenkinsci.plugins.blueoceandisplayurl;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.branch.MultiBranchProject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;


/**
 *`@author Ivan Meredith
 */
@Extension
public class BlueOceanDisplayURLImpl extends DisplayURLProvider {

    private static final Set<String> SUPPORTED_RUNS = ImmutableSet.of(
            FreeStyleBuild.class.getName(),
            WorkflowRun.class.getName(),
            "hudson.maven.AbstractMavenBuild"
    );

    private static final Set<String> SUPPORTED_JOBS = ImmutableSet.of(
            WorkflowJob.class.getName(),
            MultiBranchProject.class.getName(),
            FreeStyleProject.class.getName(),
            "hudson.maven.AbstractMavenProject"
    );

    @Override
    public String getDisplayName() {
        return "Blue Ocean";
    }

    @Override
    public String getRoot() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not started");
        }
        String root = jenkins.getRootUrl();
        if (root == null) {
            root = "http://unconfigured-jenkins-location/";
        }
        return root + "blue/";
    }

    @Override
    public String getRunURL(Run<?, ?> run) {
        if (isSupported(run)) {
            if (run instanceof WorkflowRun) {
                WorkflowJob job = ((WorkflowRun) run).getParent();
                if (job.getParent() instanceof MultiBranchProject) {
                    return getJobURL(((MultiBranchProject) job.getParent())) + "detail/" + Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
                }
            }
            Job job = run.getParent();
            return getJobURL(job) + "detail/" + Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
        } else {
            return DisplayURLProvider.getDefault().getRunURL(run);
        }
    }

    @Override
    public String getChangesURL(Run<?, ?> run) {
        if (isSupported(run)) {
            return getRunURL(run) + "changes";
        } else {
            return DisplayURLProvider.getDefault().getChangesURL(run);
        }
    }

    @Override
    public String getJobURL(Job<?, ?> job) {
        if (isSupported(job)) {
            String jobPath;
            if(job.getParent() instanceof MultiBranchProject) {
                jobPath = Util.rawEncode(job.getParent().getFullName());
            } else {
                jobPath = Util.rawEncode(job.getFullName());
            }
            return getRoot() + "organizations/jenkins/" + jobPath + "/";
        } else {
            return DisplayURLProvider.getDefault().getJobURL(job);
        }
    }

    private static boolean isSupported(Run<?, ?> run) {
        return isInstance(run, SUPPORTED_RUNS);
    }

    private static boolean isSupported(Job<?, ?> job) {
        return isInstance(job, SUPPORTED_JOBS);
    }

    private static boolean isInstance(Object o, Set<String> clazzes) {
        for (String clazz : clazzes) {
            if (o != null && o.getClass().getName().equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private String getJobURL(MultiBranchProject<?, ?> project) {
        String jobPath = Util.rawEncode(project.getFullName());

        return getRoot() + "organizations/jenkins/" + jobPath + "/";
    }
}
