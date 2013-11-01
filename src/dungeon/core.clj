(ns dungeon.core
  (import java.nio.charset.Charset
          com.googlecode.lanterna.TerminalFacade
          com.googlecode.lanterna.terminal.Terminal
          com.googlecode.lanterna.terminal.Terminal$Color
          com.googlecode.lanterna.screen.ScreenCharacterStyle
          com.googlecode.lanterna.input.Key$Kind)
  (:gen-class))

(def terminal (TerminalFacade/createTerminal System/in System/out (Charset/forName "UTF8")))

(def screen (TerminalFacade/createScreen terminal))

(def shift-coords
  (fn [x y direction]
    (case direction
      :down (list x (+ y 1))
      :up (list x (- y 1))
      :left (list (- x 1) y)
      :right (list (+ x 1) y))))

(def flip-direction
  (fn [direction]
    (case direction
      :down :up
      :up :down
      :left :right
      :right :left)))

(def clear-region
  (fn [coords]
    (.putString screen (first coords) (last coords) " " Terminal$Color/BLACK Terminal$Color/BLACK #{})))

(def draw-char
  (fn [x y direction]
    (clear-region (shift-coords x y (flip-direction direction)))
    (.putString screen x y "@" Terminal$Color/GREEN Terminal$Color/BLACK #{ScreenCharacterStyle/Bold
                                                                            ScreenCharacterStyle/Blinking})))

(def draw-fireball
  (fn [x y direction]
    (let [coords (shift-coords x y direction)]
      (.putString screen (first coords) (last coords) "*" Terminal$Color/RED Terminal$Color/BLACK #{ScreenCharacterStyle/Bold}))
    (.refresh screen)))

(def read-input-loop
  (fn []
    (Thread/sleep 20)
    (let [key (.readInput terminal)]
      (if (nil? key) (recur) key))))

(def walk
  (fn [x y direction]
    (draw-char x y direction)
    (.refresh screen)

    (let [key (read-input-loop)
          kind (str (.getKind key))
          value (str (.getCharacter key))]

      (case kind
        "ArrowDown" (recur x (+ y 1) :down)
        "ArrowUp" (recur x (- y 1) :up)
        "ArrowLeft" (recur (- x 1) y :left)
        "ArrowRight" (recur(+ x 1) y :right)
        "NormalKey" (do
                      (case value
                        "f" (draw-fireball x y direction)
                        (println "I don't know about this hotkey"))
                        (recur x y direction))
        (do
          (println "Unrecognizable input")
          (recur x y direction))))))

(def notice
  (fn [text]
    (.putString screen 1 0 text Terminal$Color/RED Terminal$Color/BLACK #{ScreenCharacterStyle/Bold})))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (.startScreen screen)
  (notice "You are in the dungeon. Move with arrows, throw fireballs with 'f'.")
  (walk 10 15 :right))
