package org.jenkinsci.plugins.blueoceandisplayurl;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.model.BlueOrganization;
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
        String root = jenkins.getRootUrl();
        if (root == null) {
            root = "http://unconfigured-jenkins-location/";
        }
        return root + "blue/";
    }

    @Override
    public String getRunURL(Run<?, ?> run) {
        BlueOrganization organization = OrganizationFactory.getInstance().getContainingOrg(run.getParent());
        if (organization == null || !isSupported(run)) {
            return DisplayURLProvider.getDefault().getRunURL(run);
        }

        if (run instanceof WorkflowRun) {
            WorkflowJob job = ((WorkflowRun) run).getParent();
            if (job.getParent() instanceof MultiBranchProject) {
                String jobURL = getJobURL(organization, ((MultiBranchProject) job.getParent()));
                return String.format("%sdetail/%s/%d/", jobURL, Util.rawEncode(job.getDisplayName()), run.getNumber());
            }
        }
        Job job = run.getParent();
        String jobURL = getJobURL(organization, job);
        return String.format("%sdetail/%s/%d/", jobURL, Util.rawEncode(job.getDisplayName()), run.getNumber());
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
        BlueOrganization organization = OrganizationFactory.getInstance().getContainingOrg(job);
        if (organization == null || !isSupported(job)) {
            return DisplayURLProvider.getDefault().getJobURL(job);
        }
        return getJobURL(organization, job);
    }

    private String getJobURL(BlueOrganization organization, Job<?, ?> job) {
        String jobPath = job.getParent() instanceof MultiBranchProject ? job.getParent().getFullName() : job.getFullName();
        return String.format("%sorganizations/%s/%s/", getRoot(), Util.rawEncode(organization.getName()), Util.rawEncode(jobPath));
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

    private String getJobURL(BlueOrganization organization, MultiBranchProject<?, ?> project) {
        return String.format("%sorganizations/%s/%s/", getRoot(), Util.rawEncode(organization.getName()), Util.rawEncode(project.getFullName()));
    }
}
