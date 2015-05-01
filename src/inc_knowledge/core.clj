(ns inc-knowledge.core
  (:require [stasis.core :as stasis]
            [inc-knowledge.pages :as pages]
            [inc-knowledge.templates :as tmpl]
            [inc-knowledge.posts :as posts]))

(def pages {"/index.html" (apply str (tmpl/layout (apply str (posts/parse-posts "./posts"))))})

(def app (stasis/serve-pages pages))

(defn export []
  (let [export-dir "./resources/posts"]
    (stasis/empty-directory! export-dir)
    (stasis/export-pages pages export-dir)
    (println "Export to" export-dir "complete!")))
