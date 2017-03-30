(defproject puppetlabs/trapperkeeper-scheduler "0.1.1-SNAPSHOT"
  :description "Trapperkeeper Scheduler Service"

  :dependencies [[org.clojure/clojure]
                 [puppetlabs/trapperkeeper]
                 [puppetlabs/i18n]
                 [overtone/at-at "1.2.0"]]

  :min-lein-version "2.7.1"

  :parent-project {:coords [puppetlabs/clj-parent "0.6.1"]
                   :inherit [:managed-dependencies]}

  :pedantic? :abort

  :test-paths ["test/unit" "test/integration"]

  :test-selectors {:integration :integration
                   :unit (complement :integration)}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[puppetlabs/trapperkeeper :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink :classifier "test" :scope "test"]]}}

  :plugins  [[lein-parent "0.3.1"]
             [puppetlabs/i18n "0.8.0"]]

  :repl-options {:init-ns user})
