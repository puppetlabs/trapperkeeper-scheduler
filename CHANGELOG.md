## 1.2.0
 * updates clj-parent and drops support for java 8

## 1.1.3
 * ensure a non-nil function is passed to scheduling routines
 
## 1.1.2
 * add testing for java 11, disambiguate StdScheduleFactory constructor

## 1.1.1
 * use a unique scheduler rather than the default scheduler

## 1.1.0
 * add interface for `interval` and `interval-after` to the
 protocol and implementation to allow regular cadance jobs.
 * add support for thread-count configuration value that defaults to 10

## 1.0.1
 * exclude c3p0 from dependencies, it isn't used.

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
