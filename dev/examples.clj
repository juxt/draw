(ns juxt.draw.examples
  (:require
   [clojure.java.io :as io]
   [juxt.draw.core :refer :all]))

(spit
  (io/file "/tmp/foo.svg")
  (render
    {:canvas-width 280
     :canvas-height 280
     :margin 10
     :isometric? false
     :grid? true
     :layers
     [{:id "floor"
       :content
       (list
         (rectangle {:title "R1" :x 100 :y 100 :w 100 :h 40 :color "#0078"})
         (rectangle {:title "R2" :x 140 :y 120 :w 100 :h 40 :color "#0078"})
         (line [20 20] [30 30])

         )}]}))
