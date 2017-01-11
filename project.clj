(def tk-version "1.1.1")
(def ks-version "1.0.0")

(defproject puppetlabs/trapperkeeper-scheduler "0.1.0"
  :description "Trapperkeeper Scheduler Service"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [puppetlabs/trapperkeeper ~tk-version]
                 [puppetlabs/kitchensink ~ks-version]
                 [prismatic/schema "0.4.0"]
                 [overtone/at-at "1.2.0"]]

  :pedantic? :abort

  :test-paths ["test/unit" "test/integration"]

  :test-selectors {:integration :integration
                   :unit (complement :integration)}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [puppetlabs/trapperkeeper ~tk-version :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink ~ks-version :classifier "test" :scope "test"]
                                  [spyscope "0.1.4" :exclusions [clj-time]]]
                   :injections [(require 'spyscope.core)]}}

  :repl-options {:init-ns user})
