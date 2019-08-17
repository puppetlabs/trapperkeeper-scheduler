(ns puppetlabs.trapperkeeper.services.scheduler.job
  (:gen-class
   :name puppetlabs.trapperkeeper.services.scheduler.job
   :state state
   :init init
   :constructors {[] []}
   :implements [org.quartz.StatefulJob org.quartz.InterruptableJob]
   :prefix "-")
  (:require [clojure.tools.logging :as log]
            [puppetlabs.i18n.core :as i18n])
  (:import (org.quartz JobExecutionContext JobDataMap JobExecutionException DateBuilder DateBuilder$IntervalUnit)
           (java.util Date)))

(defn -init
  []
  [[] (atom {})])

(defn- recurring?
  [options]
  (or (contains? options :interval) (contains? options :interspaced)))

(defn- calculate-next-execution-time
  [context options]
  (if (contains? options :interspaced)
    (Date. ^Long (+ (System/currentTimeMillis) (:interspaced options)))
    (.getFireTimeAfter (.getTrigger context) (Date.))))

(defn- should-skip?
  [context options]
  (when (contains? options :interval)
    (let [interval-ms (:interval options)
          now-ms (System/currentTimeMillis)
          scheduled-fire-time-ms (.getTime (.getScheduledFireTime context))]
      ; if the scheduled execution time is an interval or more away, skip it.
      (> now-ms (+ scheduled-fire-time-ms interval-ms)))))

(defn -execute
  [this ^JobExecutionContext context]
  (try
    (let [^JobDataMap merged (.getMergedJobDataMap context)
          options (.get merged "jobData")
          f (:job options)]
      (swap! (.state this) into {:current-thread (Thread/currentThread)})

      (if-not (should-skip? context options)
        (f)
        (log/info (i18n/trs "Skipping execution of job {0} because of missed interval." (.toString (.getKey (.getJobDetail context))))))

      ; using quartz interval execution does not take into account the
      ; execution time of the actual job.  For interspaced jobs, this means
      ; triggering the job after this one completes, for interval jobs,
      ; this means fast-forwarding the execution time to the next logical
      ; one
      (when (recurring? options)
        (let [scheduler (.getScheduler context)
              oldTrigger (.getTrigger context)
              future-date (calculate-next-execution-time context options)
              trigger (-> (.getTriggerBuilder oldTrigger)
                          (.startAt future-date)
                          (.build))]
          (.rescheduleJob scheduler (.getKey oldTrigger) trigger))))


    (catch Throwable e
      (log/error e (i18n/trs "scheduled job threw error"))
      (let [new-exception (JobExecutionException. ^Throwable e)]
        (.setUnscheduleFiringTrigger new-exception true)
        (throw new-exception)))))

(defn -interrupt
  [this]
  (when-let [thread (:current-thread @(.state this))]
    (.interrupt thread)))

