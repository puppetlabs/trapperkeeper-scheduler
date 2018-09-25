(ns puppetlabs.trapperkeeper.services.scheduler.job
  (:gen-class
   :name puppetlabs.trapperkeeper.services.scheduler.job
   :state "state"
   :init "init"
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

(defn -execute
  [this ^JobExecutionContext context]
  (try
    (let [^JobDataMap merged (.getMergedJobDataMap context)
          options (.get merged "jobData")
          f (:job options)]
      (swap! (.state this) into {:current-thread (Thread/currentThread)})
      (f)
      ; using quartz interval execution does not take into account the
      ; execution time of the actual job.  If that is important, we
      ; manually trigger the job after the interval.
      (when (contains? options :interval)
        (let [interval (:interval options)
              scheduler (.getScheduler context)
              oldTrigger (.getTrigger context)
              future-date (Date. ^Long (+ (System/currentTimeMillis) interval))
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

