(ns ics
  (:require [ics-datetimes]
            [clojure.java.io]
            [selmer.parser :as sel]))

;; Module to output very basic .ics files from a template
;; These all have the same event name, and use the provided
;; event.start and event.end times

;; This first template is for an event with a start and end time

(def ics-event-template "#BEGIN:VCALENDAR
#VERSION:2.0
#PRODID:-//cheeseshifts/cheesescraper//NONSGML v1.0//EN
{% for event in events %}
#BEGIN:VEVENT
#UID:cheese{{forloop.counter}}
#DTSTAMP:{{timestamp}}
#DTSTART:{{event.start}}
#DTEND:{{event.end}}
#SUMMARY:Cheeseland
#END:VEVENT{% endfor %}
#END:VCALENDAR")

(defn write-entries
  "Write out .ics to STDOUT for event start/end times in `filename`"
  [filename template]
  (with-open [rdr (clojure.java.io/reader filename)] 
    (let [lines (line-seq rdr)
         events (->> lines 
                     (map ics-datetimes/intermediate-to-start-end)
                     (map (fn [[start end]] {:start start
                                             :end end})))
         timestamp (ics-datetimes/now-timestamp)
         data-map {:timestamp timestamp :events events}]
     (println (sel/render template data-map)))))

(defn write-event-entries
  "Write out .ics to STDOUT for event start/end times in `filename`"
  [filename]
  (write-entries filename ics-event-template))
 
;; This event only has a start time (corresponding to the 'end' of one of the
;; events above with a start and end time)
;; This is used to allow a separate calendar to be set up showing only
;; the end times of the events.

(def ics-event-end-template "#BEGIN:VCALENDAR
#VERSION:2.0
#PRODID:-//cheeseshift-ends/cheesescraper//NONSGML v1.0//EN
{% for event in events %}
#BEGIN:VEVENT
#UID:cheese-end{{forloop.counter}}
#DTSTAMP:{{timestamp}}
#DTSTART:{{event.end}}
#SUMMARY:End of cheese
#END:VEVENT{% endfor %}
#END:VCALENDAR")

(defn write-event-end-entries
  "Write out .ics to STDOUT using only the end times  in `filename`
  These are written to the DTSTART element, so end up with an event
  which marks the end-time of the input events"
  [filename]
  (write-entries filename ics-event-end-template))

