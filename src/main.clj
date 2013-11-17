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
  (let [jpg? (fn [^java.io.File f]
               (and (.isFile f)
                    (.endsWith (.getName f) ".jpg")))]
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

(defn- html-export [result]
  (println "<style>\n"
           ".podcast { display:inline-block; }\n"
           ".podcast > img { width:200px; height:200px; }\n"
           ".podcast > div { border:1px solid gray; display:inline-block;"
           "width:30px; height:30px; }\n"
           "</style>")
  (doseq [[path bg fg] result]
    (printf
     (str "<div class=\"podcast\">\n"
          "  <img src=\"%s\" /><br />\n"
          "  <div style=\"background-color:%s;\"></div>\n"
          "  <div style=\"background-color:%s;\"></div>\n"
          "  <br />\n"
          "</div>\n") path (:hex bg) (:hex fg))))


;;; Interface

;; Main

(defn -main []
  #_(cache (map image-url (podcast-urls "podcasts.opml")))
  (->> (images "resources")
       (pmap #(cons (.getPath %) (dominance/bg-fg %)))
       html-export))
