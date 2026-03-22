(ns ics-datetimes
  (:require [clojure.string :as str]
            [tick.core :as t]))

(def month-to-ordinal
  {"Jan" 1 "Feb" 2 "Mar" 3 "Apr" 4 "May" 5 "Jun" 6
   "Jul" 7 "Aug" 8 "Sep" 9 "Oct" 10 "Nov" 11 "Dec" 12})

(defn format-date-time
  "Format `zoned-date-time` appropriately for use in .ics format"
  [zoned-date-time]
  (t/format (t/formatter "yyyyMMdd'T'HHmmss'Z'")
            (t/in zoned-date-time "UTC")))

(defn now-timestamp
  []
  (format-date-time (t/instant)))

(defn text-to-date-components
  "Parse str components into a date. Assumes a 2-digit year."
  [[day-str month-str year-str]]
  [(+ (Integer/parseInt year-str) 2000)
   (get month-to-ordinal month-str)
   (Integer/parseInt day-str)])

(defn date-from-str
  "Takes a str like '1 Apr 26' and converts it to a date"
  [input-str]
  (apply t/new-date (text-to-date-components
                      (str/split input-str #" "))))

(defn date-time-from
  "Parse `time-str` and `date-str` into a ZonedDateTime
  `time-str` should be something like '08:30'
  `date-str` expected to be something like '1 Apr 26'"
  [time-str date-str]
  (t/in (t/at (date-from-str date-str)
              (t/time time-str))
        "Europe/London"))

(defn parse-start-end-times
  "Parse start/end time/date strs into zoned times which are then formatted"
  [[start-t-str start-d-str end-t-str end-d-str]]
  (->> [[start-t-str start-d-str] [end-t-str end-d-str]]
       (map #(apply date-time-from %))
       (map format-date-time)))

