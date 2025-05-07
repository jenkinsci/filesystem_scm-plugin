## Changelog

### Version 2.1 (Jan 31, 2018)

-   [![(error)](docs/images/error.svg) JENKINS-49053](https://issues.jenkins-ci.org/browse/JENKINS-49053) -
    Prevent NullPointerException when writing empty changelog files to
    the disk

### Version 2.0 (Dec 08, 2017)

-   ![(plus)](docs/images/add.svg) [JENKINS-40743](https://issues.jenkins-ci.org/browse/JENKINS-40743) -
    Make the plugin compatible with Jenkins Pipeline and other Job types

-   ![(error)](docs/images/error.svg) [JENKINS-43993](https://issues.jenkins-ci.org/browse/JENKINS-43993) -
    Update the SCM implementation to be compatible with Stapler
    Databinding API
-   ![(error)](docs/images/error.svg) Cleanup
    issues reported by FindBugs and other static analysis checks
-   ![(info)](docs/images/information.svg) Update
    Jenkins core requirement to 1.642.3

### Archive

Version 1.20 (Dec 5th, 2011)

-   Support ANT style wildcard in file filtering
-   Add an extra config parameter to let users to choose whether hidden
    files/dirs should be copied or not

Version 1.10 (Apr 2, 2011)

-   No really user visible changes:
-   fixed isEmptySet() method on ChangeLog
-   updated to current version of Jenkins API

Version 1.9 (Sep 21, 2010)

-   Works on Hudson core 1.337 as well

Version 1.8 (Mar 29, 2010)

-   Bug fixed: enable clearWorkspace on the 1st jobrun will throw
    Exception

Version 1.7 (Mar 11, 2010)

-   Avoid Hudson startup error when upgrading to Hudson 1.349 or newer
    ([JENKINS-5893](https://issues.jenkins-ci.org/browse/JENKINS-5893))

Version 1.6 (Feb 12, 2010)

-   Bug fixed: chmod before copying readonly files on Unix
-   Bug fixed: Master/Slave bug
-   Bug fixed: help page URL correctly handled even Winstone started
    with prefix

Version 1.5

-   Preserve file permission (rwxrwxrwx) when copying files (on Unix
    platform only)
-   will only delete a file from workspace if it is copied by this
    plugin
-   ChangelogSet changed to follow the latest API
