(ns puppetlabs.trapperkeeper.services.scheduler.scheduler-core
  (:require [clojure.tools.logging :as log]
            [puppetlabs.i18n.core :as i18n]
            [puppetlabs.kitchensink.core :as ks])
  (:import (org.quartz.impl.matchers GroupMatcher)
           (org.quartz.impl StdSchedulerFactory SchedulerRepository)
           (org.quartz JobBuilder SimpleScheduleBuilder TriggerBuilder DateBuilder
                       DateBuilder$IntervalUnit Scheduler JobKey SchedulerException JobDataMap)
           (org.quartz.utils Key)
           (java.util Date)))

(def shutdown-timeout-sec 30)

(defn create-scheduler
  "Creates and returns a thread pool which can be used for scheduling jobs."
  [thread-count]
  ;; without the following property set, quartz does a version check automatically
  (System/setProperty "org.quartz.scheduler.skipUpdateCheck" "true")
  (System/setProperty "org.quartz.threadPool.threadCount" (str thread-count))
  (let [scheduler (StdSchedulerFactory/getDefaultScheduler)]
    (.start scheduler)
    scheduler))

(defn build-executable-job
  ([f job-name group-name] (build-executable-job f job-name group-name {}))
  ([f job-name group-name options]
   (let [jdm (JobDataMap.)
         options (assoc options :job f)]
     (.put jdm "jobData" options)
     (-> (JobBuilder/newJob puppetlabs.trapperkeeper.services.scheduler.job)
         (.withIdentity job-name group-name)
         (.usingJobData jdm)
         (.build)))))

(defn interspaced
  [n f ^Scheduler scheduler group-name]
  (try
    (let [job-name (Key/createUniqueName group-name)
           job (build-executable-job f job-name group-name {:interspaced n})
           schedule (SimpleScheduleBuilder/simpleSchedule)
           trigger (-> (TriggerBuilder/newTrigger)
                       (.withSchedule schedule)
                       (.startNow)
                       (.build))]
      (.scheduleJob scheduler job trigger)
      (.getJobKey trigger))
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/error e (i18n/trs "Failed to schedule job")))))

(defn after
  [n f ^Scheduler scheduler group-name]
  (try
    (let [job-name (Key/createUniqueName group-name)
          job (build-executable-job f job-name group-name)
          future-date (Date. ^Long (+ (System/currentTimeMillis) n))
          trigger (-> (TriggerBuilder/newTrigger)
                      (.startAt future-date)
                      (.build))]
      (.scheduleJob scheduler job trigger)
      (.getJobKey trigger))
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/error e (i18n/trs "Failed to schedule job")))))

(defn interval
  [^Scheduler scheduler repeat-delay f group-name]
  (try
    (let [job-name (Key/createUniqueName group-name)
          job (build-executable-job f job-name group-name {:interval repeat-delay})
          schedule (-> (SimpleScheduleBuilder/simpleSchedule)
                       (.withIntervalInMilliseconds repeat-delay)
                       ; allow quartz to reschedule things outside "org.quartz.jobStore.misfireThreshold" using internal logic
                       ; this isn't sufficient for short interval jobs, so additional scheduling logic is included in the job itself
                       (.withMisfireHandlingInstructionNextWithRemainingCount)
                       (.repeatForever))

          trigger (-> (TriggerBuilder/newTrigger)
                      (.withSchedule schedule)
                      (.startNow)
                      (.build))]
      (.scheduleJob scheduler job trigger)
      (.getJobKey trigger))
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/error e (i18n/trs "Failed to schedule job")))))

(defn interval-after
  [^Scheduler scheduler initial-delay repeat-delay f group-name]
  (try
    (let [job-name (Key/createUniqueName group-name)
          job (build-executable-job f job-name group-name {:interval repeat-delay})
          schedule (-> (SimpleScheduleBuilder/simpleSchedule)
                       (.withIntervalInMilliseconds repeat-delay)
                       ; allow quartz to reschedule things outside "org.quartz.jobStore.misfireThreshold" using internal logic
                       ; this isn't sufficient for short interval jobs, so additional scheduling logic is included in the job itself
                       (.withMisfireHandlingInstructionNextWithRemainingCount)
                       (.repeatForever))
          future-date (Date. ^Long (+ (System/currentTimeMillis) initial-delay))
          trigger (-> (TriggerBuilder/newTrigger)
                      (.withSchedule schedule)
                      (.startAt future-date)
                      (.build))]
      (.scheduleJob scheduler job trigger)
      (.getJobKey trigger))
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/error e (i18n/trs "Failed to schedule job")))))

(defn stop-job
  "Returns true, if the job was deleted, and false if the job wasn't found."
  [^JobKey id ^Scheduler scheduler]
  (try
    (.deleteJob scheduler id)
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/debug e (i18n/trs "Failure stopping job"))
      false)))

(defn get-all-jobs
  [^Scheduler scheduler]
  (try
    (let [groups (seq (.getJobGroupNames scheduler))
          extract-keys (fn [group-name] (seq (.getJobKeys scheduler (GroupMatcher/jobGroupEquals group-name))))]
      (mapcat extract-keys groups))
    (catch SchedulerException e
      ; this can occur if the interface is being used while the scheduler is shutdown
      (log/debug e (i18n/trs "Failure getting all jobs"))
      [])))

(defn stop-all-jobs!
  [^Scheduler scheduler]
  (when-not (.isShutdown scheduler)
    (try
      (let [sr (SchedulerRepository/getInstance)
            scheduler-name (.getSchedulerName scheduler)]
        (doseq [job (get-all-jobs scheduler)]
          (try
            (.interrupt scheduler job)
            (.deleteJob scheduler job)
            (catch SchedulerException e
              ; this can occur if the interface is being used while the scheduler is shutdown
              (log/debug e (i18n/trs "Failure stopping job")))))

        (when (= :timeout (ks/with-timeout shutdown-timeout-sec :timeout (.shutdown scheduler true)))
          (log/info (i18n/trs "Failed to shutdown schedule service in {0} seconds" shutdown-timeout-sec))
          (.shutdown scheduler))
        ; explicitly remove the scheduler from the registry to prevent leaks.  This can happen if the
        ; jobs don't terminate immediately
        (.remove sr scheduler-name))
      (catch SchedulerException e
        ; this can occur if the interface is being used while the scheduler is shutdown
        (log/debug e (i18n/trs "Failure stopping all jobs"))))))

(defn get-jobs-in-group
  [^Scheduler scheduler group-id]
  (try
    (seq (.getJobKeys scheduler (GroupMatcher/jobGroupEquals group-id)))
    (catch SchedulerException e
      ; this can occur if the function is called when the scheduler is shutdown
      (log/debug e (i18n/trs "Failure getting jobs in group"))
      [])))
