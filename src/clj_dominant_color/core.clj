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

(defn -main []
  #_(cache (map image-url (podcast-urls "podcasts.opml")))
  (let [size 64 ;; 200
        extract-fn
        #(->> %
              image/load-image
              (image/resize-width size)
              image/get-pixels

              ;;(map image/pixel->rgb)
              ;;(k-means/centroids '(                                             image/RGB-WHITE image/RGB-BLACK)) ;; 2: B W       _result_2c_rgb.html
              ;;(k-means/centroids '(image/RGB-RED image/RGB-GREEN image/RGB-BLUE                                )) ;; 3: R G B     _result_3c_rgb.html
              ;;(k-means/centroids '(image/RGB-RED image/RGB-GREEN image/RGB-BLUE image/RGB-WHITE                )) ;; 4: R G B   W _result_4c_rgb.html
              ;;(k-means/centroids '(image/RGB-RED image/RGB-GREEN image/RGB-BLUE image/RGB-WHITE image/RGB-BLACK)) ;; 5: R G B B W _result_5c_rgb.html

              (map image/pixel->yuv)
              (k-means/centroids '(                                             image/YUV-WHITE image/YUV-BLACK)) ;; 2: B W       _result_2c_yuv.html
              ;;(k-means/centroids '(image/YUV-RED image/YUV-GREEN image/YUV-BLUE                                )) ;; 3: R G B     _result_3c_yuv.html
              ;;(k-means/centroids '(image/YUV-RED image/YUV-GREEN image/YUV-BLUE image/YUV-WHITE                )) ;; 4: R G B   W _result_4c_yuv.html
              ;;(k-means/centroids '(image/YUV-RED image/YUV-GREEN image/YUV-BLUE image/YUV-WHITE image/YUV-BLACK)) ;; 5: R G B B W _result_5c_yuv.html
              (map image/clamp)
              (map image/yuv->rgb)

              (map image/rgb->hex)
              (cons (.getPath %)))
        res (map extract-fn (images "resources"))]
    (doseq [[path & colors] res]
      (println "<div style=\"display:inline-block;\">")
      (println (format "  <img style=\"width:200px; height:200px;\" src=\"%s\" />" path))
      (println "  <br />")
      (doseq [color colors]
        (println (format "  <div style=\"border:1px solid gray; display:inline-block; background-color:%s; width:30px; height:30px\">" color))
        (println "</div>"))
      (println "</div>"))))

;;; TODO: Create new function "weighted-centroids" that
;;;       returns a sorted by weight list of centroids [center weight(%?)].
;;; TODO: Convert RGB to YUV guesses
;;; TODO: Check if YUV is really necessary
;;; TODO: Check for minimal image size
;;; TODO: Add weighting between brightness and away from black & white
;;; TODO: Add stddev and mean to centroid
;;; FIXME: Why 2 missing darkest blue?
;;; FIXME: Why 3 two reds?
;;; FIXME: Why 6 no red?
;;; FIXME: Why 10 no red?
;;; FIXME: Why 13 no blue/turkis?
;;; FIXME: Why 18 four instead of 2 (b/w) colors?
;;; FIXME: Why 19 two reds?
;;; FIXME: Why 40+41 so bad?
