(ns inc-knowledge.core
  (:require [stasis.core :as stasis]
            [inc-knowledge.pages :as pages]
            [inc-knowledge.templates :as tmpl]
            [inc-knowledge.posts :as posts]))

(def pages {"/index.html" (->> "./pre-posts" posts/parse-posts first :post tmpl/layout (apply str)) })

(defn export []
  (let [posts-dir "./posts"]
    (stasis/empty-directory! posts-dir)
    (stasis/export-pages "./")
    (println "Export to" posts-dir "complete!")))
