(ns puppetlabs.trapperkeeper.services.scheduler.scheduler-service
  (:require [puppetlabs.trapperkeeper.services :as tk]
            [puppetlabs.trapperkeeper.services.protocols.scheduler :refer :all]
            [puppetlabs.trapperkeeper.services.scheduler.scheduler-core :as core]
            [clojure.tools.logging :as log]
            [puppetlabs.i18n.core :as i18n])
  (:import (org.quartz SchedulerException)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Internal "helper" functions

(defn get-scheduler
  [this]
  (-> this
      (tk/service-context)
      :scheduler))

(defn- create-maybe-stop-job-fn
  "given a stop-job function, return function that when given a job returns
  a map with the job and a boolean to indicate if the job was stopped"
  [stop-fn]
  (fn [job]
    {:job job
     :stopped? (stop-fn job)}))

(def default-group-name "SCHEDULER_DEFAULT")

(defn safe-group-id
  [group-id]
  (if (and (not (keyword? group-id)) (empty? group-id))
    default-group-name
    (str group-id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Trapperkeeper service definition
(tk/defservice scheduler-service
  SchedulerService
  [[:ConfigService get-in-config]]

  (init [this context]
    (log/info (i18n/trs "Initializing Scheduler Service"))
     ;; the default in Quartz is 10 threads, so make that the default if it isn't specified
    (let [scheduler (core/create-scheduler (get-in-config [:scheduler :thread-count] 10))]
      (assoc context :scheduler scheduler)))

  (stop [this context]
    (log/info (i18n/trs "Shutting down Scheduler Service"))
    (core/stop-all-jobs! (get-scheduler this))
    (log/info "Scheduler Service shutdown complete.")
    context)

  (interspaced [this n f]
    (interspaced this n f default-group-name))

  (interspaced [this n f group-id]
    (core/interspaced n f (get-scheduler this) (safe-group-id group-id)))
  
  (cron [this cron-string f]
        (cron this cron-string f default-group-name))
  
  (cron [this cron-string f group-id]
        (core/cron cron-string f (get-scheduler this) (safe-group-id group-id)))

  (after [this n f]
   (after this n f default-group-name))

  (after [this n f group-id]
     (core/after n f (get-scheduler this) (safe-group-id group-id)))

  (interval [this n f]
    (interval this n f default-group-name))

  (interval [this n f group-id]
    (core/interval (get-scheduler this) n f (safe-group-id group-id)))


  (interval-after [this initial-delay repeat-delay f]
    (interval-after this initial-delay repeat-delay f default-group-name))

  (interval-after [this initial-delay repeat-delay f group-id]
    (core/interval-after (get-scheduler this) initial-delay repeat-delay f (safe-group-id group-id)))

  (stop-job [this job]
    (core/stop-job job (get-scheduler this)))

  (stop-jobs
    [this]
    (stop-jobs this default-group-name))

  (stop-jobs [this group-id]
    (let [jobs-by-group (core/get-jobs-in-group (get-scheduler this) (safe-group-id group-id))]
       (reduce conj []
         (map
           (create-maybe-stop-job-fn (partial stop-job this))
           jobs-by-group))))

  (get-jobs
   [this]
   (core/get-all-jobs (get-scheduler this)))

  (get-jobs
    [this group-id]
    (core/get-jobs-in-group (get-scheduler this) (safe-group-id group-id)))

  (count-jobs [this]
    (let [jobs (core/get-all-jobs (get-scheduler this))]
      (count jobs)))

  (count-jobs [this group-id]
    (count (core/get-jobs-in-group (get-scheduler this) (safe-group-id group-id)))))
