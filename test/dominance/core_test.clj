(ns dominance.core-test
  (:require [clojure.test :refer :all]
            [dominance.core :refer :all]))

(deftest decorate-palette-test
  (is (= (decorate-palette [{:mean [255 128 0]}
                            {:mean [0 0 0]}])
         [{:mean [255 128 0]
           :chrominance 0
           :contrast 0
           :hex "#FF8000"}
          {:mean [0 0 0]
           :chrominance 9850
           :contrast 26750
           :hex "#000000"}])))
