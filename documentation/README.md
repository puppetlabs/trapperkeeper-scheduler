## Trapperkeeper Scheduler Service Documentation

### What Does This Service Do?

The `SchedulerService` provides some simple API for scheduling (potentially
recurring) background tasks.  The service manages the lifecycle of the underlying
scheduling subsystem so that other services don't need to (and avoids potential issues
around multiple services attempting to initialize the same scheduling subystem
in a single JVM process).

This is a very early release, and provides only the most basic API.  The functions
that are currently available are as follows:

* `interspaced [interval-ms f]`: schedules a job that will call `f`, block until
  that call completes, sleep for `interval-ms` milliseconds, and then repeat.
  Returns an identifier that can be used to reference this scheduled job (e.g.,
  for cancellation) later.
* `after [interval-ms f]`: schedules a job that will call `f` a single time, after
  a delay of `interval-ms` milliseconds.  Returns an identifier that can be used
  to reference this scheduled job (e.g. for cancellation) later.
* `stop-job [job-id]`: Given a `job-id` returned by one of the previous functions,
  cancels the job.  If the job is currently executing it will be allowed to complete,
  but will not be invoked again afterward.  Returns `true` if the job was successfully
  stopped, `false` otherwise.

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