(ns threatmodeler.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [cljsjs.react-draggable]
            [goog.string.format]))

(def threat-model (r/atom { :elements { 1 {:type :actor :name "hackerman" :x 30 :y 30 :width 100 :height 100 :id 1}
                                       2 {:type :process :name "webapp" :id 2 :x 400 :y 80 :width 100 :height 50}
                                       3 {:type :communication :from 1 :to 2 :what "cool"}
                                       4 {:type :boundary :x1 100 :y1 100 :x2 200 :y2 200}}
                           :threats []}))


(defonce timer (r/atom (js/Date.)))

(defonce time-color (r/atom "#f34"))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))


(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (str/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))

(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(def draggable (r/adapt-react-class js/ReactDraggable))


(defn calculate-line-points [element1 element2]
  (let [ele1WidthDiv2 (/ (:width element1) 2)
        ele1HeightDiv2 (/ (:height element1) 2)
        ele2WidthDiv2 (/ (:width element2) 2)
        ele2HeightDiv2 (/ (:height element2) 2)]
    {:x1 (+ (:x element1) (:width element1) )
     :y1 (+ (:y element1) ele1WidthDiv2)
     :x2 (:x element2)
     :y2 (+ (:y element2) ele2HeightDiv2)}))


(defn render-line [ {:keys [x1 y1 x2 y2 style] } ]
  (js/console.log x1 y1 x2 y2 style)
  (let [lineLength (js/Math.sqrt (+ (js/Math.pow (- x2 x1) 2)
                                    (js/Math.pow (- y2 y1) 2)))
        slope (/ (- y2 y1) (- x2 x1))
        rotationDegree (js/Math.atan slope)
        style (or style "solid")]

    [:div.line.diagram-threat-model-element {:style {:display :inline-block
                                                     :left "0px"
                                                     :top "0px"
                                                     :height "2px"
                                                     :width (goog.string.format "%dpx" lineLength)
                                                     :color "black"
                                                     :border-top (goog.string.format "2px %s black" style)
                                                     :transform (goog.string.format "translate(%dpx,%dpx) rotate(%frad)" x1 y1 rotationDegree )
                                                     :transform-origin "center left"}}]))

(defn diagram-event-element-drag-stop [event data]
  "Persist position of dragged threat model diagram element to local state."
  (swap! threat-model update-in [:elements 1] merge {:x (-> data .-lastX) :y (-> data .-lastY)}))


(defn render-threat-model-element-common [{:keys [x y type name id]}]
  [draggable {:grid [25 25]
              :defaultPosition {:x x :y y}
              :onStop diagram-event-element-drag-stop}
   [:span.diagram-threat-model-element {:class (str "diagram-" (cljs.core/name type))
                                        :style {:transform (goog.string.format "translate(%dpx,%dpx)" x y)}}
    [:p name]]])


(defn render-threat-model-element-communication [element elements]
  [render-line (calculate-line-points (get elements (:from element)) (get elements (:to element) ))])

(defn render-threat-model-element-boundary [element elements]
  [render-line (assoc-in element [:style] "dashed")])

(defmulti render-threat-model-element (fn [element _] (:type element)))
(defmethod render-threat-model-element :actor [element] (render-threat-model-element-common element))
(defmethod render-threat-model-element :process [element] (render-threat-model-element-common element))
(defmethod render-threat-model-element :communication [element elements] (render-threat-model-element-communication element elements))
(defmethod render-threat-model-element :boundary [element elements] (render-threat-model-element-boundary element elements))

(defn simple-example [threat-model]
  [:div
   (for [element (vals (:elements @threat-model))]
     (render-threat-model-element element (:elements @threat-model)))
   [greeting "Hello world, it is now"]
   [clock]
   [color-input]])


(defn ^:export init! [])
(rdom/render [simple-example threat-model] (js/document.getElementById "app"))

