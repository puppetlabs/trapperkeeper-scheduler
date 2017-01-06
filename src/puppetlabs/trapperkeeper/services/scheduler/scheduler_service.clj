(ns puppetlabs.trapperkeeper.services.scheduler.scheduler-service
  (:require [puppetlabs.trapperkeeper.services :as tk]
            [puppetlabs.trapperkeeper.services.protocols.scheduler :refer :all]
            [puppetlabs.trapperkeeper.services.scheduler.scheduler-core :as core]
            [clojure.tools.logging :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Internal "helper" functions

(defn get-pool
  [this]
  (-> this
      (tk/service-context)
      :pool))

(defn get-jobs
  [this]
  (-> this
      (tk/service-context)
      :jobs))

(defn- enqueue-job!
  ([this id]
   (enqueue-job! this id {}))
  ([this id opts]
   (let [result (assoc opts :job id)]
    (swap! (get-jobs this) conj result)
    result)))

(defn- dequeue-job!
  ([this job]
    (swap! (get-jobs this) disj job))
  ([this id keyword]
    (when-let [item (first (filter #(= id (get % keyword)) @(get-jobs this)))]
      (swap! (get-jobs this) disj item))))

(defn- after-job
  "Jobs run with `after` only execute once, and when done need to be reomved from
  the scheduled jobs set.  This wraps the job's function so that the job is removed
  correctly from the set when it completes (or fails)."
  [this after-id f]
  (fn []
    (try
      (f)
      (finally
        (dequeue-job! this after-id :after-id)))))

(defn- jobs-by-group-id
  [this group-id]
  (if group-id
    (filter #(= group-id (:group-id %)) @(get-jobs this))
    @(get-jobs this)))

(defn- create-maybe-stop-job-fn
  "given a stop-job function, return function that when given a job returns
  a map with the job and a boolean to indicate if the job was stopped"
  [stop-fn]
  (fn [job]
    {:job job
    :stopped? (stop-fn job)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Trapperkeeper service definition
(tk/defservice scheduler-service
  SchedulerService
  []

  (init [this context]
    (log/debug "Initializing Scheduler Service")
    (let [pool (core/create-pool)]
      (assoc context :pool pool
                     :jobs (atom #{})
                     :after-id (atom 0))))

  (stop [this context]
    (log/debug "Shutting down Scheduler Service")
    ; Stop any jobs that are still running
    (core/stop-all-jobs! @(:jobs context) (get-pool this))
    (log/debug "Scheduler Service shutdown complete.")
    context)

  (interspaced [this n f]
    (interspaced this n f nil))

  (interspaced [this n f group-id]
               (let [id (core/interspaced n f (get-pool this))]
                 (enqueue-job! this id {:group-id group-id})))

  (after [this n f]
   (after this n f nil))

  (after [this n f group-id]
     ; use after-id to identify the job for the cleanup "after-job" wrapper
    (let [after-id (swap! (:after-id (tk/service-context this)) inc)
          ; wrap the job function in a function that will remove the job from the job set when it is done
          wrapped-fn (after-job this after-id f)
          id (core/after n wrapped-fn (get-pool this))]
      (enqueue-job! this id {:after-id after-id
                           :group-id   group-id})))

  (stop-job [this job]
    (let [result (core/stop-job (:job job) (get-pool this))]
       (dequeue-job! this job)
       result))

  (stop-jobs
    [this]
    (stop-jobs this nil))

  (stop-jobs [this group-id]
    (let [jobs-by-group (jobs-by-group-id this group-id)]
       (reduce conj []
         (map
           (create-maybe-stop-job-fn (partial stop-job this))
            jobs-by-group))))

  (count-jobs [this]
    (count-jobs this nil))

  (count-jobs [this group-id]
    (count (jobs-by-group-id this group-id))))
