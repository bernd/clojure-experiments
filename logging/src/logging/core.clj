(ns logging.core
  (:require [clojure.tools.logging :as logger]))

(defn -main []
  (logger/debug "Debuuuug")
  (logger/info "Hello logging world!")
  (logger/warn "Warning")
  (logger/error "Oups!"))
