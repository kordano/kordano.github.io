(ns inc-knowledge.posts
  (:require [markdown.core :refer [md-to-html-string]]
            [clojure.java.io :refer [file]]))


(defn parse-posts [path]
  (let [directory (file path)]
    (for [post (remove #(.isDirectory %) (file-seq directory))]
      {:post (-> post slurp md-to-html-string)
       :target (clojure.string/replace (.getName post) #".md" ".html")})))
