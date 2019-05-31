package org.jenkinsci.plugins.blueoceandisplayurl;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.displayurlapi.DisplayURLContext;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class EnvironmentContributorImpl extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        DisplayURLContext ctx = DisplayURLContext.open();
        try {
            ctx.run(r);
            ctx.plugin(null); // environment contributor "comes from" core
            BlueOceanDisplayURLImpl urlProvider = new BlueOceanDisplayURLImpl();
            envs.put("RUN_TESTS_DISPLAY_URL", urlProvider.getTestsURL(r));
            envs.put("RUN_ARTIFACTS_DISPLAY_URL", urlProvider.getArtifactsURL(r));
        } finally {
            ctx.close();
        }
    }
}
