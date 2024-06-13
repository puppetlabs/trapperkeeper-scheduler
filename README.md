# Trapperkeeper Scheduler Service

[![Build Status](https://travis-ci.org/puppetlabs/trapperkeeper-scheduler.svg)](https://travis-ci.org/puppetlabs/trapperkeeper-scheduler)

[![Clojars Project](http://clojars.org/puppetlabs/trapperkeeper-scheduler/latest-version.svg)](http://clojars.org/puppetlabs/trapperkeeper-scheduler)

A Trapperkeeper service that provides a simple API for scheduling background tasks.

Other Trapperkeeper services may specify a dependency on the Scheduler service,
and then use its functions to schedule and cancel jobs to be run on background
worker threads.

### What Does This Service Do?

The `SchedulerService` provides some simple API for scheduling (potentially
recurring) background tasks.  The service manages the lifecycle of the underlying
scheduling subsystem so that other services don't need to (and avoids potential issues
around multiple services attempting to initialize the same scheduling subystem
in a single JVM process).

The functions that are currently available are as follows:

* `interspaced [interval-ms f]`: schedules a job that will call `f`, block until
  that call completes, sleep for `interval-ms` milliseconds, and then repeat.
  Returns an identifier that can be used to reference this scheduled job (e.g.,
  for cancellation) later.
* `interspaced [interval-ms f group-id]`: schedules a job that will call `f`, block until
  that call completes, sleep for `interval-ms` milliseconds, and then repeat.
  Returns an identifier that can be used to reference this scheduled job (e.g.,
  for cancellation) later. A group identifier `group-id` can be provided that
  allows jobs in the same group to be stopped at the same time.
* `interval [interval-ms f]`: schedules a job that will call `f`, block until
  that call completes, and then run again at the next logical interval based on
  `interval-ms` and the original start time.  In other words, `f` will get called every
  `interval-ms` unless the execution time for `f` exceeds `interval-ms` in which case
  that execution is skipped.
  Returns an identifier that can be used to reference this scheduled job (e.g.,
  for cancellation) later.
* `interval [interval-ms f group-id]`: schedules a job that will call `f`, block until
  that call completes, and then run again at the next logical interval based on
  `interval-ms` and the original start time.  In other words, `f` will get called every
  `interval-ms` unless the execution time for `f` exceeds `interval-ms` in which case
  that execution is skipped. If there are insufficient threads in the thread pool to
  run the interval job at the time of execution, the job will be skipped. Returns an
  identifier that can be used to reference this scheduled job (e.g.,
  for cancellation) later. A group identifier `group-id` can be provided that
  allows jobs in the same group to be stopped at the same time.
* `cron [cron-string f]`: schedules a job that will call `f` in accordance with the
  cron schedule indicated by the 'cron-string'. Returns an identifier that can 
  be used to reference this scheduled job (e.g. for cancellation) later.  More information
  on a valid cron string can be found [here](https://www.quartz-scheduler.org/api/2.3.0/org/quartz/CronExpression.html).
* `cron [cron-string f group-id]`: schedules a job that will call `f` in accordance
  with the cron schedule indicated by the 'cron-string'. Returns an identifier that can 
  be used to reference this scheduled job (e.g. for cancellation) later. A group identifier
  `group-id` can be provided that allows jobs in the same group to be stopped at the same time.
  More information on a valid cron string can be found [here](https://www.quartz-scheduler.org/api/2.3.0/org/quartz/CronExpression.html).
* `after [interval-ms f]`: schedules a job that will call `f` a single time, after
  a delay of `interval-ms` milliseconds.  Returns an identifier that can be used
  to reference this scheduled job (e.g. for cancellation) later.
* `after [interval-ms f group-id]`: schedules a job that will call `f` a single time, after
  a delay of `interval-ms` milliseconds.  Returns an identifier that can be used
  to reference this scheduled job (e.g. for cancellation) later. A group identifier
  `group-id` can be provided that allows jobs in the same group to be stopped
  at the same time.
* `interval-after [initial-delay-ms interval-ms f]`: Similar to `interval` but delays
  initial execution until `initial-delay-ms` has occurred.
* `interval-after [initial-delay-ms interval-ms f group-id]`:Similar to `interval` but delays
  initial execution until `initial-delay-ms` has occurred A group identifier `group-id` can be provided that
  allows jobs in the same group to be stopped at the same time.
* `stop-job [job-id]`: Given a `job-id` returned by one of the previous functions,
  cancels the job.  If the job is currently executing it will be allowed to complete,
  but will not be invoked again afterward.  Returns `true` if the job was successfully
  stopped, `false` otherwise.
* `stop-jobs [group-id]`: Given a `group-id` identifier, cancel all the jobs
  associated with that `group-id`.  If any of the jobs are currently executing they
  will be allowed to complete, but will not be invoked again afterward.  Returns a
  sequence of maps, one for each job in the group, with each map containing the
  `job` and a boolean `stopped?` key indiciating if the job was stopped successfully
  or not.
* `count-jobs []`: Return a count of the total number of scheduled jobs known to
  to the scheduling service.  `after` jobs that have completed won't be included
  in the total.
* `count-jobs [group-id]`: Return a count of the total number of scheduled jobs
  with the associated `group-id` known to to the scheduling service.
  `after` jobs that have completed won't be included in the total.
* `get-jobs []`: return a list of the current job identifiers
* `get-jobs [group-id]`: return a list of the current job identifiers associated
  with the specified group identifier

### Implementation Details

A configuration value is available under scheduler->thread-count that controls
the number of threads used internally by the quartz library for job scheduling.
If not specified, it defaults to 10, which is the quartz internal default.

The current implementation of the `SchedulerService` is a wrapper around
the [`org.quartz-scheduler/quartz`](http://www.quartz-scheduler.org/) library.

### What's Next?

* Add additional scheduling API functions with more complicated recurring models.`.
* Add API for introspecting the state of currently scheduled jobs

#Support

Please log tickets and issues at our [Jira Tracker](https://tickets.puppetlabs.com/issues/?jql=project%20%3D%20Trapperkeeper).

