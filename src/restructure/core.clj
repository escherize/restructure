(ns restructure.core
  (:require [clojure.set :as set]
            [clojure.walk :as w]))

(defn- shallow->m [coll]
  (with-meta
    (if (or (list? coll) (vector? coll))
      (->> coll
           (map-indexed (fn [idx x] [idx x]))
           (into {}))
      coll )
    {::type (type coll)}))

(defn- m-able? [x]
  (when x
    (and
      (not= clojure.lang.MapEntry (type x))
      (or (list? x) (vector? x)))))

(defn ->m [coll]
  (w/prewalk (fn [x] (if (m-able? x) (shallow->m x) x)) coll))

(defn m->list [m-map]
  (map m-map (range 0 (count m-map))))

(defn m->vec [m-map]
  (mapv m-map (range 0 (count m-map))))

(defn shallow<-m [m-map]
  (if-let [type (::type (meta m-map))]
    (let [operator ({clojure.lang.PersistentVector m->vec
                     clojure.lang.PersistentList m->list}
                    type)]
      (operator m-map))
    (throw (ex-info
             (str "m-map called on something without :restructure.core/type metadata: "
                  m-map " metadata: " (meta m-map)) {}))))

(defn <-m [m-map]
  (w/postwalk
    (fn [x] (if (::type (meta x)) (shallow<-m x) x))
    m-map))

(defn- keys-into [m]
  (if (map? m)
    (vec
      (mapcat (fn [[k v]]
                (let [nested (->> (keys-into v)
                                  (filter (comp not empty?))
                                  (map #(into [k] %)))]
                  (if (seq nested) nested [[k]])))
              m))
    []))

(defn paths [m]
  (let [value+paths
        (->> m
             ->m
             keys-into
             (mapv (fn [path]
                     (let [leaf-value (get-in m path)]
                       [leaf-value path]))))
        no-dupe-values? (apply distinct? (map first value+paths))]
    (when (not no-dupe-values?)
      (throw (ex-info
               (str "paths called on m with non-unique leaf-values: "
                    (pr-str (map first value+paths))) {})))
    (into {} value+paths)))

(defn- dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn rs
  "Given two maps in and out, returns f s.t. f(in) = out for all shared keys.
  -----
  Given two maps in out, and value->fxn,
  returns f s.t. f(in) = out with functions applied to the "
  ([in out] (rs in out {}))
  ([in out value->fxn]
   (let [in-paths (paths in)
         out-paths (paths out)]
     (fn [input]
       (let [m-input (->m input)
             final-merge (reduce dissoc-in m-input (vals in-paths))]
         (loop [out-map (->m out)
                common-vals (vec
                              (set/intersection
                                (set (keys in-paths))
                                (set (keys out-paths))))]
           (let [current-val (first common-vals)
                 current-fn (get value->fxn current-val identity)
                 in-path (get in-paths current-val)
                 out-path (get out-paths current-val)]
             (if (nil? common-vals)
               (<-m (merge out-map final-merge))
               (recur
                 (assoc-in out-map out-path
                           (-> m-input (get-in in-path) current-fn))
                 (next common-vals))))))))))

(comment

  ;; user-function will return a function which expects a value like
  (def user-function
    (rs
      {:user_id :rs/id
       :user_info {:user_age_on_date ["_" :rs/age]}}
      ;; and returns a restructured value that matches
      {:id :rs/id :age :rs/age}))

  (user-function
    {:user_id  1910191
     :user_info {:user_age_on_date ["ignore me" 10.5]}})
  ;;=> {:id 1910191, :age 10.5}

  (user-function {:user_id  1910191})
  ;; => {:id 1910191, :age nil}

  (user-function {:user_id  1910191
                  :user_info {:user_age_on_date ["ignore me" 10.5]}
                  :user_title "Person"})
  ;;=> {:id 1910191, :age 10.5, :user_title "Person"}

  ;; if you want to make sure certian keys are there:
  (def safe-user-function
    (rs
      {:user_id :rs/id
       :user_info {:user_age_on_date ["_" :rs/age]}}
      ;; and returns a restructured value that matches
      {:id :rs/id :age :rs/age}
      {:rs/id
       #(when (nil? %)
          (throw
            (ex-info "expected a value for :rs/id" {:value :rs/id})))}))

  (safe-user-function {})
  ;;=>  1. Unhandled clojure.lang.ExceptionInfo
  ;;=>  expected a value for :rs/id
  ;;=>  {:value :rs/id}


  )
