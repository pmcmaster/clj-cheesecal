(ns shift-data
  (:require
    [clojure.java.io] 
    [clojure.string :as str]))

;; This module parses the saved web page content, extracting out the relevant
;; times and dates for the specified 'target' person. The saved output is split
;; across multiple lines, which must be combined into individual shift start/
;; end times.

(defn is-shift-line
  "Shift lines start with something like '08:30'. Is `line` one of them?"
  [line]
  (re-find #"^\d\d:\d\d," line))

(defn only-shift-lines-for-target
  "Find `target` in `lines` then output subsequent lines until a line is
  encountered which should not be output."
  [target lines]
  (letfn [(find-target [[line & rest-lines] shift-lines]
            (if line
              (if (= line target)
               #(found-target rest-lines shift-lines)
               #(find-target rest-lines shift-lines))
              ;; Exit point when `line` is nil: return shift-lines
              shift-lines))
          (found-target [[line & rest-lines] shift-lines]
            (if (= line "Shift details")
              #(found-target rest-lines shift-lines)
              (if (is-shift-line line)
                #(found-target rest-lines (conj shift-lines line))
                #(find-target rest-lines shift-lines))))]
    (trampoline find-target lines [])))

(defn shift-times-from-file
  "Find shifts for `target` in `filename` and return a list of pairs of start
  and end times"
  [target filename]
  (with-open [rdr (clojure.java.io/reader filename)]
    (only-shift-lines-for-target target (line-seq rdr))))

(defn line-to-intermediate
  "Get just the four start/end time/date items from `line`
  Join only those items, with '-' between start and end"
  [line]
  (let [words (str/split line #",?\s") ;; Maybe comma, followed by whitespace
        start-time (subvec words 0 1)
        start-date (subvec words 2 5)
        end-time (subvec words 5 6)
        end-date (subvec words 7 10)]
    (str/join " " (flatten [start-time start-date "-"
                            end-time end-date]))))

(defn read-from-file
  "Read `filename` and return formatted start/end times for shifts for person
  `target`"
  [target filename]
  (->> (shift-times-from-file target filename)
       (map line-to-intermediate)))

(defn read-and-write
  "Read `filename` and dump out shifts for `target`"
  [target filename]
  (doseq [shift-start-end (read-from-file target filename)]
    (println shift-start-end)))
