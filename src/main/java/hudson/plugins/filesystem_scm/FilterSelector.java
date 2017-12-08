/*
 * The MIT License
 *
 * Copyright (c) 2017 Oleg Nenashev
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
package hudson.plugins.filesystem_scm;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Manages particular wildcards.
 * @since TODO
 * @author Oleg Nenashev
 */
public class FilterSelector implements Describable<FilterSelector> {
    
    private final String wildcard;

    @DataBoundConstructor
    public FilterSelector(String wildcard) {
        this.wildcard = wildcard;
    }

    public String getWildcard() {
        return wildcard;
    }

    @Override
    public Descriptor<FilterSelector> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(DescriptorImpl.class);
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<FilterSelector> {
        
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckWildcard(@QueryParameter String value) {
            if (null == value || value.trim().length() == 0) {
                return FormValidation.ok();
            }
            if (value.startsWith("/") || value.startsWith("\\") || value.matches("[a-zA-Z]:.*")) {
                return FormValidation.error("Pattern can't be an absolute path");
            } else {
                try {
                    SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter(value);
                    return FormValidation.ok("Pattern is correct: " + filter.getPattern());
                } catch (Exception e) {
                    return FormValidation.error(e, "Invalid wildcard pattern");
                }
            }
        }
    }
    
}
