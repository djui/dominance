# Dominance

A library to extract the dominant color in an image. The algorithm is optimized
for graphics like podcast covers, rather than pictures.

## Background

Lately many mobile clients exploit the feature to tint their UI elements
color which is dominant in the podcast or album cover. This library tries to
resample this feature.

## Usage

To see an examplary usage, run the following command:

```shell
$ lein run > result.html
$ open result.html
```

To use the library in your application, use the following API:

```clojure
(require '[dominance.core :as dominance]
         '[clojure.java.io :as io])

;; Extract a palette of maximum 5 most dominant colors
(->> "resources/01.jpg" io/file dominance/palette (map :hex))

;; Extract the most dominant color
(->> "resources/01.jpg" io/file dominance/color :hex)

;; Extract the most dominant background and foreground colors
(->> "resources/01.jpg" io/file dominance/bg-fg (map :hex))
```

## Credits

Courtesy to the following authors:

* [John Lawrence Aspden](http://www.learningclojure.com/2011/01/k-means-algorithm-for-clustering-data.html)

## License

Copyright © 2013 Uwe Dauernheim <uwe@dauernheim.net>

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
