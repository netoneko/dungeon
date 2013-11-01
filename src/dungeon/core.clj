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

(def human-turn
  (fn [hero]
    (let [coords (hero :coords )
          direction (hero :direction )
          key (read-input-loop)
          kind (str (.getKind key))
          value (str (.getCharacter key))]

      (merge-hero hero (case kind
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
                           (list coords direction)))))))

(def ai-turn
  (fn [hero]
    (def direction (rand-nth (list :up :down :left :right )))
    (merge-hero hero (list (shift-coords (hero :coords ) direction) direction))))

(def walk
  (fn [hero]
    (let [hero (if (= (hero :status ) :human )
                 (human-turn hero)
                 (ai-turn hero))]
      (draw-hero hero)
      (.refresh screen)
      hero)))

(def notice
  (fn [text]
    (.putString screen 1 0 text Terminal$Color/RED Terminal$Color/BLACK #{ScreenCharacterStyle/Bold})))

(def turn
  (fn [hero]
    (walk hero)))

(def game-loop
  (fn [hero enemies]
    (let [hero (turn hero)
          enemies (map turn enemies)]
      (println hero)
      (println enemies)
      (recur hero enemies))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (.startScreen screen)
  (notice "You are in the dungeon. Move with arrows, throw fireballs with 'f'.")

  (def hero {:coords (list 10 15), :direction :right, :icon "@", :status :human,
             :color Terminal$Color/YELLOW, :background-color Terminal$Color/BLACK
             :screen-settings #{ScreenCharacterStyle/Bold}})
  (def orc {:coords (list 20 20), :direction :right, :icon "B", :status :ai
            :color Terminal$Color/GREEN, :background-color Terminal$Color/BLACK
            :screen-settings #{ScreenCharacterStyle/Bold}})

  (draw-hero hero)
  (draw-hero orc)
  (.refresh screen)

  (game-loop hero (list orc)))
