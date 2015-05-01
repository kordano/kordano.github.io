(defproject inc-knowledge "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.2"]
                 [stasis "2.2.2"]
                 [markdown-clj "0.9.66"]
                 [enlive "1.1.5"]]
  :plugins [[lein-ring "0.9.3"]]
  :ring {:handler inc-knowledge.core/app}
  :aliases {"build-site" ["run" "-m" "inc-knowledge.core/export"]})
