(ns clj-dominant-color.core
  (:import java.net.URL)
  (:require [clj-dominant-color.html    :as html]
            [clj-dominant-color.image   :as image]
            [clj-dominant-color.k-means :as k-means]
            [clojure.java.io            :as io]
            [net.cgrand.enlive-html     :as css]))


;;; Utilities

(def & partial)
(defn- try' [f & args] (try (apply f args) (catch Exception _)))
(defn- sqr [n] (* n n))

(defn download [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))


;;; Internals

(defn- cache [urls]
  (let [files (map (& format "resources/%02d.jpg") (iterate inc 1))]
    (dorun (pmap #(try' download % %2) urls files))))

(defn- images [path]
  (let [jpg? (fn [^java.io.File f] (and (.isFile f) (.endsWith (.getName f) ".jpg")))]
    (->> path io/file file-seq (filter jpg?))))

(defn- image-url [podcast-url]
  (let [dom (try' #(html/dom (URL. podcast-url)))]
    (or (html/select [:itunes:image] (html/attr :href) dom)
        (html/select [:image :url] (comp first :content) dom))))

(defn- podcast-urls [opml-path]
  (->> opml-path
       io/resource
       (try' html/dom)
       (html/select-all [:opml :body :outline :outline] (html/attr :xmlurl))))

(defn- pixels [size img]
  (->> img
       image/load-image
       (image/resize-width size)
       image/get-pixels))

(defn- weight-function [point]
  (or (try' #(/ (sqr (:count point))
                (:stddev point)))
      0))

(defn- dominant-yuv-colors [pixels]
  (let [guesses (list image/YUV-RED
                      image/YUV-GREEN
                      image/YUV-BLUE
                      image/YUV-WHITE
                      image/YUV-BLACK)]
    (->> pixels
         (map image/pixel->yuv)
         (k-means/weighted-centroids guesses weight-function)
         (map #(update-in % [:mean] image/yuv->rgb)))))

(defn- dominant-rgb-colors [pixels]
  (let [guesses (list image/RGB-RED
                      image/RGB-GREEN
                      image/RGB-BLUE
                      image/RGB-WHITE
                      image/RGB-BLACK)]
    (->> pixels
         (map image/pixel->rgb)
         (k-means/weighted-centroids guesses weight-function))))

(defn- decorate [color]
  (assoc-in color [:hex] (-> color :mean image/clamp image/rgb->hex)))

(defn- filter-palette [palette]
  (let [weight-threshold 120] ;; 130
    (if (> (count palette) 2)
      (filter #(> (:weight %) weight-threshold) palette)
      palette)))

(defn- dominant-colors [^java.io.File img]
  (let [pixels (pixels 100 img)
        yuv-palette (dominant-yuv-colors pixels)
        rgb-palette (dominant-rgb-colors pixels)
        best-palette (max-key count rgb-palette yuv-palette)
        palette (map decorate best-palette)]
    (cons (.getPath img)
          (filter-palette palette))))

(defn- html [result]
  (println "<style>")
  (println ".podcast { display:inline-block; }")
  (println ".podcast > img { width:200px; height:200px; }")
  (println ".podcast > div { border:1px solid gray; display:inline-block; width:30px; height:30px; }")
  (println ".podcast > span { display:none; }")
  (println "</style>")
  (doseq [[path & palette] result]
    (println "<div class=\"podcast\">")
    (println (format "  <img src=\"%s\" /><br />" path))
    (doseq [color (sort-by :weight palette)]
      (println (format "  <div style=\"background-color:%s;\"></div>" (:mean color)))
      (println "</div>"))))


;;; Interface

;; Main

;;; TODO: Create new function \"weighted-centroids\" that
;;;       returns a sorted by weight list of centroids [center weight(%?)].
;;; TODO: Add weighting between brightness and away from black & white
;;; TODO: Add stddev and mean to centroid
(defn -main
  " Generation Rule: Resize to 100x100px, calculate both, YUV and RGB, prefer
    YUV, but pick RGB if more colors.

    Selection Rule: Sort color by mu with smallest sigma * brightness.
    Pick from top as soon as not too bright."
  []
  #_(cache (map image-url (podcast-urls "podcasts.opml")))
  (html (map dominant-colors (images "resources"))))
