(ns inc-knowledge.templates
  (:require [net.cgrand.enlive-html :refer :all]
            [clojure.java.io :as io]))

(defn- stylesheet-link [path]
  {:tag :link, :attrs {:rel "stylesheet" :href path}})

(defn- inline-script [request path]
  {:tag :script
   :content (->> request :optimus-assets
                 (filter #(= path (:original-path %)))
                 first :contents)})


(deftemplate layout "templates/layout.html" [body]
  [:head :title] (content "(inc knowledge)")
  [:div#content] (html-content body))
