(ns nolan.store
  (:import [java.util UUID]))

(defprotocol ScheduleStore
  "Interface for custom schedule stores. Implementations of this protocol
  can make schedule meta persistent to datastore of choice."
  (add [scheduler scid iso-str entity]
    "Save a schedule defined by `entity` with ISO8601 string `iso-str`, should
    return a schedule id. User may opt to pass a nil id in which case
    implementation should generate one and return it.")
  (get-schedule [scheduler scid]
    "Get a schedule i.e. map with keys :iso-str, :id and :entity.")
  (expire [scheduler scid]
    "Expire an schedule by `scid`. Stops all executions for a schedule after this call.")
  (expired? [scheduler scid]
    "Check if expired.")
  (get-active-schedules [scheduler]
    "Return all active schedules from the store.")
  (execute [scheduler entity]
    "How to execute `entity`."))

; In-Memory ScheduleStore implementation. {{{1
(deftype MemScheduleStore [store]
  ScheduleStore
  (add [this scid iso-str entity]
    (let [scid (or scid (UUID/randomUUID))]
      (swap! store assoc scid {:iso-str iso-str :entity entity :id scid})
      scid))
  (get-schedule [this scid]
    (get @store scid))
  (expire [this scid]
    (swap! store dissoc scid))
  (expired? [this scid]
    (nil? (get-schedule this scid)))
  (get-active-schedules [this]
    (vals @store))
  (execute [this entity]
    (entity)))
