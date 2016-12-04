package org.jenkinsci.plugins.blueoceandisplayurl;

import hudson.Extension;
import hudson.Util;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import hudson.tasks.test.TestResult;
import jenkins.branch.MultiBranchProject;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;


/**
 *`@author Ivan Meredith
 */
@Extension
public class BlueOceanDisplayURLImpl extends DisplayURLProvider {

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
        if(run instanceof WorkflowRun) {
            WorkflowJob job =  ((WorkflowRun) run).getParent();
            if(job.getParent() instanceof MultiBranchProject) {
                return getJobURL(((MultiBranchProject) job.getParent()))+ "detail/" +  Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
            }
        }

        Job job = run.getParent();
        return getJobURL(job) + "detail/" + Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
    }

    @Override
    public String getChangesURL(Run<?, ?> run) {
        return getRunURL(run) + "changes";
    }

    public String getJobURL(MultiBranchProject<?, ?> project) {
        String jobPath = Util.rawEncode(project.getFullName());

        return getRoot() + "organizations/jenkins/" + jobPath + "/";
    }
    @Override
    public String getJobURL(Job<?, ?> project) {
        String jobPath;
        if(project.getParent() instanceof MultiBranchProject) {
            jobPath = Util.rawEncode(project.getParent().getFullName());
        } else {
            jobPath = Util.rawEncode(project.getFullName());
        }

        return getRoot() + "organizations/jenkins/" + jobPath + "/";
    }

    @Override
    public String getTestUrl(TestResult result) {
        return getRunURL(result.getRun()) + "/tests";
    }
}
