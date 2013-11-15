(ns dominance.core
  (:require [dominance.image   :as image]
            [dominance.k-means :as k-means]))


;;; Utilities

(defn- try'
  ([f] (try' f nil))
  ([f default] (try (f) (catch Exception _ default))))

(defn- sqr [n]
  (* n n))


;;; Internals

(defn- weight-function [point]
  (try' #(/ (sqr (:count point)) (:stddev point))) 0)

(defn- filter-palette [palette]
  (let [weight-threshold 120] ;; 130
    (if (> (count palette) 2)
      (filter #(> (:weight %) weight-threshold) palette)
      palette)))

(defn- decorate-color [color]
  (assoc-in color [:hex] (-> color :mean image/clamp image/rgb->hex)))

(defn- yuv-palette [pixels]
  (let [guesses (list image/YUV-RED
                      image/YUV-GREEN
                      image/YUV-BLUE
                      image/YUV-WHITE
                      image/YUV-BLACK)]
    (->> pixels
         (map image/pixel->yuv)
         (k-means/weighted-centroids guesses weight-function)
         (map #(update-in % [:mean] image/yuv->rgb)))))

(defn- rgb-palette [pixels]
  (let [guesses (list image/RGB-RED
                      image/RGB-GREEN
                      image/RGB-BLUE
                      image/RGB-WHITE
                      image/RGB-BLACK)]
    (->> pixels
         (map image/pixel->rgb)
         (k-means/weighted-centroids guesses weight-function))))

(defn- pixel-data [size img]
  (->> img
       image/load-image
       (image/resize-width size)
       image/get-pixels))


;;; Interface

(defn analyze [^java.io.File img]
  (->> img
       (pixel-data 100)
       ((juxt rgb-palette yuv-palette))
       (apply max-key count)
       (map decorate-color)
       (filter-palette)))
