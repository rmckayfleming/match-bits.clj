(ns rmckayfleming.match-bits
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn- pattern-variable-characters [pattern]
  (set/difference (set (name pattern))
                  #{\_ \0 \1 \%}))

(defn- valid-pattern-variables? [pattern]
  (let [pattern (name pattern)]
    (every? (fn [char]
              (= 1 (count (re-seq (re-pattern (str char "+")) pattern))))
            (pattern-variable-characters pattern))))

(defn- throw-pattern-error! [pattern]
  (when-not (or (number? pattern) (symbol? pattern))
    (throw (ex-info (str "Bit patterns must be a literal number, or pattern symbol. Given '" pattern "'.") {})))
  (when (symbol? pattern)
    (when-not (re-matches #"%[a-zA-Z01_]+" (apply str (rest (name pattern))))
      (throw (ex-info (str "Bit pattern symbols must start with a %, followed by the characters 0, 1, _, a-z, and A-Z. Given '" pattern "'.") {})))
    (when-not (valid-pattern-variables? pattern)
      (throw (ex-info (str "Bit pattern symbol variables must be contiguous (i.e. %aba is not allowed because a exists in two places). Given '" pattern "'.") {})))))

(defn- valid-pattern? [pattern]
  (or (number? pattern)
      (and (symbol? pattern)
           (= \% (first (name pattern)))
           (re-matches #"[a-zA-Z01_]+" (apply str (rest (name pattern))))
           (valid-pattern-variables? pattern))))

(defn- pattern-variable-mask [pattern char]
  (-> (name pattern)
      (str/replace (re-pattern (str "[^" char "]")) "0")
      (str/replace (str char) "1")))

(defn- pattern-variable-offset [pattern char]
  (reduce (fn [offset char]
            (if (= char \0)
              (inc offset)
              (reduced offset)))
          0
          (reverse (seq (pattern-variable-mask pattern char)))))

(defn- pattern-variable->mask [pattern char]
  (Long/parseUnsignedLong (pattern-variable-mask pattern char) 2))

(defn- pattern-variable->symbol [pattern symbol-char]
  (symbol (apply str (filter (fn [char] (= char symbol-char)) (seq (name pattern))))))

(defn- pattern->mask [pattern]
  (let [pattern (apply str (rest (name pattern)))]
    (-> pattern
        (str/replace "0" "1")
        (str/replace #"[^1]" "0")
        (Long/parseUnsignedLong 2))))

(defn- pattern->test [pattern]
  (let [pattern (apply str (rest (name pattern)))]
    (-> pattern
        (str/replace #"[^01]" "0")
        (Long/parseUnsignedLong 2))))

(defn- has-pattern-variables? [pattern]
  (not (empty? (pattern-variable-characters pattern))))

(defn- match-bits-clause [test-var pattern-variable body]
  (when-not (valid-pattern? pattern-variable)
    (throw-pattern-error! pattern-variable))
  (if (number? pattern-variable)
    `((= ~pattern-variable ~test-var) ~body)
    (let [pattern-binding (fn [mask offset]
                            (if (not= offset 0)
                              `(bit-shift-right (bit-and ~test-var ~mask) ~offset)
                              `(bit-and ~test-var ~mask)))]
      `((= (bit-and ~test-var ~(pattern->mask pattern-variable))
           ~(pattern->test pattern-variable))
        ~(if (has-pattern-variables? pattern-variable)
           (let [pat-chars (pattern-variable-characters pattern-variable)
                 pat-var-syms (map (partial pattern-variable->symbol pattern-variable) pat-chars)
                 pat-var-masks (map (partial pattern-variable->mask pattern-variable) pat-chars)
                 pat-var-offsets (map (partial pattern-variable-offset pattern-variable) pat-chars)
                 pat-var-bindings (map pattern-binding pat-var-masks pat-var-offsets)
                 bindings (interleave pat-var-syms pat-var-bindings)]
             `(let [~@bindings]
                ~body))
           body)))))

(defmacro match-bits
  "Destructure a number by patterns of bits. Takes an expression that evaluates to a number,
   followed by a series of pattern symbols and bodies. A pattern symbol starts with % and is followed
   by a series of 0s, 1s, and other alphabetic characters. A pattern matches when the 0s and 1s match
   the same positions in the number. The other positions in the number will be destructured and bound
   to variables based on their character. For instance, 01aa01 will match 011001 and will bind the
   symbol a to 10 in the case body. Alphabetic characters must be contiguous. 01a1a0 is illegal."
  [num & clauses]
  (let [test-var (gensym)
        else-body (when (odd? (count clauses))
                    (last clauses))
        clauses (mapcat (fn [clause] (apply match-bits-clause test-var clause)) (partition 2 clauses))]
    `(let [~test-var ~num]
       (cond
         ~@clauses
         ~@(when else-body `(:else ~else-body))))))