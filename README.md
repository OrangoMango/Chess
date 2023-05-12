# Chess
Chess GUI made in JavaFX. To change the game settings, click twice on the background.  
*Under development, this application is currently in beta*
# Itch
https://orangomango.itch.io/chess
# Stockfish
Install stockfish and add it as an environment variable or place the executable (the filename must start with `stockfish` in order to be found by the application) in the `.omchess` folder loacated in your home directory.
# Run
Java 17+ required, `./gradlew :run`
## Features
* Default chess rules (Capture, movement, en passant, castle, promotion)
* Play on the same board or **in LAN**
* Play agains stockfish (be sure to have `stockfish` installed)
* Watch stockfish vs stockfish
* Custom FEN (The first line of the text area must be `CUSTOM` and then click `reset board`)
* Export game FEN and PGN
* Arrows
* Premoves
* Time control (write `<seconds>+<increment>` in the text field and then click reset board)

# Screenshots
![image](https://user-images.githubusercontent.com/61402409/236507038-099f9154-9668-4c95-9361-5d5b156c91c6.png)
![image](https://user-images.githubusercontent.com/61402409/234310007-e4e514eb-2837-442c-b9d7-db9cb75bd50d.png)
![Screenshot from 2023-04-25 16-05-41](https://user-images.githubusercontent.com/61402409/234302473-74633016-9f7b-476e-b104-803e29a8b10f.png)
![Screenshot from 2023-05-02 19-32-18](https://user-images.githubusercontent.com/61402409/235984400-e4388a1f-e218-45c8-bb29-7cf63d9be09b.png)
