(defproject trium "0.1"
  :plugins [[lein-cljsbuild "1.0.3"] [com.cemerick/clojurescript.test "0.3.1"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2261"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.5.3"]
                 [org.clojars.whodidthis/cljs-uuid-utils "1.0.0"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.3.0"]
                 ]
  :profiles {:dev {:plugins []}}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild
  {
   :builds [{:id "dev"
              :source-paths ["src" "test"]
              :compiler {
                         :output-to "main.js"
                         :output-dir "target"
                         :optimizations :none
                         :source-map true
                         }}
            ;; {:id "test"
            ;;  :source-paths ["src" "test"]
            ;;  :compiler {
            ;;             :output-to "testable.js"
            ;;             :optimizations :simple
            ;;             }}
            ;; {:id "release"
            ;;   :source-paths ["src"]
            ;;   :compiler {
            ;;              :output-to "main.js"
            ;;              :optimizations :advanced
            ;;              :pretty-print false
            ;;              :preamble ["externs/react/react-0.10.0.js"]
            ;;              :externs ["externs/react/react-0.10.0.js"]}}
             ]
   ;; :test-commands
   ;; { "unit-tests" [ "phantomjs" :runner
   ;;                  "test/phantomjs-shims.js"
   ;;                  "externs/react/react-0.10.0.js"
   ;;                  "externs/soundjs/soundjs-0.5.2.min.js"
   ;;                  "testable.js" ]}
   })
