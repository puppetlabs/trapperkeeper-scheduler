(ns puppetlabs.trapperkeeper.services.protocols.scheduler)

(defprotocol SchedulerService

  (interspaced
    [this n f]
    [this n f group-id]
    "Calls 'f' repeatedly with a delay of 'n' milliseconds between the
    completion of a given invocation and the beginning of the following
    invocation.  Returns an identifier for the scheduled job. An optional
    group-id can be provided to collect a set of jobs into one group to allow
    them to be stopped together.")

  (cron
   [this cron-string f]
   [this cron-string f group-id]
    "Calls 'f' in accordance with the cron schedule indicated by the 'cron-string'.
    Returns an identifier for the scheduled job. An optional
    group can be provided to associated jobs with each other to allow
    them to be stopped together.")

  (after
    [this n f]
    [this n f group-id]
    "Calls 'f' once after a delay of 'n' milliseconds.
    Returns an identifier for the scheduled job. An optional
    group can be provided to associated jobs with each other to allow
    them to be stopped together.")

  (interval
    [this n f]
    [this n f group-id]
   "Calls 'f' repeatedly with a delay of 'n' milliseconds between the
    beginning of a given invocation and the beginning of the following
    invocation. If an invocation executon time is longer than the interval,
    the subsquent invocation is skipped.
    Returns an identifier for the scheduled job. An optional
    group-id can be provided to collect a set of jobs into one group to allow
    them to be stopped together.")

  (interval-after
    [this initial-delay repeat-delay f]
    [this initial-delay repeat-delay f group-id]
    "Calls 'f' repeatedly with a delay of 'repeat-delay' milliseconds after the `initial-delay` in millseconds.
    Returns an identifier for the scheduled job. An optional
    group-id can be provided to collect a set of jobs into one group to allow
    them to be stopped together.")

  (cron-next-valid-time
   [this cron-string date] 
   "Given a cron specification and a date, returns a date that corresponds
    to the next execution of the timer based on that cron value")


  (stop-job [this job]
    "Given an identifier of a scheduled job, stop its execution.  If an
    invocation of the job is currently executing, it will be allowed to
    complete,  but the job will not be invocated again.
    Returns 'true' if the job was successfully stopped, 'false' otherwise.")

  (stop-jobs
    [this]
    [this group-id]
    "Stop all the jobs associated with the service.  Given an optional group-id
    stop only the jobs associated with that group id.
    Returns a sequence of maps, each with an identifier for the job and a boolean to
    indicate if the job was stopped successfully.")

  (count-jobs
    [this]
    [this group-id]
    "Return the number of jobs known to the scheduler service, or the number
    of jobs known to the scheduler service by group id. A nil group-id
    will return the count of all jobs.")

  (get-jobs
   [this]
   [this group-id]
   "Return all the known job identifiers, or the job identifiers associated
   with the given group."))


