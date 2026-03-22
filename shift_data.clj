(ns shift-data
  (:require [clojure.string :as str]))

(defn is-shift-line
  "Shift lines start with something like '08:30'. Is `line` one of them?"
  [line]
  (re-find #"^\d\d:\d\d," line))

(defn is-header-row-for-target
  "Is `line` a header row that precedes shift data?
  Finds only header rows which are for `target` or when the previous line was
  for target (designated by `in-target`"
  [target in-target line]
  (or (= target line)
      (and in-target (= "Shift details" line))))

(defn append-shift-line-for-target
  "If line is `target` then return with `in-target` as true
  If we are already `in-target` then append any matching shift lines to
  lines-acc"
  [target [lines-acc in-target] line]
  (if (and in-target (is-shift-line line))
    [(conj lines-acc line) in-target]
    (if (is-header-row-for-target target in-target line)
      [lines-acc true]
      [lines-acc false])))

(defn shift-lines-for-target
  "Get only the relevant shift lines for `target` from `lines`"
  [target lines]
  (let [[shift-lines _] (reduce
                          (partial append-shift-line-for-target target)
                          [[] false]
                          lines)]
    shift-lines))

(defn shift-times-from-file
  "Find shifts for `target` in `filename` and return a list of pairs of start
  and end times"
  [target filename]
  (with-open [rdr (clojure.java.io/reader filename)]
    (shift-lines-for-target target (line-seq rdr))))

(defn split-shift-line
  "Get just the four start/end time/date items from `line`"
  [line]
  (let [words (str/split line #",?\s")
        start-time (subvec words 0 1)
        start-date (subvec words 2 5)
        end-time (subvec words 5 6)
        end-date (subvec words 7 10)]
    (map #(str/join " " %) [start-time start-date
                            end-time end-date])))

(defn read-from-file
  [target filename]
  (->> (shift-times-from-file target filename)
       (map split-shift-line)))

