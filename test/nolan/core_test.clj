(ns nolan.core-test
  (:require [clojure.test :refer :all]
            [nolan.core :refer :all]
            [overtone.at-at :refer [now]]))

(defn- on-complete
  [sc scid timeout func]
  (let [start (now)]
    (if (> (- (now) start) timeout)
      (throw (Exception. "Completion timed out."))
      (if-let [completed? (expired? sc scid)]
        (func)
        (do
          (Thread/sleep 10)
          (recur sc scid timeout func))))))

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
    (is (= @c 10) "Tailing executions should work from above.")))
