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

(def draw-char
  (fn [x y]
    (.putString screen x y "@" Terminal$Color/GREEN Terminal$Color/BLACK #{ScreenCharacterStyle/Bold
                                                                            ScreenCharacterStyle/Blinking})))

(def read-input-loop
  (fn []
    (Thread/sleep 20)
    (let [key (.readInput terminal)]
      (if (nil? key) (recur) key))))

(def walk
  (fn [x y]
    (.clear screen)
    (draw-char x y)
    (.refresh screen)

    (let [key (read-input-loop)
          kind (str (.getKind key))]

      (case kind
        "ArrowDown" (recur x (+ y 1))
        "ArrowUp" (recur x (- y 1))
        "ArrowLeft" (recur (- x 1) y)
        "ArrowRight" (recur(+ x 1) y)
        (do
          (println "Unrecognizable input")
          (recur x y))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (.startScreen screen)
  (walk 10 15))
