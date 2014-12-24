# nolan

[![Travis Badge](https://img.shields.io/travis/kul/nolan.svg?style=flat)](https://travis-ci.org/kul/nolan "Travis Badge")

For scheduling task according to limited `Repeating Time Interval` Syntax of [ISO 8601][iso].

#Install

[![Clojars Project](http://clojars.org/nolan/latest-version.svg)](http://clojars.org/nolan)

## Usage

nolan is a customizable scheduler which can schedule according to ISO_8601
syntax of Repeating Time Interval. It can be customized to make the schedules
persistent to datastore of users choice by implementing a simple protocol.

```clojure
(use 'nolan.core)
```
nolan comes with a inbuilt in-memory scheduler which keeps all schedules in a
atom and its implementation can be used as a starting point for implementing
custom schedulers.
```clojure
(def sc (get-mem-scheduler))
```
in-memory scheduler can be used to schedule functions directly.

```clojure
(add-schedule sc "R4//" #(println "ok")) ; schedule 4 times starting from `now` and tail previous execution
(def scid (add-schedule sc "R//PT2S" #(println "ok"))) ; schedule indefinitely starting from `now` every 2 seconds
```

`add-schedule` returns a schedule id which can be used to expire a scehdule
which stops all further executions and removes it from schedule store.

```clojure
(expire sc scid)
(expired? s cscid) ;=> true
```

## Custom Schedule Stores

[`MemScheduleStore`][memstore] in namespace nolan.store can be used as a
reference for implementing custom stores by extending protocol
[`ScheduleStore`][sstore].

## License
Copyright © 2014 kul

Distributed under the Eclipse Public License.

[iso]: http://en.wikipedia.org/wiki/ISO_8601#Repeating_intervals
[memstore]: https://github.com/kul/nolan/blob/master/src/nolan/store.clj#L23
[sstore]: https://github.com/kul/nolan/blob/master/src/nolan/store.clj#L4
