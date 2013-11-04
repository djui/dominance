# clj-dominant-color

A library to extract the dominant color in an image. The algorithm is optimized
for graphics like podcast covers, rather than pictures.

## Background

After comparing the **k-means clustering** and **Connected-component labeling**
algorithm, it became clear that the **k-means clustering** algorithm is a better
fit for pictures and the **Connected-component labeling** algorithm is a better
for graphics.

This is obvious when comparing the results for sample image `30.jpg` and
`31.jpg`. The dominant color should be in the yellow top bar (`#??????`) but the
**k-means clustering** algorithm fails on one of them because of overloading
noise from the picture beneath.

## Usage

    $ lein run

## License

Copyright © 2013 Uwe Dauernheim <uwe@dauernheim.net>

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
