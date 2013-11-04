(ns clj-dominant-color.core
  (:import java.net.URL)
  (:require [clj-dominant-color.html    :as html]
            [clj-dominant-color.image   :as image]
            [clj-dominant-color.k-means :as k-means]
            [clojure.java.io            :as io]
            [net.cgrand.enlive-html     :as css]))


;;; Utilities

(def & partial)

(defn try' [f & args]
  (try (apply f args)
       (catch Exception _)))

(defn download [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))


;;; Internals

(defn- cache [urls]
  (let [files (map (& format "resources/%02d.jpg") (iterate inc 1))]
    (dorun (pmap #(try' download % %2) urls files))))

(defn- images [path]
  (let [jpg? #(and (.isFile %) (.endsWith (.getName %) ".jpg"))]
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


;;; Interface

;; Main

;;; TODO: Create new function \"weighted-centroids\" that
;;;       returns a sorted by weight list of centroids [center weight(%?)].
;;; TODO: Add weighting between brightness and away from black & white
;;; TODO: Add stddev and mean to centroid
(defn -main
  " Generation Rule: Resize to 100x100px, calculate both, YUV and RGB, prefer YUV, but pick RGB if more colors.
    Selection  Rule: ..."
  []
  #_(cache (map image-url (podcast-urls "podcasts.opml")))
  (let [size 100
        yuv-color-guesses (list image/YUV-RED image/YUV-GREEN image/YUV-BLUE
                                image/YUV-WHITE image/YUV-BLACK)
        rgb-color-guesses (list image/RGB-RED image/RGB-GREEN image/RGB-BLUE
                                image/RGB-WHITE image/RGB-BLACK)
        extract-fn (fn [img]
                     (let [pixels (->> img
                                       image/load-image
                                       (image/resize-width size)
                                       image/get-pixels)
                           yuv-res (->> pixels
                                        (map image/pixel->yuv)
                                        (k-means/centroids yuv-color-guesses)
                                        (map image/yuv->rgb))
                           rgb-res (->> pixels
                                        (map image/pixel->rgb)
                                        (k-means/centroids rgb-color-guesses))
                           res (if (> (count rgb-res) (count yuv-res))
                                 rgb-res
                                 yuv-res)]
                       (->> res
                            (map image/clamp)
                            (map image/rgb->hex)
                            (cons (.getPath img)))))
        res (map extract-fn (images "resources"))]
    (doseq [[path & colors] res]
      (println "<div style=\"display:inline-block;\">")
      (println (format "  <img style=\"width:200px; height:200px;\" src=\"%s\" />" path))
      (println "  <br />")
      (doseq [color colors]
        (println (format "  <div style=\"border:1px solid gray; display:inline-block; background-color:%s; width:30px; height:30px\">" color))
        (println "</div>"))
      (println "</div>"))))
