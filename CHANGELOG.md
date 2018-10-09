## 1.0.0
 * switch from at/at to the Quartz schedule library
 * update clj-parent and drop support for java 7
 * reimplement the group-id mapping using quartz internals
 * add interface for listing the exiting job identifiers

## 0.1.0
 * Add the concept of group-id to the job creation endpoints to allow
 jobs to be grouped together for listing and cancellation.
 * Fix a potential memory leak with jobs created using `after`
 * Add an interface to return the total number of jobs and the number
 of jobs in a group-id

## 0.0.1
 * Initial release
