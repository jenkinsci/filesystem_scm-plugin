package hudson.plugins.filesystem_scm.integration.pipeline;

import hudson.plugins.filesystem_scm.FSSCM;
import hudson.plugins.filesystem_scm.FilterSelector;
import hudson.plugins.filesystem_scm.FilterSettings;
import java.io.File;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for Jenkins Pipeline integration.
 * 
 * @author Oleg Nenashev
 */
public class PipelineLibraryTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
   // @Rule
    //public TemporaryFolder tmpDir = new TemporaryFolder();
    
    @Test
    public void shouldSupportFSSCMsJenkinsfileSource() throws Exception {
        
        // Init repo
     //   File fsscmDir = tmpDir.newFolder("fsscm");
    //    File jenkinsfile = new File(fsscmDir, "Jenkinsfile");
    //    FileUtils.write(jenkinsfile, "echo `Hello, world!`");
        
        // Create job
        WorkflowJob job = new WorkflowJob(j.jenkins, "MyPipeline");
        job.setDefinition(new CpsScmFlowDefinition(new FSSCM(null, false, false, 
                new FilterSettings(true, Collections.<FilterSelector>emptyList())), "Jenkinsfile"));
        
        j.buildAndAssertSuccess(job);
    }
    
}
