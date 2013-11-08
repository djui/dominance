(ns clj-dominant-color.k-means)
;; Courtesy http://www.learningclojure.com/2011/01/k-means-algorithm-for-clustering-data.html


;;; Utilities


(defn- sqr [n]
  #_(Math/pow n 2)
  (* n n))

;; Scalar

(defn- distance [a b]
  (if (< a b) (- b a) (- a b)))

(defn- average [& l]
  (/ (reduce + l) (count l)))

(defn- stddev [& l]
  (let [avg (apply average l)]
    (Math/sqrt (/ (reduce + (map #(sqr (distance avg %)) l)) (count l)))))

;; Vector

(defn- vec-distance [a b]
  (reduce + (map sqr (map - a b))))

(defn- vec-average [& l]
  (map #(/ % (count l)) (apply map + l)))

(defn- vec-stddev [& l]
  (let [avg (apply vec-average l)]
    (Math/sqrt (/ (reduce + (map #(sqr (vec-distance avg %)) l)) (count l)))))


;;; Internals

(defn- closest [p means distance-fn]
  (first (sort-by #(distance-fn % p) means)))

(defn- point-groups [means data distance-fn]
  (group-by #(closest % means distance-fn) data))

(defn- update-seq [sq f]
  (let [freqs (frequencies sq)]
    (apply concat
           (for [[k v] freqs]
             (if (= v 1) (list (f k))
                 (cons (f k) (repeat (dec v) k)))))))

(defn- new-means [average point-groups old-means]
  (update-seq old-means (fn [o]
                          (if (contains? point-groups o)
                            (apply average (get point-groups o)) o))))

(defn- iterate-means [data distance-fn average]
  (fn [means] (new-means average (point-groups means data distance-fn) means)))

(defn- groups [data distance-fn means]
  (vals (point-groups means data distance-fn)))

(defn- take-while-unstable
  ([sq]
     (lazy-seq (if-let [sq (seq sq)]
                 (cons (first sq)
                       (take-while-unstable (rest sq) (first sq))))))
  ([sq last]
     (lazy-seq (if-let [sq (seq sq)]
                 (if (= (first sq) last)
                   '()
                   (take-while-unstable sq))))))

(defn- k-groups [data distance-fn average-fn]
  (fn [guesses]
    (take-while-unstable
     (map #(groups data distance-fn %)
          (iterate (iterate-means data distance-fn average-fn) guesses)))))

(defn- random-guesses [n data]
  (take n (repeatedly #(rand-nth data))))


;;; Interface

(defn clusters [guesses data]
  (let [guesses' (cond (list?    guesses) guesses
                       (integer? guesses) (random-guesses guesses data))
        k-groups-fn (cond (vector? (first data)) (k-groups data vec-distance vec-average)
                          (number? (first data)) (k-groups data distance average))]
    (last (k-groups-fn guesses'))))

(defn centroids [guesses data]
  (let [average-fn (cond (vector? (first data)) vec-average
                         (number? (first data)) average)]
    (map #(apply average-fn %) (clusters guesses data))))
