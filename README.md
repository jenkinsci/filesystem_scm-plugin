[![][ButlerImage]][homepage] 

# About
This is the source repository for the **File System SCM** plugin for Jenkins.
This plugin provides an additional SCM option to Jenkins, based on the file
system.

For more information see the [homepage].

# Purpose and Use Cases

Use this plugin if you want to use the Jenkins SCM (source code management)
functionality, but instead of retrieving code from a version control system such
as CVS, Subversion or git, instead retrieve the code from a directory on the
file system.

A typical use case is: during development of a Jenkins job (e.g. a pipeline),
you don't need/want to connect to an actual version control system, but avoid
the latency by just getting things from the local file system. 

# How to Raise Issues

If you find any bug, or if you want to file a change request, then please
check out:
[How to report an issue](https://wiki.jenkins.io/display/JENKINS/How+to+report+an+issue).

When creating a ticket in the [Jenkins JIRA](https://issues.jenkins-ci.org/)
system, select the component `filesystem_scm_plugin`.

# Source
The source code can be found on
[GitHub](https://github.com/jenkinsci/filesystem_scm-plugin). Fork us!

# Contributing

Contributions are welcome! Check out the
[open tickets](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20%28Open%2C%20Reopened%29%20AND%20component%20%3D%20filesystem_scm-plugin)
for this plugin in JIRA.

If you are a newbie, and want to pick up an easier task first, you may
want to start with
[newbie-friendly open tickets](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20%28Open%2C%20Reopened%29%20AND%20component%20%3D%20filesystem_scm-plugin%20AND%20labels%20%3D%20newbie-friendly)
first (if there are any).

# License
This Jenkins plugin is licensed under the
[MIT License](https://github.com/jenkinsci/jenkins/raw/master/LICENSE.txt).
The terms of the license are as follows:

    The MIT License (MIT)

    Copyright (c) 2009-2018 Various contributors. 

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    
[ButlerImage]: https://jenkins.io/sites/default/files/jenkins_logo.png
[homepage]: https://plugins.jenkins.io/filesystem_scm
