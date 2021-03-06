(ns dominance.image
  (:import java.net.URL
           javax.imageio.ImageIO
           org.imgscalr.Scalr
           org.imgscalr.Scalr$Method
           org.imgscalr.Scalr$Mode)
  (:require [clojure.java.io :as io]))


;;; Globals

(def RGB-BLACK [  0   0   0])
(def RGB-WHITE [255 255 255])
(def RGB-RED   [255   0   0])
(def RGB-GREEN [  0 255   0])
(def RGB-BLUE  [  0   0 255])

(def YUV-BLACK [ 16 128 128])
(def YUV-WHITE [235 128 128])
(def YUV-RED   [ 82  90 240])
(def YUV-GREEN [144  54  34])
(def YUV-BLUE  [ 41 240 110])


;;; Internals

(defn- as-url [path]
  (cond
   (instance? URL path) path
   (instance? java.io.File path) (.toURL ^java.io.File path)
   (instance? String path) (URL. path)))

;;; Interface

;; Image

(defn load-image [path]
  (ImageIO/read
   ^java.net.URL (as-url path)))

(defn save-image [image type path]
  (ImageIO/write
   ^java.awt.image.BufferedImage image
   ^String type
   ^java.io.File (io/file path)))

(defn resize [width height image]
  (Scalr/resize
   ^java.awt.image.BufferedImage image
   Scalr$Method/AUTOMATIC
   ^int width
   ^int height
   nil))

(defn resize-width [width image]
  (Scalr/resize
   ^java.awt.image.BufferedImage image
   Scalr$Method/AUTOMATIC
   Scalr$Mode/FIT_TO_WIDTH
   ^int width
   nil))

(defn resize-height [height image]
  (Scalr/resize
   ^java.awt.image.BufferedImage image
   Scalr$Method/AUTOMATIC
   Scalr$Mode/FIT_TO_HEIGHT
   ^int height
   nil))

;; Color

(defn get-pixels [^java.awt.image.BufferedImage image]
  (let [w (.getWidth image)
        h (.getHeight image)]
    (.getRGB image 0 0 w h nil 0 w)))

(defn set-pixels [^java.awt.image.BufferedImage image pixels]
  (let [w (.getWidth image)
        h (.getHeight image)]
    (.setRGB image 0 0 w h pixels 0 w)))

(defn clamp [color]
  (->> color
       (map #(cond (> % 255) 255
                   (< % 0) 0
                   :else (int %)))
       vec))

(defn pixel->argb [pixel]
  [(bit-shift-right (bit-and pixel 0xFF000000) 24)
   (bit-shift-right (bit-and pixel 0x00FF0000) 16)
   (bit-shift-right (bit-and pixel 0x0000FF00)  8)
   (bit-and pixel 0x000000FF)])

(defn pixel->rgb [pixel]
  (subvec (pixel->argb pixel) 1))

(defn rgb->yuv [[r g b]]
  ;; Courtesy http://en.wikipedia.org/wiki/YUV
  [(+ (bit-shift-right (+ (*  66 r) (* 129 g) (*  25 b) 128) 8)  16)
   (+ (bit-shift-right (+ (* -38 r) (* -74 g) (* 112 b) 128) 8) 128)
   (+ (bit-shift-right (+ (* 112 r) (* -94 g) (* -18 b) 128) 8) 128)])

(defn pixel->yuv [pixel]
  (-> pixel pixel->rgb rgb->yuv))

(defn yuv->rgb [[y u v]]
  ;; Courtesy http://msdn.microsoft.com/en-us/library/windows/desktop/dd206750(v=vs.85).aspx
  (let [c (- (int y)  16)
        d (- (int u) 128)
        e (- (int v) 128)]
    (clamp [(bit-shift-right (+ (* 298 c)            (*  409 e) 128) 8)
            (bit-shift-right (+ (* 298 c) (* -100 d) (* -208 e) 128) 8)
            (bit-shift-right (+ (* 298 c) (*  516 d)            128) 8)])))

(defn argb->pixel [[a r g b]]
  (bit-or (bit-shift-left a 24)
          (bit-shift-left r 16)
          (bit-shift-left g  8)
          b))

(defn rgb->pixel [[r g b]]
  (argb->pixel [255 r g b]))

(defn yuv->pixel [yuv]
  (-> yuv yuv->rgb rgb->pixel))

(defn rgb->hex [[r g b]]
  (format "#%02X%02X%02X" r g b))

(defn luma [[y u v]]
  y)

(defn chrominance [[y u v]]
  [u v])

(defn- euclidean-distance [a b]
  (reduce + (map #(* % %) (map - a b))))

(defn luma-distance [rgb1 rgb2]
  (let [y1 (luma (rgb->yuv (clamp rgb1)))
        y2 (luma (rgb->yuv (clamp rgb2)))]
    (euclidean-distance [y1] [y2])))

(defn chrominance-distance [rgb1 rgb2]
  (let [uv1 (chrominance (rgb->yuv (clamp rgb1)))
        uv2 (chrominance (rgb->yuv (clamp rgb2)))]
    (euclidean-distance uv1 uv2)))

(defn contrast [rgb1 rgb2]
  (let [yuv1 (rgb->yuv (clamp rgb1))
        yuv2 (rgb->yuv (clamp rgb2))]
    (euclidean-distance yuv1 yuv2)))
