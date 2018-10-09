(ns puppetlabs.trapperkeeper.services.scheduler.scheduler-service-test
  (:require [clojure.test :refer :all]
            [puppetlabs.kitchensink.core :as ks]
            [puppetlabs.trapperkeeper.testutils.bootstrap :refer :all]
            [puppetlabs.trapperkeeper.services.scheduler.scheduler-service :refer :all]
            [puppetlabs.trapperkeeper.services.protocols.scheduler :refer :all]
            [puppetlabs.trapperkeeper.services.scheduler.scheduler-core :as sc]
            [puppetlabs.trapperkeeper.app :as tk]))

(deftest ^:integration test-interspaced
  (testing "without group-id"
    (with-app-with-empty-config app [scheduler-service]
      (testing "interspaced"
        (let [service (tk/get-service app :SchedulerService)
              num-runs 10 ; let it run a few times, but not too many
              interval 300
              p (promise)
              counter (atom 0)
              delays (atom [])
              last-completion-time (atom nil)
              job (fn []
                    (when @last-completion-time
                      (let [delay (- (System/currentTimeMillis) @last-completion-time)]
                        (swap! delays conj delay)))
                    (swap! counter inc)

                    ; Make this job take a while so we can measure the duration
                    ; between invocations and ensure that the next invocation is
                    ; not scheduled until this one completes.
                    (Thread/sleep 100)

                    ; The test is over!
                    (when (= @counter num-runs)
                      (deliver p nil))

                    (reset! last-completion-time (System/currentTimeMillis)))]

          ; Schedule the job, and wait for it run num-runs times, then stop it.
          (let [job-id (interspaced service interval job)]
            (deref p)
            (stop-job service job-id))

          (testing (str "Each delay should be at least " interval "ms")
            (is (every? (fn [delay] (>= delay interval)) @delays)))

          (testing "can schedule far in the future"
            (interspaced service 21026149688 (constantly nil)))))))

  (testing "with group-id"
    (with-app-with-empty-config app [scheduler-service]
      (testing "interspaced"
        (let [group-id :some-group-identifier
              service (tk/get-service app :SchedulerService)
              num-runs 3 ; let it run a few times, but not too many
              interval 300
              p (promise)
              counter (atom 0)
              delays (atom [])
              last-completion-time (atom nil)
              job (fn []
                    (when @last-completion-time
                      (let [delay (- (System/currentTimeMillis) @last-completion-time)]
                        (swap! delays conj delay)))
                    (swap! counter inc)

                    ; Make this job take a while so we can measure the duration
                    ; between invocations and ensure that the next invocation is
                    ; not scheduled until this one completes.
                    (Thread/sleep 100)

                    ; The test is over!
                    (when (= @counter num-runs)
                      (deliver p nil))

                    (reset! last-completion-time (System/currentTimeMillis)))]

          ; Schedule the job, and wait for it run num-runs times, then stop it.
          (let [job-id (interspaced service interval job group-id)]
            (deref p)
            (stop-job service job-id))

          (testing (str "Each delay should be at least " interval "ms")
            (is (every? (fn [delay] (>= delay interval)) @delays))))))))

(deftest ^:integration test-after
  (testing "without group-id"
    (with-app-with-empty-config app [scheduler-service]
      (testing "after"
        (let [delay 100
              service (tk/get-service app :SchedulerService)]
          (testing "should execute at least " delay " milliseconds in the future"
            (let [completed (promise)
                  job #(deliver completed (System/currentTimeMillis))]
              (let [schedule-time (System/currentTimeMillis)]
                (after service delay job)
                (let [execution-time (deref completed)
                      actual-delay (- execution-time schedule-time)]
                  (is (>= actual-delay delay))))))
          (testing "can schedule far in the future"
            (after service 21026149688 (constantly nil)))))))

  (testing "with group-id"
    (with-app-with-empty-config app [scheduler-service]
      (testing "after"
        (let [delay 100]
          (testing "should execute at least " delay " milliseconds in the future"
            (let [completed (promise)
                  job #(deliver completed (System/currentTimeMillis))
                  service (tk/get-service app :SchedulerService)]
              (let [schedule-time (System/currentTimeMillis)]
                (after service delay job :some-group-identifier)
                (let [execution-time (deref completed)
                      actual-delay (- execution-time schedule-time)]
                  (is (>= actual-delay delay)))))))))))

(deftest ^:integration test-stop-job
  (testing "without group-id"
    (testing "stop-job lets a job complete but does not run it again"
      (with-app-with-empty-config app [scheduler-service]
        (let [service (tk/get-service app :SchedulerService)
              started (promise)
              stopped (promise)
              start-time (atom 0)
              completed (promise)
              job (fn []
                    (reset! start-time (System/currentTimeMillis))
                    (deliver started nil)
                    (deref stopped)
                    (deliver completed nil))
              interval 10
              job-id (interspaced service interval job)]
          ; wait for the job to start
          (deref started)
          (let [original-start-time @start-time]
            (testing "the job can be stopped"
              (is (stop-job service job-id)))
            (deliver stopped nil)
            (deref completed)
            ; wait a bit, ensure the job does not run again
            (testing "the job should not run again"
              (Thread/sleep 100)
              (is (= original-start-time @start-time)))
            (testing "there should be no other jobs running"
              (is (= 0 (count-jobs service)))))))))

  (testing "with group-id"
    (testing "stop-job lets a job complete but does not run it again"
      (with-app-with-empty-config app [scheduler-service]
        (let [service (tk/get-service app :SchedulerService)
              started (promise)
              stopped (promise)
              start-time (atom 0)
              completed (promise)
              job (fn []
                    (reset! start-time (System/currentTimeMillis))
                    (deliver started nil)
                    (deref stopped)
                    (deliver completed nil))
              interval 10
              job-id (interspaced service interval job :some-group-identifier)]
          ; wait for the job to start
          (deref started)
          (let [original-start-time @start-time]
            (testing "the job can be stopped"
              (is (stop-job service job-id)))
            (deliver stopped nil)
            (deref completed)
            ; wait a bit, ensure the job does not run again
            (testing "the job should not run again"
              (Thread/sleep 100)
              (is (= original-start-time @start-time)))
            (testing "there should be no other jobs running"
              (is (= 0 (count-jobs service))))))))))

(defn guaranteed-start-interval-job
  ([service interval]
   (let [started (promise)
         job (fn []
               (deliver started nil))
         result (interspaced service interval job)]
     (deref started)
     result))

  ([service interval group-id]
   (let [started (promise)
         job (fn []
               (deliver started nil))
         result (interspaced service interval job group-id)]
     (deref started)
     result)))

; This test has a few race conditions, but unlikely to occur in reality
(deftest ^:integration test-count-job
  (testing "count-jobs shows correct number of non-group-id jobs"
    (with-app-with-empty-config app [scheduler-service]
      (let [service (tk/get-service app :SchedulerService)
            interval 10
            job-0 (guaranteed-start-interval-job service interval)]
        (is (= 1 (count-jobs service)))
        (let [job-1 (guaranteed-start-interval-job service interval)]
          (is (= 2 (count-jobs service)))
          (let [job-2 (guaranteed-start-interval-job service interval)]
              (is (= 3 (count-jobs service)))
              (stop-job service job-0)
              (is (= 2 (count-jobs service)))
              (stop-job service job-1)
              (is (= 1 (count-jobs service)))
              (stop-job service job-2)
              (is (= 0 (count-jobs service))))))))

  (testing "count-jobs shows correct number of group-id and non-group-id jobs"
    (with-app-with-empty-config app [scheduler-service]
      (let [service (tk/get-service app :SchedulerService)
            interval 10
            job-0 (guaranteed-start-interval-job service interval)
            group-id :unique-group-id]
        (is (= 1 (count-jobs service)))
        (let [group-id-job-0 (guaranteed-start-interval-job service interval group-id)]
          (is (= 2 (count-jobs service)))
          (is (= 1 (count-jobs service group-id)))
          (let [job-1 (guaranteed-start-interval-job service interval)]
            (is (= 3 (count-jobs service)))
            (is (= 1 (count-jobs service group-id)))
            (let [group-id-job-1 (guaranteed-start-interval-job service interval group-id)]
              (is (= 4 (count-jobs service)))
              (is (= 2 (count-jobs service group-id)))
              (let [job-2 (guaranteed-start-interval-job service interval)]
                (is (= 5 (count-jobs service)))
                (is (= 2 (count-jobs service group-id)))
                (stop-job service job-0)
                (is (= 4 (count-jobs service)))
                (is (= 2 (count-jobs service group-id)))
                (stop-job service group-id-job-0)
                (is (= 3 (count-jobs service)))
                (is (= 1 (count-jobs service group-id)))
                (stop-job service job-1)
                (is (= 2 (count-jobs service)))
                (is (= 1 (count-jobs service group-id)))
                (stop-job service group-id-job-1)
                (is (= 1 (count-jobs service)))
                (is (= 0 (count-jobs service group-id)))
                (stop-job service job-2)
                (is (= 0 (count-jobs service)))
                (is (= 0 (count-jobs service group-id))))))))))

  (testing "after reduces count when complete"
    (with-app-with-empty-config app [scheduler-service]
      (let [service (tk/get-service app :SchedulerService)
            delay 100
            wait-for-start (promise)
            completed (promise)
            job (fn []
                  (deref wait-for-start)
                  (deliver completed (System/currentTimeMillis)))]
        (after service delay job)
        (is (= 1 (count-jobs service)))
        (deliver wait-for-start true)
        (deref completed)
        ; there is a small window between when the promise is delivered and the count changes
        (Thread/sleep 100)
        (is (= 0 (count-jobs service)))))))

(deftest ^:integration test-stop-grouped-jobs
  (testing "stop-jobs stops the jobs for a group"
    (with-app-with-empty-config app [scheduler-service]
      (let [service (tk/get-service app :SchedulerService)
            started (promise)
            job (fn []
                  (deliver started nil))
            interval 10
            group-id-0 :unique-group-id
            group-id-1 :more-unique-group-id
            ; create one job without a group-id and two with one group-id
            ; and a third with a different group-id
            job-3 (interspaced service interval (constantly true))
            job-2 (interspaced service interval (constantly true) group-id-0)
            job-1 (interspaced service interval (constantly true) group-id-0)
            job-0 (interspaced service interval job group-id-1)]

        (testing "all the jobs were started"
          (is (= 4 (count-jobs service)))
          (is (= 2 (count-jobs service group-id-0)))
          (is (= 1 (count-jobs service group-id-1))))

        ; wait for the jobs to start
        (deref started)
        (Thread/sleep 100)
        (testing "stopping one group-id does not stop them all"
          (stop-jobs service group-id-0)
          (is (= 2 (count-jobs service)))
          (is (= 0 (count-jobs service group-id-0)))
          (is (= 1 (count-jobs service group-id-1))))

        (testing "stopping one job does not stop the group id based job"
          (stop-job service job-3)
          (is (= 1 (count-jobs service)))
          (is (= 0 (count-jobs service group-id-0)))
          (is (= 1 (count-jobs service group-id-1))))

        (testing "stopping by group id stops the job"
          (stop-jobs service group-id-1)
          (is (= 0 (count-jobs service)))
          (is (= 0 (count-jobs service group-id-0)))
          (is (= 0 (count-jobs service group-id-1))))))))

(defn schedule-random-jobs
  "Schedules several random jobs and returns their JobKeys."
  [service]
  (set
   (for [x [1 2 3]]
     (interspaced service 1000 (constantly x)))))

(deftest ^:integration test-shutdown
  (testing "Any remaining jobs will be stopped when the service is stopped."
    (let [app (bootstrap-services-with-empty-config [scheduler-service])
          service (tk/get-service app :SchedulerService)
          job-ids (schedule-random-jobs service)]

      (testing "reports all of the jobs we just scheduled"
        (is (= (set job-ids) (set (get-jobs service)))))

      (testing "Stopping the service stops all of the scheduled jobs"
        (tk/stop app)
        (is (empty? (get-jobs service))))))

  (testing "Shutdown honors timeout and interrupts existing jobs"
    ; redefine the default timeout so we don't have to wait forever
    (with-redefs [sc/shutdown-timeout-sec 5]
      (let [app (bootstrap-services-with-empty-config [scheduler-service])
            service (tk/get-service app :SchedulerService)
            is-test-done (promise)
            job-done (promise)
            ; run a job that waits on a promise that isn't ever delivered, but is interrupted
            job (interspaced service 1000 (fn []
                                            (try
                                              (deref is-test-done)
                                              (catch InterruptedException _
                                                (deliver job-done true)))))]

        (testing "job was correctly scheduled"
          (is (= (set [job]) (set (get-jobs service)))))

        (testing "Stopping the service does not block forever"
          (is (not= :timeout (ks/with-timeout 10 :timeout (tk/stop app))))
          (is (empty? (get-jobs service))))
        (deref job-done)))))



