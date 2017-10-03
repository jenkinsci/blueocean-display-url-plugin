package org.jenkinsci.plugins.blueoceandisplayurl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.acegisecurity.AccessDeniedException;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.DELETE;
import org.springframework.util.CollectionUtils;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.User;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.ApiHead;
import io.jenkins.blueocean.rest.factory.organization.AbstractOrganization;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import io.jenkins.blueocean.rest.model.BluePipelineContainer;
import io.jenkins.blueocean.rest.model.BlueUser;
import io.jenkins.blueocean.rest.model.BlueUserContainer;
import io.jenkins.blueocean.service.embedded.OrganizationFactoryImpl;
import io.jenkins.blueocean.service.embedded.rest.OrganizationImpl;
import io.jenkins.blueocean.service.embedded.rest.PipelineContainerImpl;
import io.jenkins.blueocean.service.embedded.rest.UserContainerImpl;
import io.jenkins.blueocean.service.embedded.rest.UserImpl;
import jenkins.branch.BranchProperty;
import jenkins.branch.BranchSource;
import jenkins.branch.DefaultBranchPropertyStrategy;
import jenkins.model.Jenkins;
import jenkins.model.ModifiableTopLevelItemGroup;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.api.SCMSource;

/**
 * @author Ivan Meredith
 */
public class BlueOceanDisplayURLImplTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GitSampleRepoRule gitSampleRepoRule = new GitSampleRepoRule();

    DisplayURLProvider displayURL;

    private GitUtil repo;

    @Before
    public void before() throws Exception {
        gitSampleRepoRule.init();
        this.repo = new GitUtil(gitSampleRepoRule);

    }
    Pattern pathPAttern = Pattern.compile("http://.+:[0-9]+(/.*)");
    private String getPath(String url) throws URISyntaxException {
        Matcher m = pathPAttern.matcher(url);
        m.matches();
        return m.group(1);
    }

    @Test
    public void testProjectURL() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("abc");
        String url = getPath(displayURL.getJobURL(p));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/abc/", url);

    }
    @Test
    public void testProjectURL_CustomOrganization() throws Exception {
        FreeStyleProject p = orgFolder.createProject(FreeStyleProject.class, "abc");
        String url = getPath(displayURL.getJobURL(p));
        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/abc/", url);
    }

    @Test
    public void testProjectInFolder() throws Exception {
        MockFolder folder = j.createFolder("test");
        Project p = folder.createProject(FreeStyleProject.class, "abc");
        p.setDisplayName("custom name");
        String url = getPath(displayURL.getJobURL(p));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/test%2Fabc/", url);

        p.scheduleBuild2(0).waitForStart();

        url = getPath(displayURL.getRunURL(p.getLastBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/test%2Fabc/detail/abc/1/", url);

        url = getPath(displayURL.getChangesURL(p.getLastBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/test%2Fabc/detail/abc/1/changes", url);

    }

    @Test
    public void testProjectInFolder_CustomOrganization() throws Exception {
        Folder folder = orgFolder.createProject(Folder.class, "test");
        Project p = folder.createProject(FreeStyleProject.class, "abc");
        String url = getPath(displayURL.getJobURL(p));
        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/test%2Fabc/", url);

        p.scheduleBuild2(0).waitForStart();

        url = getPath(displayURL.getRunURL(p.getLastBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/test%2Fabc/detail/abc/1/", url);

        url = getPath(displayURL.getChangesURL(p.getLastBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/test%2Fabc/detail/abc/1/changes", url);

    }

    @Test
    public void testMultibranchUrls() throws Exception {
        repo.checkoutNewBranch("feature/test-1")
                .writeJenkinsFile(JenkinsFile.createFile().node().stage("stage1").echo("test").endNode())
                .addFile("Jenkinsfile")
                .commit("Initial commit to feature/test-1");

        MultiBranchTestBuilder mp = MultiBranchTestBuilder.createProjectInFolder(j, "folder", "test", gitSampleRepoRule);

        WorkflowJob job = mp.scheduleAndFindBranchProject("feature%2Ftest-1");

        String url = getPath(displayURL.getRunURL(job.getFirstBuild()));

        Assert.assertEquals("/jenkins/blue/organizations/jenkins/folder%2Ftest/detail/feature%2Ftest-1/1/", url);

        url = getPath(displayURL.getChangesURL(job.getFirstBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/folder%2Ftest/detail/feature%2Ftest-1/1/changes", url);
    }

    @Test
    public void testMultibranchUrlsWithDisplayNameBranches() throws Exception {
        repo.checkoutNewBranch("feature/test-1")
                .writeJenkinsFile(JenkinsFile.createFile().node().stage("stage1").echo("test").endNode())
                .addFile("Jenkinsfile")
                .commit("Initial commit to feature/test-1");

        MultiBranchTestBuilder mp = MultiBranchTestBuilder.createProjectInFolder(j, "folder", "test", gitSampleRepoRule);

        WorkflowJob job = mp.scheduleAndFindBranchProject("feature%2Ftest-1");
        job.setDisplayName("Custom Name");
        String url = getPath(displayURL.getRunURL(job.getFirstBuild()));

        Assert.assertEquals("/jenkins/blue/organizations/jenkins/folder%2Ftest/detail/feature%2Ftest-1/1/", url);

        url = getPath(displayURL.getChangesURL(job.getFirstBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/jenkins/folder%2Ftest/detail/feature%2Ftest-1/1/changes", url);
    }


    @Test
    public void testMultibranchUrls_CustomOrganization() throws Exception {
        repo.checkoutNewBranch("feature/test-1")
            .writeJenkinsFile(JenkinsFile.createFile().node().stage("stage1").echo("test").endNode())
            .addFile("Jenkinsfile")
            .commit("Initial commit to feature/test-1");

        MultiBranchTestBuilder mp = MultiBranchTestBuilder.createProjectInFolder(j, "folder", "test", gitSampleRepoRule, orgFolder);

        WorkflowJob job = mp.scheduleAndFindBranchProject("feature%2Ftest-1");

        String url = getPath(displayURL.getRunURL(job.getFirstBuild()));

        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/folder%2Ftest/detail/feature%2Ftest-1/1/", url);

        url = getPath(displayURL.getChangesURL(job.getFirstBuild()));
        Assert.assertEquals("/jenkins/blue/organizations/TestOrg/folder%2Ftest/detail/feature%2Ftest-1/1/changes", url);
    }

    MockFolder orgFolder;
    @Before
    public void setUp() throws IOException {
        displayURL = Iterables.find(DisplayURLProvider.all(), Predicates.instanceOf(BlueOceanDisplayURLImpl.class));
        orgFolder = j.createFolder("TestOrgFolderName");
        orgFolder.setDisplayName("TestOrgFolderName Display Name");
    }

    public static class MultiBranchTestBuilder{
        private JenkinsRule j;
        private WorkflowMultiBranchProject mp;
        public MultiBranchTestBuilder(JenkinsRule j, WorkflowMultiBranchProject mp) {
            this.mp = mp;
            this.j = j;
        }

        public static MultiBranchTestBuilder createProjectInFolder(JenkinsRule j, String folderName, String name, GitSampleRepoRule gitRepo) throws IOException {
            return createProjectInFolder(j, folderName, name, gitRepo, null);
        }

        public static MultiBranchTestBuilder createProjectInFolder(JenkinsRule j, String folderName, String name, GitSampleRepoRule gitRepo, MockFolder parent) throws IOException {
            Folder folder;
            if (parent == null) {
                folder = j.createProject(Folder.class, folderName);
            } else {
                folder = parent.createProject(Folder.class, folderName);
            }

            WorkflowMultiBranchProject mp = folder.createProject(WorkflowMultiBranchProject.class, name);
            mp.getSourcesList().add(new BranchSource(new GitSCMSource(null, gitRepo.toString(), "", "*", "", false),
                    new DefaultBranchPropertyStrategy(new BranchProperty[0])));

            for (SCMSource source : mp.getSCMSources()) {
                assertEquals(mp, source.getOwner());
            }

            return new MultiBranchTestBuilder(j, mp);
        }

        public static MultiBranchTestBuilder createProject(JenkinsRule j, String name, GitSampleRepoRule gitRepo) throws IOException {
            WorkflowMultiBranchProject mp = j.jenkins.createProject(WorkflowMultiBranchProject.class, name);
            mp.getSourcesList().add(new BranchSource(new GitSCMSource(null, gitRepo.toString(), "", "*", "", false),
                    new DefaultBranchPropertyStrategy(new BranchProperty[0])));

            for (SCMSource source : mp.getSCMSources()) {
                assertEquals(mp, source.getOwner());
            }

            return new MultiBranchTestBuilder(j, mp);
        }


        public WorkflowJob scheduleAndFindBranchProject(String name) throws Exception {
            mp.scheduleBuild2(0).getFuture().get();
            return findBranchProject(name);
        }

        public void schedule() throws Exception {
            mp.scheduleBuild2(0).getFuture().get();
        }

        public WorkflowJob findBranchProject(String name) throws Exception {
            WorkflowJob p = mp.getItem(name);
            if (p == null) {
                mp.getIndexing().writeWholeLogTo(System.out);
                fail(name + " project not found");
            }
            return p;
        }
    }


    public static class JenkinsFile {
        private String file = "";
        public static JenkinsFile createFile() {
            return new JenkinsFile();
        }

        public JenkinsFile node() {
            file += "node {\n";
            return this;
        }

        public JenkinsFile endNode() {
            file += "}\n";
            return this;
        }

        public JenkinsFile stage(String name) {
            file += "stage '"+name+"';\n";
            return this;
        }
        public JenkinsFile sleep(int seconds) {
            file += "sleep " + seconds + ";\n";
            return this;
        }

        public JenkinsFile echo(String msg) {
            file += "echo '"+msg+"'";
            return this;
        }

        public String getFileContents() {
            return file;
        }
    }

    @TestExtension(value = { "testProjectURL_CustomOrganization",
            "testProjectInFolder_CustomOrganization",
            "testMultibranchUrls_CustomOrganization" })
    public static class TestOrganizationFactoryImpl extends OrganizationFactoryImpl {
        private OrganizationImpl instance = new OrganizationImpl("TestOrg", Jenkins.getInstance().getItem("/TestOrgFolderName", Jenkins.getInstance(), MockFolder.class));

        @Override
        public OrganizationImpl get(String name) {
            if (instance != null) {
                if (instance.getName().equals(name)) {
                    System.out.println("" + name + " Intance returned " + instance);
                    return instance;
                }
            }
            System.out.println("" + name + " no instance found");
            return null;
        }

        @Override
        public Collection<BlueOrganization> list() {
            return Collections.singleton((BlueOrganization) instance);
        }

        @Override
        public OrganizationImpl of(ItemGroup group) {
            if (group == instance.getGroup()) {
                return instance;
            }
            return null;
        }
    }

}
