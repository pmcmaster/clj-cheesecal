(ns ics
  (:require [ics-datetimes]
            [selmer.parser :as sel]))

(def ics-template "#BEGIN:VCALENDAR
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
  "Write out .ics to STDOUT for events in `start-end-time-strs`"
  [start-end-time-strs]
  (let [events (->> start-end-time-strs 
                    (map ics-datetimes/parse-start-end-times)
                    (map (fn [[start end]] {:start start
                                            :end end})))
        timestamp (ics-datetimes/now-timestamp)
        data-map {:timestamp timestamp :events events}]
    (println (sel/render ics-template data-map))))


