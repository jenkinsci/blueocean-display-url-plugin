/*
 * The MIT License
 *
 * Copyright 2015 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.blueoceandisplayurl;

import jenkins.plugins.git.GitSampleRepoRule;

import java.io.IOException;

/**
 * Manages a sample Git repository.
 */
public final class GitUtil {
    private final GitSampleRepoRule gitSampleRepoRule;
    public GitUtil(GitSampleRepoRule gitSampleRepoRule) {
        this.gitSampleRepoRule = gitSampleRepoRule;
    }

    public void git(String... cmds) throws Exception {
        gitSampleRepoRule.git(cmds);
    }

    public GitUtil checkoutNewBranch(String branchName) throws Exception {
        git("checkout", "-b", branchName);
        return this;
    }

    public GitUtil writeFile(String fileName, String fileContents) throws IOException {
        gitSampleRepoRule.write(fileName, fileContents);
        return this;
    }

    public GitUtil writeJenkinsFile(BlueOceanDisplayURLImplTest.JenkinsFile file) throws IOException {
        gitSampleRepoRule.write("Jenkinsfile", file.getFileContents());
        return this;
    }

    public GitUtil addFile(String fileName) throws Exception {
        git("add", fileName);
        return this;
    }

    public GitUtil commit(String msg) throws Exception {
        git("commit", "--all", "--message='"+msg+"'");
        return this;
    }

}
