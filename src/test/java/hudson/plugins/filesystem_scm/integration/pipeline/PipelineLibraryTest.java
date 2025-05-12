package hudson.plugins.filesystem_scm.integration.pipeline;

import hudson.plugins.filesystem_scm.FSSCM;
import hudson.plugins.filesystem_scm.FilterSelector;
import hudson.plugins.filesystem_scm.FilterSettings;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for Jenkins Pipeline integration.
 * 
 * @author Oleg Nenashev
 */
@WithJenkins
public class PipelineLibraryTest {

    @TempDir
    public File tmpDir;
    
    //TODO: JenkinsRule just hangs on mvn clean verify, passes for test file run
    @Test
    @Disabled
    public void shouldSupportFSSCMsJenkinsfileSource(JenkinsRule j) throws Exception {

        // Init repo
        File fsscmDir = newFolder(tmpDir, "fsscm");
        File jenkinsfile = new File(fsscmDir, "Jenkinsfile");
        FileUtils.write(jenkinsfile, "echo `Hello`");

        // Create job
        WorkflowJob job = new WorkflowJob(j.jenkins, "MyPipeline");
        job.setDefinition(new CpsScmFlowDefinition(new FSSCM(null, false, false,
                new FilterSettings(true, Collections.<FilterSelector>emptyList())), "Jenkinsfile"));

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
