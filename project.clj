(defproject trium "0.1"
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [om "0.5.3"]]
  :profiles {:dev {:plugins []}}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                         :output-to "main.js"
                         :output-dir "target"
                         :optimizations :none
                         :source-map true}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                         :output-to "main.js"
                         :optimizations :advanced
                         :pretty-print false
                         :preamble ["react/react.min.js"]
                         :externs ["react/externs/react.js"]}}
             ]})
          ;:source-map "target/main.js.map"
