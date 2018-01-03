# restructure

A Clojure library designed to declaritively reshape data.

[![Clojars Project](https://img.shields.io/clojars/v/restructure.svg)](https://clojars.org/restructure)

        _H_              _H_               _H_                  o88o.
      .=|_|===========v==|_|============v==|_|===========.    (8%8898),
     /                |                 |                 \ ,(8888%8688)
    /_________________|_________________|__________________(898%88688HJW)
    |=|_|_|_|  =|_|_|=|X|)^^^(|X|=|/ \|=||_|_|_|=| ||_|_|=|`(86888%8%9b)
    |=|_|_|_|== |_|_|=|X|\___/|X|=||_||=||_____|=|_||_|_|=|___(88%%8888)
    |=_________= ,-. =|""""""""""="""""=|=_________== == =|_______\//`'
    |=|__|__|_| //O\\=|X|"""""|X|=//"\\=|=|_|_|_|_| .---.=|.=====.||
    |=|__|__|_|=|| ||=|X|_____|X|=|| ||=|=|_______|=||"||=||=====|||
    |___d%8b____||_||_|=_________=||_||_|__d8%o%8b_=|j_j|=|j==o==j|\---
## Usage

I often find myself wondering what shape of data is expected by a certain piece of code.

This library sprang forth from those thoughts.

Consider an api that will return to us information about a user. Of course, the information is presented to us in a manner that makes sense for the producer of the data rather than our purpoises. We want to =restructure= the data so that it makes senses for our usecases.

Using *restructure* we will define a function `user-function` which will reshape our data the way we want it.

``` clojure
(def user-function
  (rs
  ;; user-function expects a value like
    {:user_id :rs/id
     :user_info {:user_age_on_date ["_" :rs/age]}}
    ;; and returns a restructured value that matches
    {:id :rs/id :age :rs/age}))
```

Notice that the exact shape of the incoming data:

```clojure
(user-function
  {:user_id  1910191
   :user_info {:user_age_on_date ["ignore me" 10.5]}})
;;=> {:id 1910191, :age 10.5}
```

If the input is missing a particular key, it will become nil:

``` clojure
(user-function {:user_id  1910191})
;; => {:id 1910191, :age nil}
```

Other included keys are left alone:

```clojure
(user-function {:user_id  1910191
                :user_info {:user_age_on_date ["ignore me" 10.5]}
                :user_title "Person"})
;;=> {:id 1910191, :age 10.5, :user_title "Person"}
```

if you want to ensure the existance of certian keys:

```clojure
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
```

### Notes

Note that I used keywords with the rs namespace above. That's actually not nessicary to do.

We can also use _sample data_ to show exactly what we expect to recieve.

``` clojure
(rs {:user_info {:u_id 12345 :u_first_name "Bryan"}
    {:name "Bryan" :id 12345})
```

The function returned by the prior call to rs is identical to this:
``` clojure
(rs {:user_info {:u_id 0 :u_first_name 1}
    {:name 1 :id 0})
```

and
``` clojure
(rs {:user_info {:u_id ::uid :u_first_name ::fn}
    {:name ::fn :id ::uid})
```

So the user has the flexibility to show what they mean, which is a noble principle to uphold.

## License

Copyright © 2018 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
