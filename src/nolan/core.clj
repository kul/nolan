(ns nolan.core
  (:require [nolan.store]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [clojure.string :as s :refer [blank?]])
  (:import [java.util UUID]
           [nolan.store MemScheduleStore]
           [org.joda.time DateTime]
           [org.joda.time.format ISOPeriodFormat]))

; Generic Scheduler Stuff {{{1
(def ^:private spool
  "Global pool for scheduling."
  (at/mk-pool))

(def ^:private ri-regex
  "Repeating Time Interval regex."
  #"^R(\d*)/(.*)?/(P.*)?$")

(defn parse-schedule [s]
  "Parse a limited subset of ISO8601 repeating time interval to be used for
  scheduling jobs."
  (let [match (re-find ri-regex s)
        _ (when (not match) (throw (Exception. "Not a valid schedule string.")))
        [_ rept start period] match
        rept (if (blank? rept) Integer/MAX_VALUE (Integer/parseInt rept))
        start (if (blank? start) :now (DateTime/parse start))
        period (if (blank? period) :tail (.. ISOPeriodFormat standard
                                             (parsePeriod period)))]
    (when (and (integer? rept) (<= rept 0))
      (throw (Exception. (format "Invalid Repeat Parameter %s." rept))))
    [rept start period]))

(defn- recuring-schedule
  [scheduler counter period entity id]
  (try
    (if (not (zero? @counter))
      (when (not (.expired? scheduler id))
        (swap! counter dec)
        (when (not= period :tail)
          (at/at (.getMillis (.plus (DateTime.) period))
                 #(recuring-schedule scheduler counter period entity id) spool))
        (.execute scheduler entity)
        (when (= period :tail)
          (at/at (+ 10 (at/now))
                 #(recuring-schedule scheduler counter period entity id) spool)))
      ; Scheduling completed
      (.expire scheduler id))
    ; Logging is must as it is not possible to throw exceptions back to user api
    (catch Exception e (log/error e))))

(defn- start-scheduling
  [scheduler iso-str entity id]
  (let [[rept start period] (parse-schedule iso-str)
        counter (atom rept)]
    (at/at (if (= start :now) (+ 10 (at/now)) (.getMillis start))
           #(recuring-schedule scheduler counter period entity id) spool)))

;; User API {{{1
(defn get-mem-scheduler
  "Get an in-memory scheduler."
  ([] (MemScheduleStore. (atom nil)))
  ([init] (MemScheduleStore. (atom init))))

(defn boot
  "Boots a scheduler, scheduling all the active jobs that exists in it."
  [scheduler]
  (doseq [{:keys [iso-str entity id]} (.get-active-schedules scheduler)]
    (start-scheduling scheduler iso-str entity id)))

(defn add-schedule
  "Add `entity` to `scheduler` and start scheduling according to `iso-str`."
  ([scheduler iso-str entity]
   (add-schedule scheduler nil iso-str entity))
  ([scheduler scid iso-str entity]
   (let [scid (.add scheduler scid iso-str entity)]
     (start-scheduling scheduler iso-str entity scid)
     scid)))

(defn expire
  "Expires a schedule by id and cancels all further executions."
  [scheduler scid]
  (.expire scheduler scid))

(defn expired?
  "Check if schedule by `scid` expired?"
  [scheduler scid]
  (.expired? scheduler scid))

;; Testing {{{1
(comment
  (def rti "R1/2014-11-28T12:18:00/")
  (java.util.Date. (System/currentTimeMillis))
  (java.util.Date. (.getMillis (DateTime.)))
  (java.util.Date.)
  (DateTime. (at/now))
  (= (System/currentTimeMillis) (.getMillis (DateTime.)))
  (/ (- (.getMillis (second (parse-schedule rti))) (at/now)) 1000.0)
  (parse-schedule "R5//PT10S")
  (def sc (get-mem-scheduler))
  (def scid (add-schedule sc rti #(log/info "OK")))
  (def scid1 (add-schedule sc "R//PT1S" #(log/info "OK")))
  (expire sc scid)

  (def sc (get-mem-scheduler {1 {:id 1 :iso-str "R//PT1S" :entity #(log/info "KO")}}))
  (boot sc)
  (expire sc 1))
