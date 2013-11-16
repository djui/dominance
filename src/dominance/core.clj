(ns dominance.core
  (:require [dominance.image   :as image]
            [dominance.k-means :as k-means]))


;;; Internals

(defn- filter-palette [palette]
  (let [weight-threshold 120] ;; 130
    (if (> (count palette) 2)
      (filter #(> (:weight %) weight-threshold) palette)
      palette)))

(defn- decorate-palette [palette]
  (let [bg-color (first palette)
        chrominance-dist #(image/chrominance-distance (:mean bg-color) (:mean %))
        contrast         #(image/contrast             (:mean bg-color) (:mean %))
        hex              #(image/rgb->hex (image/clamp                 (:mean %)))]
  (map #(as-> % c
              (assoc-in c [:chrominance-dist] (chrominance-dist c))
              (assoc-in c [:contrast] (contrast c))
              (assoc-in c [:hex] (hex c)))
       palette)))

(defn- weight-fn [point]
  (let [{:keys [count stddev]} point]
    (if (pos? stddev)
      (/ (* count count) stddev)
      0)))

(defn- palette-yuv [pixels]
  (let [guesses (list image/YUV-RED
                      image/YUV-GREEN
                      image/YUV-BLUE
                      image/YUV-WHITE
                      image/YUV-BLACK)]
    (->> pixels
         (map image/pixel->yuv)
         (k-means/weighted-centroids guesses weight-fn)
         (map #(update-in % [:mean] image/yuv->rgb)))))

(defn- palette-rgb [pixels]
  (let [guesses (list image/RGB-RED
                      image/RGB-GREEN
                      image/RGB-BLUE
                      image/RGB-WHITE
                      image/RGB-BLACK)]
    (->> pixels
         (map image/pixel->rgb)
         (k-means/weighted-centroids guesses weight-fn))))

(defn- pixel-data [size img]
  (->> img
       image/load-image
       (image/resize-width size)
       image/get-pixels))


;;; Interface

(defn palette [^java.io.File img]
  (->> img
       (pixel-data 100)                 ;; Resize
       ((juxt palette-rgb palette-yuv)) ;; Compute two palettes
       (apply max-key count)            ;; Pick best palette
       filter-palette                   ;; Filter out "weak" colors
       decorate-palette))

(defn color [^java.io.File img]
  (first (palette img)))

(defn bg-fg [^java.io.File img]
  (let [palette (palette img)
        contrast-key (if (> (apply max (map :chrominance-dist palette)) 200)
                       :chrominance-dist
                       :contrast)
        bg-color (first palette)
        fg-color (apply max-key contrast-key palette)]
    [bg-color fg-color]))
