(defproject siili/humanize "0.1.0-SNAPSHOT"
  :description "Translate computer produced garble into human readable form"
  :url "https://github.com/siilisolutions/humanize"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [prismatic/schema "1.1.7"]]
  :source-paths ["src/main/clj"]
  :test-paths ["src/test/clj"]
  :repl-options {:init-ns humanize.user}
  :plugins [[lein-cljfmt "0.5.7"]
            [lein-codox "0.10.3"]
            [lein-kibit "0.1.5"]]
  :codox {:output-path "docs/api"})
