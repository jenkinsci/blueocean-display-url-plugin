package org.jenkinsci.plugins.blueoceandisplayurl;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.blueocean.rest.factory.organization.AbstractOrganization;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import jenkins.branch.MultiBranchProject;
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
    @NonNull
    public String getDisplayName() {
        return "Blue Ocean";
    }

    @Override
    @NonNull
    public String getName() {
        return "blueocean";
    }

    @Override
    @NonNull
    public String getRoot() {
        return super.getRoot() + "blue/";
    }

    @Override
    @NonNull
    public String getRunURL(Run<?, ?> run) {
        BlueOrganization organization = OrganizationFactory.getInstance().getContainingOrg(run.getParent());
        if (organization == null || !isSupported(run)) {
            return DisplayURLProvider.getDefault().getRunURL(run);
        }

        if (run instanceof WorkflowRun) {
            WorkflowJob job = ((WorkflowRun) run).getParent();
            if (job.getParent() instanceof MultiBranchProject) {
                String jobURL = getJobURL(organization, ((MultiBranchProject) job.getParent()));
                return String.format("%sdetail/%s/%d/", jobURL, job.getName(), run.getNumber());
            }
        }
        Job job = run.getParent();
        String jobURL = getJobURL(organization, job);
        return String.format("%sdetail/%s/%d/", jobURL, Util.rawEncode(job.getName()), run.getNumber());
    }

    @Override
    @NonNull
    public String getArtifactsURL(Run<?, ?> run) {
        if (isSupported(run)) {
            return getRunURL(run) + "artifacts";
        } else {
            return DisplayURLProvider.getDefault().getArtifactsURL(run);
        }
    }

    @Override
    @NonNull
    public String getChangesURL(Run<?, ?> run) {
        if (isSupported(run)) {
            return getRunURL(run) + "changes";
        } else {
            return DisplayURLProvider.getDefault().getChangesURL(run);
        }
    }

    @Override
    @NonNull
    public String getTestsURL(Run<?, ?> run) {
        if (isSupported(run)) {
            return getRunURL(run) + "tests";
        } else {
            return DisplayURLProvider.getDefault().getTestsURL(run);
        }
    }

    @Override
    @NonNull
    public String getJobURL(Job<?, ?> job) {
        BlueOrganization organization = OrganizationFactory.getInstance().getContainingOrg(job);
        if (organization == null || !isSupported(job)) {
            return DisplayURLProvider.getDefault().getJobURL(job);
        }
        return getJobURL(organization, job);
    }

    @NonNull
    private String getJobURL(BlueOrganization organization, Job<?, ?> job) {
        String jobPath = job.getParent() instanceof MultiBranchProject ? getFullNameForItemGroup(organization, job.getParent()) : getFullNameForItem(organization, job);
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

    @NonNull
    private String getJobURL(BlueOrganization organization, MultiBranchProject<?, ?> project) {
        return String.format("%sorganizations/%s/%s/", getRoot(), Util.rawEncode(organization.getName()), Util.rawEncode(getFullNameForItem(organization, project)));
    }

    /**
     * Returns full name relative to the <code>BlueOrganization</code> base. Each name is separated by '/'
     *
     * @param org the organization the item belongs to
     * @param item to return the full name of
     * @return full name relative to <code>BlueOrganization</code> base
     */
    private static String getFullNameForItem(@CheckForNull BlueOrganization org, @NonNull Item item) {
        ItemGroup<?> group = getBaseGroup(org);
        return Functions.getRelativeNameFrom(item, group);
    }

    /**
     * Returns full name relative to the <code>BlueOrganization</code> base. Each name is separated by '/'
     *
     * @param org the organization the item belongs to
     * @param itemGroup to return the full name of
     * @return full name relative to <code>BlueOrganization</code> base
     */
    private static String getFullNameForItemGroup(@CheckForNull BlueOrganization org, @NonNull ItemGroup itemGroup) {
        if (itemGroup instanceof Item) {
            return getFullNameForItem(org, (Item)itemGroup);
        } else {
            return itemGroup.getFullName();
        }
    }

    /**
     * Tries to obtain the base group for a <code>BlueOrganization</code>
     *
     * @param org to get the base group of
     * @return the base group
     */
    private static ItemGroup<?> getBaseGroup(BlueOrganization org) {
        ItemGroup<?> group = null;
        if (org instanceof AbstractOrganization) {
            group = ((AbstractOrganization) org).getGroup();
        }
        return group;
    }
}
