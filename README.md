# Chess
Chess GUI made in JavaFX for desktop, browser and android.
## Web-version
Try it now at https://orangomango.itch.io/chess
# Stockfish
* **Android**: stockfish is already included in the apk
* **Desktop**: stockfish needs to be installed separately and must be accessed with the stockfish command or place the executable (the filename must start with stockfish in order to be found by the application) in the .omchess folder loacated in your home directory.
* **Browser**: thanks to the [stockfish.online](https://stockfish.online) API, it works also in the browser without installing anything.
# Build & Run
Java 17+ required, `./gradlew :run`
# Features
* Default chess rules (Capture, movement, en passant, castle, promotion)
* Play on the same board (**Pass & play**), in **LAN** or in a **server room**
* Play against stockfish (be sure to have stockfish installed)
* Custom FEN (Click on the edit board button)
* Export game FEN or PGN once the game ended.
* Arrow
* Premoves
* Custom time control
* Drag and drop
* Underpromotion by dragging a piece in the opposite direction

# Screenshot
![Screenshot from 2023-09-16 16-39-26](https://github.com/OrangoMango/Chess/assets/61402409/739a1668-851d-4cfb-99d6-6347a269f4f5)
