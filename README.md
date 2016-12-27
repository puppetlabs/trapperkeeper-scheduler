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
* `after [interval-ms f]`: schedules a job that will call `f` a single time, after
  a delay of `interval-ms` milliseconds.  Returns an identifier that can be used
  to reference this scheduled job (e.g. for cancellation) later.
* `after [interval-ms f group-id]`: schedules a job that will call `f` a single time, after
  a delay of `interval-ms` milliseconds.  Returns an identifier that can be used
  to reference this scheduled job (e.g. for cancellation) later. A group identifier
  `group-id` can be provided that allows jobs in the same group to be stopped
  at the same time.
* `stop-job [job-id]`: Given a `job-id` returned by one of the previous functions,
  cancels the job.  If the job is currently executing it will be allowed to complete,
  but will not be invoked again afterward.  Returns `true` if the job was successfully
  stopped, `false` otherwise.
* `stop-grouped-jobs [group-id]`: Given a `group-id` identifier, cancel all the jobs
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

### Implementation Details

The current implementation of the `SchedulerService` is a fairly thin wrapper around
the [`overtone/at-at`](https://github.com/overtone/at-at) library.  This approach
was chosen for a couple of reasons:

* A few existing PL projects already use this library, so we thought it'd be better
  not to introduce a different scheduling subsystem until all of the PL TK projects
  are aligned to use the SchedulerService.
* The `at-at` API seems like a pretty reasonable Clojure API for scheduling tasks.

It would probably be a good idea to switch the implementation out to use a different
backend in the future; without having done too much investigation yet, I'd be
interested in looking into Quartz/Quartzite, simply because Quartz is very widely-used
and battle-tested in the Java world.  `at-at` does not appear to be being maintained
any longer.  We've had a few minor issues with it (especially w/rt shutting down
the system), and haven't gotten any responses from the maintainer on the github
issues we've opened.  Also, the source repo for `at-at` contains no tests :(

### What's Next?

* Add additional scheduling API functions, e.g. `every`.
* Add API for introspecting the state of currently scheduled jobs
* Consider porting the backend to something more sophisticated (and maintained)
  than `at-at`; if we do this, the intent would be to maintain the existing API.

#Support

Please log tickets and issues at our [Jira Tracker](https://tickets.puppetlabs.com/issues/?jql=project%20%3D%20Trapperkeeper).

