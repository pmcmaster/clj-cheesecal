(ns cheese-cal
  (:require [ics]
            [shift-data]))

(let [[target filename] *command-line-args*]
  (->> (shift-data/read-from-file target filename)
       (ics/write-entries)))
