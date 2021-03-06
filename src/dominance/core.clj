(ns dominance.core
  (:require [dominance.image   :as image]
            [dominance.k-means :as k-means]))


;;; Utilities

(defn- pjuxt [& fns]
  (fn [& args]
    (vec (pmap #(apply % args) fns))))


;;; Internals

(defn filter-palette [palette]
  (let [weight-threshold 120] ;; 130
    (if (> (count palette) 2)
      (filter #(> (:weight %) weight-threshold) palette)
      palette)))

(defn decorate-palette [palette]
  (let [bg-mean (:mean (first palette))]
    (map #(merge % {:chrominance (image/chrominance-distance bg-mean (:mean %))
                    :contrast (image/contrast bg-mean (:mean %))
                    :hex (image/rgb->hex (image/clamp (:mean %)))})
         palette)))

(defn weight-fn [point]
  (let [{:keys [count stddev]} point]
    (if (pos? stddev)
      (/ (* count count) stddev)
      0)))

(defn palette-yuv [pixels]
  (let [guesses (list image/YUV-RED
                      image/YUV-GREEN
                      image/YUV-BLUE
                      image/YUV-WHITE
                      image/YUV-BLACK)]
    (as-> pixels x
         (map image/pixel->yuv x)
         (k-means/centroids guesses x {:weight-fn weight-fn, :iterations 10})
         (map #(update-in % [:mean] image/yuv->rgb) x))))

(defn palette-rgb [pixels]
  (let [guesses (list image/RGB-RED
                      image/RGB-GREEN
                      image/RGB-BLUE
                      image/RGB-WHITE
                      image/RGB-BLACK)]
    (as-> pixels x
         (map image/pixel->rgb x)
         (k-means/centroids guesses x {:weight-fn weight-fn, :iterations 10}))))

(defn pixel-data [size img]
  (->> img
       image/load-image
       (image/resize-width size)
       image/get-pixels))


;;; Interface

(defn palette [^java.io.File img]
  (->> img
       (pixel-data 100)                  ;; Resize
       ((pjuxt palette-rgb palette-yuv)) ;; Compute two palettes
       (apply max-key count)             ;; Pick best palette
       filter-palette                    ;; Filter out "weak" colors
       decorate-palette))

(defn color [^java.io.File img]
  (first (palette img)))

(defn bg-fg [^java.io.File img]
  (let [palette (palette img)
        contrast-key (if (> (apply max (map :chrominance palette)) 200)
                       :chrominance
                       :contrast)
        bg-color (first palette)
        fg-color (apply max-key contrast-key palette)]
    [bg-color fg-color]))
