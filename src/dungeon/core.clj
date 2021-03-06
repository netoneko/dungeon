(ns dungeon.core
  (import java.nio.charset.Charset
    com.googlecode.lanterna.TerminalFacade
    com.googlecode.lanterna.terminal.Terminal
    com.googlecode.lanterna.terminal.Terminal$Color
    com.googlecode.lanterna.screen.ScreenCharacterStyle
    com.googlecode.lanterna.input.Key$Kind)
  (:gen-class ))

(def terminal (TerminalFacade/createTerminal System/in System/out (Charset/forName "UTF8")))

(def screen (TerminalFacade/createScreen terminal))

(def print-return
  (fn [x]
    (println x)
    x))

(def shift-coords
  (fn [coords direction]
    (let [x (first coords)
          y (last coords)]
      (case direction
        :down (list x (+ y 1))
        :up (list x (- y 1))
        :left (list (- x 1) y)
        :right (list (+ x 1) y)))))

(def flip-direction
  (fn [direction]
    (case direction
      :down :up, :up :down, :left :right, :right :left )))

(def clear-region
  (fn [coords]
    (.putString screen (first coords) (last coords) " " Terminal$Color/BLACK Terminal$Color/BLACK #{})))

(def draw-hero
  (fn [hero]
    (clear-region (shift-coords (hero :coords ) (flip-direction (hero :direction ))))
    (.putString screen (first (hero :coords )) (last (hero :coords )) (hero :icon ) (hero :color ) (hero :background-color )
      (hero :screen-settings ))
    hero))

(def draw-fireball
  (fn [coords direction]
    (let [coords (shift-coords coords direction)]
      (.putString screen (first coords) (last coords) "*" Terminal$Color/RED Terminal$Color/BLACK #{ScreenCharacterStyle/Bold}))
    (.refresh screen)))

(def read-input-loop
  (fn []
    (Thread/sleep 20)
    (let [key (.readInput terminal)]
      (if (nil? key) (recur) key))))

(def merge-hero
  (fn [hero args]
    (merge hero {:coords (first args) :direction (second args)})))

(def merge-world
  (fn [world enemies old-coords hero]
    (let [world-map (assoc (dissoc (world :map ) old-coords) (hero :coords ) hero)
          filtered-enemies (print-return (filter #(> (.indexOf (vec (vals world-map)) %1) -1) enemies))
          heroes (vec (flatten [hero filtered-enemies]))]
      (merge world {:map world-map
                    :heroes heroes}))))

(def human-turn
  (fn [world hero enemies]
    (println "HUMAN TURN!!!")
    (let [coords (hero :coords )
          direction (hero :direction )
          key (read-input-loop)
          kind (str (.getKind key))
          value (str (.getCharacter key))
          new-hero (merge-hero hero (case kind
                                      "ArrowDown" (list (shift-coords coords :down ) :down )
                                      "ArrowUp" (list (shift-coords coords :up ) :up )
                                      "ArrowLeft" (list (shift-coords coords :left ) :left )
                                      "ArrowRight" (list (shift-coords coords :right ) :right )
                                      "NormalKey" (do
                                                    (case value
                                                      "f" (draw-fireball coords direction)
                                                      (println "I don't know about this hotkey"))
                                                    (list coords direction))
                                      (do
                                        (println "Unrecognizable input")
                                        (list coords direction))))]
      (list (merge-world world enemies (hero :coords ) new-hero) new-hero))))

(def distance
  "Euclidean distance between 2 points"
  (fn [[x1 y1] [x2 y2]]
    (Math/sqrt
      (+ (Math/pow (- x1 x2) 2)
        (Math/pow (- y1 y2) 2)))))

(def abs
  (fn [x]
    (if (neg? x) (- x) x)))

(def direction-to-target
  (fn [[x1 y1] [x2 y2]]
    (let [horizontal (- x1 x2)
          vertical (- y1 y2)]
      ((if (> (abs horizontal) (abs vertical)) first second)
        (list (if (> horizontal 0) :right :left )
          (if (> vertical 0) :down :up ))))))

(def hero-icon
  (fn [hero]
    (hero :icon )))

(def observe
  (fn [world hero enemies]
    (println (str "His enemies are " (clojure.string/join " and " (map hero-icon enemies))))
    (last (first (sort #(- (first %1) (first %2))
                   (map #(list (distance (%1 :coords ) (hero :coords )) %1) enemies))))))

(def ai-turn
  (fn [world hero enemies]
    (print (str "===== " (hero-icon hero) " turn! "))
    (let [target (observe world hero enemies)
          direction (direction-to-target (target :coords ) (hero :coords ))
          new-hero (merge-hero hero
                     (list (shift-coords (hero :coords ) direction) direction))]
      (println (str (new-hero :icon ) "'s target is " (target :icon )))
      (list (merge-world world enemies (hero :coords ) new-hero) new-hero))))

(def turn
  (fn [world]
    (let [hero (peek (world :heroes ))
          enemies (pop (world :heroes ))
          results (if (= (hero :status ) :human )
                    (human-turn world hero enemies)
                    (ai-turn world hero enemies))
          world (first results)
          hero (second results)]
      (draw-hero hero)
      (.refresh screen)
      world)))

(def notice
  (fn [text]
    (.putString screen 1 0 text Terminal$Color/RED Terminal$Color/BLACK #{ScreenCharacterStyle/Bold})))

(def game-loop
  (fn [world]
    (if (> (count (world :heroes )) 1)
      (recur (turn world))
      (println world))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (.startScreen screen)
  (notice "You are in the dungeon. Move with arrows, throw fireballs with 'f'. Press 'space' to start.")
  (.refresh screen)

  (def hero {:coords (list 10 15), :direction :right, :icon "@", :status :human,
             :color Terminal$Color/YELLOW, :background-color Terminal$Color/BLACK
             :screen-settings #{ScreenCharacterStyle/Bold}})
  (def orc {:coords (list 20 20), :direction :right, :icon "B", :status :ai
            :color Terminal$Color/RED, :background-color Terminal$Color/BLACK
            :screen-settings #{ScreenCharacterStyle/Bold}})
  (def troll {:coords (list 15 25), :direction :right, :icon "T", :status :ai
              :color Terminal$Color/GREEN, :background-color Terminal$Color/BLACK
              :screen-settings #{ScreenCharacterStyle/Bold}})

  (def heroes [hero orc troll])

  (def world {:heroes heroes :map (reduce #(assoc %1 (%2 :coords ) %2) {} heroes)})

  (game-loop world))
