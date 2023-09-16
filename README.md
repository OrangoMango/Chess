# Chess
Chess GUI made in JavaFX for desktop, browser and android.
## Web-version
Try it now at https://orangomango.itch.io/chess
# Stockfish
* **Android**: stockfish is already included in the apk
* **Desktop**: stockfish needs to be installed separately and must be accessed with the stockfish command or place the executable (the filename must start with stockfish in order to be found by the application) in the .omchess folder loacated in your home directory.
* **Browser**: thanks to the stockfish.online API, it works also in the browser without installing anything.
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

# Screenshots
![image](https://user-images.githubusercontent.com/61402409/236507038-099f9154-9668-4c95-9361-5d5b156c91c6.png)
![image](https://user-images.githubusercontent.com/61402409/234310007-e4e514eb-2837-442c-b9d7-db9cb75bd50d.png)
![Screenshot from 2023-04-25 16-05-41](https://user-images.githubusercontent.com/61402409/234302473-74633016-9f7b-476e-b104-803e29a8b10f.png)
![Screenshot from 2023-05-02 19-32-18](https://user-images.githubusercontent.com/61402409/235984400-e4388a1f-e218-45c8-bb29-7cf63d9be09b.png)
