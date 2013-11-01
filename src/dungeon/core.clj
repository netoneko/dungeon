(ns dungeon.core
  (import java.nio.charset.Charset
          com.googlecode.lanterna.TerminalFacade
          com.googlecode.lanterna.terminal.Terminal
          com.googlecode.lanterna.terminal.Terminal$Color)
  (:gen-class))

(def terminal (TerminalFacade/createTerminal System/in System/out (Charset/forName "UTF8")))

(def screen (TerminalFacade/createScreen terminal))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))


  (.startScreen screen)
  (.putString screen 10 5 "Welcome to the jungle!" Terminal$Color/WHITE Terminal$Color/BLACK #{})
  (.refresh screen))
