(ns main
  (:import java.net.URL)
  (:require [clojure.java.io :as io]
            [dominance.core  :as dominance]
            [html            :as html]))


;;; Utilities

(defn- try'
  ([f] (try' f nil))
  ([f default] (try (f) (catch Exception _ default))))

(defn download [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))


;;; Internals

(defn- cache [urls]
  (let [files (map (partial format "resources/%02d.jpg") (iterate inc 1))]
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

(defn- html [result]
  (println "<style>")
  (println ".podcast { display:inline-block; }")
  (println ".podcast > img { width:200px; height:200px; }")
  (println ".podcast > div { border:1px solid gray; display:inline-block; width:30px; height:30px; }")
  (println "</style>")
  (doseq [[path & palette] result]
    (println "<div class=\"podcast\">")
    (println (format "  <img src=\"%s\" /><br />" path))
    (let [bg-fg-threshold 200
          bg-color (first palette)
          chrominance-colors (map #(assoc-in % [:contrast] (image/chrominance-distance (:mean bg-color) (:mean %))) palette)
          contrast-colors (map #(assoc-in % [:contrast] (image/contrast (:mean bg-color) (:mean %))) palette)
          chrominance-fg-color (apply max-key :contrast chrominance-colors)
          contrast-fg-color (apply max-key :contrast contrast-colors)
          fg-color (if (> (:contrast chrominance-fg-color) bg-fg-threshold) chrominance-fg-color contrast-fg-color)]
      (println (format "  <div style=\"background-color:%s;\">BG</div>" (:hex bg-color)))
      (println (format "  <div style=\"background-color:%s;\">FG</div>" (:hex fg-color)))
      (println "  <br />")
      (doseq [color palette]
        (println (format "  <div style=\"background-color:%s;\"></div>" (:hex color)))
        (println (format "  <!-- w:%.2f s:%.2f c:%d k:%d -->" (:weight color) (:stddev color) (:count color) (:contrast color)))))
    (println "</div>")))


;;; Interface

;; Main

(defn -main
  " Generation Rule: Resize to 100x100px, calculate both, YUV and RGB, prefer
    YUV, but pick RGB if more colors.

    Selection Rule: Sort color by mu with smallest sigma * brightness.
    Pick from top as soon as not too bright."
  []
  #_(cache (map image-url (podcast-urls "podcasts.opml")))
  (->> (images "resources")
       (map #(cons (.getPath %) (dominant-color/analyze %)))
       html))
