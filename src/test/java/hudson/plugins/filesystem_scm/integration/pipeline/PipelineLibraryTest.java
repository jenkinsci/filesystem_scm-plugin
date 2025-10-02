package hudson.plugins.filesystem_scm.integration.pipeline;

import hudson.plugins.filesystem_scm.FSSCM;
import hudson.plugins.filesystem_scm.FilterSettings;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Tests for Jenkins Pipeline integration.
 *
 * @author Oleg Nenashev
 */
@WithJenkins
class PipelineLibraryTest {

    private JenkinsRule j;

    @TempDir
    private File tmpDir;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Disabled("JenkinsRule just hangs on mvn clean verify, passes for test file run")
    void shouldSupportFSSCMsJenkinsfileSource() throws Exception {
        // Init repo
        File fsscmDir = newFolder(tmpDir, "fsscm");
        File jenkinsfile = new File(fsscmDir, "Jenkinsfile");
        FileUtils.write(jenkinsfile, "echo `Hello`", StandardCharsets.UTF_8);

        // Create job
        WorkflowJob job = new WorkflowJob(j.jenkins, "MyPipeline");
        job.setDefinition(new CpsScmFlowDefinition(new FSSCM(null, false, false,
                new FilterSettings(true, Collections.emptyList())), "Jenkinsfile"));

        j.buildAndAssertSuccess(job);
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }

}
