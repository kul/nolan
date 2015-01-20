(ns nolan.core-test
  (:require [clojure.test :refer :all]
            [nolan.core :refer :all]
            [overtone.at-at :refer [now]])
  (:import [org.joda.time Period DateTime]))

(defn- on-complete
  [sc scid timeout func]
  (let [start (now)]
    (if (> (- (now) start) timeout)
      (throw (Exception. "Completion timed out."))
      (if (expired? sc scid)
        (func)
        (do
          (Thread/sleep 10)
          (recur sc scid timeout func))))))

(deftest parsing-test
  (is (= (parse-schedule "R//") [Integer/MAX_VALUE :now :tail])
      "Repeat forever tailing.")
  (is (= (parse-schedule "R10//") [10 :now :tail])
      "Repeat 10 times tailing.")
  (is (= (parse-schedule "R10//PT10S") [10 :now (Period. 10000)])
      "Repeat 10 times tailing.")
  (let [sch "R/1991-11-11T16:12:00/PT1S"
        start (now)]
    (is (nil? (doseq [i (range 1000)]
                (assert (as-> (apply next-time (rest (parse-schedule sch))) $
                          (<= start $)))))
        "Next schedule should always be in the future."))
  (let [sch "R/1991-11-11T16:12:00/PT1S"]
    (is (as-> (apply next-time (rest (parse-schedule "R/1991-11-11T16:12:00/"))) $
          (- $ (now)) (<= $ *pool-delta*))
        "Tailing schedules sould be started almost immedately.")))

(deftest scheduling-test
  (let [sc (get-mem-scheduler)
        c (atom 0)]
    (is (= (on-complete sc (add-schedule sc "R10//" #(swap! c inc)) 1000
                        #(deref c))
           10)
        "Increment 10 times")
    (reset! c 0)
    (is (= (on-complete sc (add-schedule sc "R10//PT.01S" #(swap! c inc)) 1000
                        #(deref c))
           10)
        "Increment 10 times at 10 ms interval.")
    (reset! c 0)
    (is (on-complete sc (add-schedule sc 1 "R//" #(if (= @c 10) (expire sc 1) (swap! c inc))) 1000
                     #(expired? sc 1))
        "Expiring schedule should work.")
    (is (= @c 10) "Tailing executions should work from above.")
    (reset! c 0)
    (is (= (on-complete sc (add-schedule
                             sc (format "R10/%s/" (DateTime. (- (now) 100000)))
                             #(swap! c inc))
                        1000 #(deref c))
           10)
        "Schedule will run even for a past starting time.")))
