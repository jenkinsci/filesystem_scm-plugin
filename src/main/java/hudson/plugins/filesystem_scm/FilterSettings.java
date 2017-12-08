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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores filter settings.
 * @since TODO
 * @author Oleg Nenashev
 */
public class FilterSettings implements Describable<FilterSettings> {

    private final boolean includeFilter;
    @Nonnull
    private final List<FilterSelector> selectors;

    @DataBoundConstructor
    public FilterSettings(boolean includeFilter, List<FilterSelector> selectors) {
        this.includeFilter = includeFilter;
        this.selectors = selectors != null ? selectors : Collections.<FilterSelector>emptyList();
    }

    @Nonnull
    public List<FilterSelector> getSelectors() {
        return Collections.unmodifiableList(selectors);
    }
    
    @Nonnull
    public List<String> getWildcards() {
        if (selectors.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> res = new ArrayList<>(selectors.size());
        for (FilterSelector s : selectors) {
            res.add(s.getWildcard());
        }
        return res;
    }

    public boolean isIncludeFilter() {
        return includeFilter;
    }
    
    @Override
    public Descriptor<FilterSettings> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(DescriptorImpl.class);
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<FilterSettings> {
        
    }
}
