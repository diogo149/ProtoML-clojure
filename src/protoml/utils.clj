(ns protoml.utils
  (:use compojure.core
        [clojure.tools.logging :only (info error)]
        [clojure.stacktrace :as stacktrace]))

;unused
(defmacro get-var-name [x]
  `(:name (meta (var ~x))))

;unused
(defmacro name-and-value [x]
  `(str (:name (meta (var ~x))) ": " ~x))

; https://github.com/amalloy/amalloy-utils/blob/master/src/amalloy/utils/debug.clj
(defmacro ?
  "A useful debugging tool when you can't figure out what's going on:
  wrap a form with ?, and the form will be printed alongside
  its result. The result will still be passed along."
  [val]
  `(let [x# ~val]
     (prn '~val '~'is x#)
     x#))

(defmacro info?
  "A useful debugging tool when you can't figure out what's going on: wrap a form with ?, and the form will be printed with debug level info alongside its result. The result will still be passed along."
  [val]
  `(let [x# ~val]
     (info '~val '~'is x#)
     x#))

; http://adambard.com/blog/acceptable-error-handling-in-clojure/
(defn bind-error [f [val err]]
  "Applies the function on a value if err is nil, or returns err with nil. Roughly equivalent to the Either monad"
  (if (nil? err)
    (f val)
    [val err]))

(defmacro err->> [val & fns]
  "function composition for bind-error"
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))

(defn log-bind-error [f [val err]]
  "Applies the function on a value if err is nil, or returns err with nil. Roughly equivalent to the Either monad"
  (if (nil? err)
    (let [new-val (f val)]
      (info new-val)
      new-val)
    [val err]))

(defmacro log-err->> [val & fns]
  "function composition for bind-error, with logging"
  (let [fns (for [f fns] `(log-bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))

(defn exception-to-error [f & arguments]
  "calls function with input arguments, catching exceptions and returning in the form [val error]"
  (try [(apply f arguments) nil]
    (catch Throwable e
      (let [st (with-out-str (stacktrace/print-stack-trace e))]
        (error st)
        [nil (.getMessage e)]))))

(defn combine-errors [value & results]
  "takes in a collection of [val error] pairs and returns a default value if there are no errors, else returns an error"
  (let [errors (map second results)]
    (if (every? nil? errors) [value nil]
      [nil (->> errors
                (filter #(not (nil? %)))
                (interpose ",")
                (apply str))])))

(defn debug-request [request]
  "shows debug information for a request"
  (doall (for [[k v] request]
           (do (println "Key: " k)
             (println "Key Class: " (class k))
             (println "Value: " v)
             (println "Value Class: " (class v))
             (println))))
  (flush)
  [request nil])
