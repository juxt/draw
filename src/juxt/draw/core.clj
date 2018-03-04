(ns juxt.draw.core
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [hiccup2.core :as h]))

(defn viewbox-str [m]
  (string/join " " ((juxt :x :y :w :h) m)))

(defn margin [viewbox pad]
  (assert pad)
  (-> viewbox
      (update :x - pad)
      (update :y - pad)
      (update :w + pad pad)
      (update :h + pad pad)))

(defn grid [{id :id width :canvas-width height :canvas-height}]
  [:g {:id id}
   (for [x (range 0 (inc width) 10)]
     [:g
      (when (pos? x)
        [:text {:x x :y 0 :font-size 4 :text-anchor "middle"} (format "%s" x)])
      [:line {:x1 x :y1 0 :x2 x :y2 height :stroke "#ccf" :stroke-width 0.2}]])

   (for [y (range 0 (inc height) 10)]
     [:g
      (when (pos? y)
        [:text {:x 0 :y y :font-size 4 :text-anchor "end" :alignment-baseline "central" :dy 2} (format "%s" y)])
      [:line {:x1 0 :y1 y :x2 width :y2 y :stroke "#ccf" :stroke-width 0.2}]])])

(defn rectangle [{:keys [cx cy x y w h title title2 color type]
                  :or {color "#777"
                       w 10
                       h 10

                       }}]
  (let [hw (int (/ w 2))
        hh (int (/ h 2))
        cx (or cx (+ (or x 0) hw))
        cy (or cy (+ (or y 0) hh))
        x (or x (- cx hw))
        y (or y (- cy hh))]

    [:g
     [:rect {:x x
             :y y
             :width (+ hw hw)
             :height (+ hh hh)
             :fill color}]
     [:text {:x cx :y (+ cy hh) :dy (- (if title2 0 3) hh) :text-anchor "middle" :font-size 6 :font-family "sans-serif" :fill "#ddd"} title]

     (when title2
       [:text {:x cx :y (+ cy hh) :dy (- 5 hh) :text-anchor "middle" :font-size 4 :font-family "sans-serif" :fill "#ddd"} (format "(%s)" title2)])

     (when type
       [:text {:x (dec (+ cx hw)) :y (- cy hh) :text-anchor "end"
               :dy 5
               :font-size 5
               :font-family "sans-serif"
               :fill "#ffff"
               } type]

       )]))

(defn cloudfront [m]
  (rectangle (merge {:w 260 :h 20
                     :color "#500"
                     :type "CloudFront"} m)))

(defn rds [m]
  (rectangle (merge {:w 140 :h 20
                     :color "#005d"
                     :type "RDS"} m)))

(defn s3 [m]
  (rectangle (merge {:w 140 :h 20
                     :color "#aa5d"
                     :type "S3"} m)))

(defn ec2 [m]
  (rectangle
    (merge {:w 40 :h 20
            :color "#008a"
            :type "EC2"} m)))

(defn alb [m]
  (rectangle (merge {:w 80 :h 20 :title "ALB" :color "#d60a" :type "ALB"} m)))

(defn line [& points]
  [:polyline {:fill "none" :stroke "#333a" :stroke-width 1.5
              :points (string/join " " (map (fn [[x y]] (format "%s,%s" x y)) points))}])

(defn availability-zone [m]
  (rectangle (assoc m :color "#0606")))

(defn cross [[x y]]
  [:g
   (line [(- x 10) y] [(+ x 10) y])
   (line [x (- y 10)] [x (+ y 10)])])

(defn isomorph [x h]
  [:g {:transform (format "translate(140,%d)" h)}
   [:g {:transform "scale(0.5,0.433)"}
    [:g {:transform "rotate(30)"}
     [:g {:transform "skewX(-30)"}
      x]]]])

(defmacro svg [config & body]
  `(h/html
     [:svg
      {:xmlns "http://www.w3.org/2000/svg"
       :version "1.1"
       :xmlns:xlink "http://www.w3.org/1999/xlink"
       :viewBox (-> {:x 0 :y 0 :w (:canvas-width ~config) :h (:canvas-height ~config)}
                    (margin (:margin ~config))
                    viewbox-str)}
      ~@body]))

(defn blur [{:keys [id margin canvas-width canvas-height]}]
  [:filter {:id id :x (- margin) :y (- margin) :filterUnits "userSpaceOnUse" :width (+ canvas-width margin margin) :height (+ canvas-height margin margin)}
   [:feGaussianBlur {:in "SourceAlpha" :stdDeviation 6 :result "blur"}]])

(defn render [{:keys [layers grid? isometric?] :as config}]
  (svg
    config
    [:defs
     (blur (assoc config :id "shadow"))
     (when grid? (grid (assoc config :id "grid")))
     (for [layer layers]
       [:g {:id (str (name (:id layer)))}
        (:content layer)])]

    (if isometric?
      [:g
       (when grid? (isomorph [:use {:xlink:href "#grid"}] 0))
       (for [layer layers]
         (list
           #_(isomorph [:use {:xlink:href (str "#" (name (:id layer))) :filter "url(#shadow)"}] (get layer :lower 0))
           (isomorph [:use {:xlink:href (str "#" (name (:id layer)))}] (get layer :lower 0))))]

      ;; 2D
      [:g
       (when grid? [:use {:xlink:href "#grid"}])
       (for [layer layers]
         [:use {:xlink:href (str "#" (name (:id layer)))}])])))
