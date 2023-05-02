package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Clipboard;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.layout.*;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.media.*;
import javafx.scene.control.*;

import java.io.*;
import java.util.*;

import com.orangomango.chess.multiplayer.Server;
import com.orangomango.chess.multiplayer.Client;

public class MainApplication extends Application{
	public static final int SQUARE_SIZE = 55;
	private static final int SPACE = 60;
	private static final double WIDTH = SQUARE_SIZE*8;
	private static final double HEIGHT = SPACE*2+SQUARE_SIZE*8;
	private volatile int frames, fps;
	private static final int FPS = 40;
	
	private Board board;
	private Engine engine;
	private String currentSelection;
	private List<String> currentMoves;
	private boolean gameFinished = false;
	private volatile String eval;
	private volatile PieceAnimation animation;
	private Color viewPoint;
	private boolean overTheBoard = false;
	private boolean engineMove = false;
	
	private Map<String, String> hold = new HashMap<>();
	
	private Client client;
	private static Color startColor = Color.WHITE;
	
	public static final Media MOVE_SOUND = new Media(MainApplication.class.getResource("/move.mp3").toExternalForm());
	public static final Media CAPTURE_SOUND = new Media(MainApplication.class.getResource("/capture.mp3").toExternalForm());
	public static final Media CASTLE_SOUND = new Media(MainApplication.class.getResource("/castle.mp3").toExternalForm());
	public static final Media CHECK_SOUND = new Media(MainApplication.class.getResource("/notify.mp3").toExternalForm());
	
	@Override
	public void start(Stage stage){
		Thread counter = new Thread(() -> {
			while (true){
				try {
					this.fps = this.frames;
					this.frames = 0;
					Thread.sleep(1000);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		counter.setDaemon(true);
		counter.start();
		
		this.viewPoint = startColor;

		stage.setTitle("Chess");
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);
		// startpos: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
		this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		this.engine = new Engine();
		
		canvas.setOnMousePressed(e -> {
			if (Server.clients.size() == 1) return;
			if (e.getButton() == MouseButton.PRIMARY){
				String not = getNotation(e);
				int x = (int)(e.getX()/SQUARE_SIZE);
				int y = (int)((e.getY()-SPACE)/SQUARE_SIZE);
				if (this.viewPoint == Color.BLACK){
					x = 7-x;
					y = 7-y;
				}
				if (not != null){
					if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.overTheBoard)) return;
					if (this.currentSelection != null){
						this.animation = new PieceAnimation(this.currentSelection, not, () -> {
							boolean ok = this.board.move(this.currentSelection, not);
							if (this.client != null) this.client.sendMessage(this.currentSelection+" "+not);
							this.currentSelection = null;
							this.currentMoves = null;
							this.animation = null;
							this.gameFinished = this.board.isCheckMate(Color.WHITE) || this.board.isCheckMate(Color.BLACK) || this.board.isDraw();
							if (ok && this.engineMove) makeEngineMove();
						});
						Piece piece = this.board.getBoard()[Board.convertNotation(this.currentSelection)[0]][Board.convertNotation(this.currentSelection)[1]];
						if (this.board.getValidMoves(piece).contains(not)){
							this.animation.start();
						} else {
							this.currentSelection = null;
							this.currentMoves = null;
							this.animation = null;
						}
					} else if (this.board.getBoard()[x][y] != null){
						this.currentSelection = not;
						this.currentMoves = this.board.getValidMoves(this.board.getBoard()[x][y]);
					}
				} else {
					System.out.println(this.board.getFEN());
					System.out.println(this.board.getPGN());
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Settings");
					alert.setHeaderText("Setup game");
					GridPane layout = new GridPane();
					layout.setPadding(new Insets(10, 10, 10, 10));
					layout.setHgap(5);
					layout.setVgap(5);
					TextField sip = new TextField();
					sip.setPromptText("192.168.1.247");
					TextField sport = new TextField();
					sport.setPromptText("1234");
					TextField cip = new TextField();
					cip.setPromptText("192.168.1.247");
					TextField cport = new TextField();
					cport.setPromptText("1234");
					sip.setMaxWidth(120);
					sport.setMaxWidth(80);
					cip.setMaxWidth(120);
					cport.setMaxWidth(80);
					ToggleGroup grp = new ToggleGroup();
					RadioButton w = new RadioButton("White");
					w.setOnAction(ev -> this.viewPoint = Color.WHITE);
					RadioButton b = new RadioButton("Black");
					b.setOnAction(ev -> this.viewPoint = Color.BLACK);
					Button startServer = new Button("Start server");
					startServer.setOnAction(ev -> {
						String ip = sip.getText().equals("") ? "192.168.1.247" : sip.getText();
						int port = sport.getText().equals("") ? 1234 : Integer.parseInt(sport.getText());
						Server server = new Server(ip, port);
						alert.close();
					});
					TextArea data = new TextArea(this.board.getFEN()+"\n\n"+this.board.getPGN());
					Button connect = new Button("Connect");
					connect.setOnAction(ev -> {
						String ip = cip.getText().equals("") ? "192.168.1.247" : cip.getText();
						int port = cport.getText().equals("") ? 1234 : Integer.parseInt(cport.getText());
						this.client = new Client(ip, port, this.viewPoint);
						if (Server.clients.size() == 1){
							reset(data.getText());
						}
						if (!this.client.isConnected()){
							this.client = null;
							return;
						}
						this.viewPoint = this.client.getColor();
						Thread listener = new Thread(() -> {
							while (!this.gameFinished){
								String message = client.getMessage();
								if (message == null){
									System.exit(0);
								} else {
									this.animation = new PieceAnimation(message.split(" ")[0], message.split(" ")[1], () -> {
										this.board.move(message.split(" ")[0], message.split(" ")[1]);
										this.animation = null;
										this.gameFinished = this.board.isCheckMate(Color.WHITE) || this.board.isCheckMate(Color.BLACK) || this.board.isDraw();
									});
									this.animation.start();
								}
							}
						});
						listener.setDaemon(true);
						listener.start();
						alert.close();
					});
					CheckBox otb = new CheckBox("Over the board");
					CheckBox eng = new CheckBox("Play against stockfish");
					eng.setSelected(this.engineMove);
					eng.setOnAction(ev -> {
						this.overTheBoard = true;
						otb.setSelected(true);
						this.engineMove = eng.isSelected();
						otb.setDisable(this.engineMove);
						if (this.engineMove && this.board.getPlayer() != this.viewPoint){
							makeEngineMove();
							this.board.playerA = "stockfish";
							this.board.playerB = System.getProperty("user.name");
						} else {
							this.board.playerA = System.getProperty("user.name");
							this.board.playerB = "stockfish";
						}
					});
					otb.setSelected(this.overTheBoard);
					otb.setOnAction(ev -> {
						this.overTheBoard = otb.isSelected();
						if (this.viewPoint == Color.WHITE){
							this.board.playerA = System.getProperty("user.name");
							this.board.playerB = "BLACK";
						} else {
							this.board.playerA = "WHITE";
							this.board.playerB = System.getProperty("user.name");
						}
					});
					data.setMaxWidth(WIDTH*0.7);
					w.setToggleGroup(grp);
					w.setSelected(this.viewPoint == Color.WHITE);
					b.setSelected(this.viewPoint == Color.BLACK);
					b.setToggleGroup(grp);
					Button reset = new Button("Reset board");
					Button startEngine = new Button("Start engine thread");
					Button copy = new Button("Copy");
					copy.setOnAction(ev -> {
						ClipboardContent cc = new ClipboardContent();
						cc.putString(data.getText());
						Clipboard cb = Clipboard.getSystemClipboard();
						cb.setContent(cc);
					});
					startEngine.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || !this.engine.isRunning());
					startEngine.setOnAction(ev -> {
						this.overTheBoard = true;
						new Thread(() -> {
							try {
								Thread.sleep(2000);
								while (!this.gameFinished){
									makeEngineMove();
									Thread.sleep(1000);
								}
							} catch (InterruptedException ex){
								ex.printStackTrace();
							}
						}).start();
						alert.close();
					});
					reset.setOnAction(ev -> {
						String text = data.getText();
						reset(text);
						alert.close();
					});
					HBox serverInfo = new HBox(5, sip, sport, startServer);
					HBox clientInfo = new HBox(5, cip, cport, connect);
					HBox whiteBlack = new HBox(5, w, b);
					HBox rs = new HBox(5, reset, startEngine);
					serverInfo.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					clientInfo.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					whiteBlack.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					rs.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					eng.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || !this.engine.isRunning());
					otb.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || this.client != null);
					layout.add(serverInfo, 0, 0);
					layout.add(clientInfo, 0, 1);
					layout.add(eng, 0, 2);
					layout.add(otb, 0, 3);
					layout.add(whiteBlack, 0, 4);
					layout.add(rs, 0, 5);
					layout.add(copy, 0, 6);
					layout.add(data, 0, 7);
					alert.getDialogPane().setContent(layout);
					alert.showAndWait();
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				String h = getNotation(e);
				if (h != null){
					this.hold.put(h, null);
				}
			} else if (e.getButton() == MouseButton.MIDDLE){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.overTheBoard)) return;
				makeEngineMove();
			}
			
		});
		
		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			String f = null;
			for (Map.Entry<String, String> entry : this.hold.entrySet()){
				if (entry.getValue() == null) f = entry.getKey();
			}
			if (f != null){
				if (f.equals(h)){
					this.hold.clear();
				} else {
					this.hold.put(f, h);
				}
			}
		});
		
		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		AnimationTimer timer = new AnimationTimer(){
			@Override
			public void handle(long time){
				MainApplication.this.frames++;
			}
		};
		timer.start();
		
		Thread evalCalculator = new Thread(() -> {
			Engine eng = new Engine();
			try {
				while (eng.isRunning()){
					this.eval = eng.getEval(this.board.getFEN());
					Thread.sleep(100);
				}
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		});
		evalCalculator.setDaemon(true);
		evalCalculator.start();
		
		stage.setResizable(false);
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}
	
	private String getNotation(MouseEvent e){
		if (e.getY() < SPACE) return null;
		int x = (int)(e.getX()/SQUARE_SIZE);
		int y = (int)((e.getY()-SPACE)/SQUARE_SIZE);
		if (this.viewPoint == Color.BLACK){
			x = 7-x;
			y = 7-y;
		}
		return Board.convertPosition(x, y);
	}
	
	private void reset(String text){
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		if (text.startsWith("CUSTOM\n") && (this.board.getMovesN() == 1 || this.gameFinished)){
			fen = text.split("\n")[1];
		}
		this.board = new Board(fen);
		this.gameFinished = false;
	}
	
	private void makeEngineMove(){
		if (this.gameFinished) return;
		new Thread(() -> {
			String output = this.engine.getBestMove(board.getFEN(), 500);
			if (output != null){
				this.animation = new PieceAnimation(output.split(" ")[0], output.split(" ")[1], () -> {
					this.board.move(output.split(" ")[0], output.split(" ")[1]);
					if (this.client != null) this.client.sendMessage(output.split(" ")[0]+" "+output.split(" ")[1]);
					this.animation = null;
					this.gameFinished = this.board.isCheckMate(Color.WHITE) || this.board.isCheckMate(Color.BLACK) || this.board.isDraw();
				});
				this.animation.start();
			}
		}).start();
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.CYAN);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.save();
		gc.translate(0, SPACE);
		Piece[][] pieces = this.board.getBoard();
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				gc.setFill((i+7*j) % 2 == 0 ? Color.WHITE : Color.GREEN);
				if (this.currentSelection != null){
					int[] pos = Board.convertNotation(this.currentSelection);
					if (i == (this.viewPoint == Color.BLACK ? 7-pos[0] : pos[0]) && j == (this.viewPoint == Color.BLACK ? 7-pos[1] : pos[1])){
						gc.setFill(Color.RED);
					}
				}
				gc.fillRect(i*SQUARE_SIZE, j*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
			}
		}
		
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				Piece piece = pieces[i][j];
				Point2D pos = new Point2D(i, j);
				if (this.animation != null && this.animation.getStartNotation().equals(Board.convertPosition(i, j))){
					pos = this.animation.getPosition();
				}
				if (this.viewPoint == Color.BLACK){
					pos = new Point2D(7-pos.getX(), 7-pos.getY());
				}
				if (piece != null){
					if (piece.getType().getName() == Piece.PIECE_KING){
						if ((piece.getColor() == Color.WHITE && this.board.getCheckingPieces(Color.WHITE).size() > 0) || (piece.getColor() == Color.BLACK && this.board.getCheckingPieces(Color.BLACK).size() > 0)){
							gc.setFill(Color.BLUE);
							gc.fillOval(pos.getX()*SQUARE_SIZE, pos.getY()*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
						}
					}
					gc.drawImage(piece.getImage(), pos.getX()*SQUARE_SIZE, pos.getY()*SQUARE_SIZE);
				}
			}
		}
		
		if (this.currentMoves != null){
			for (String move : this.currentMoves){
				int[] pos = Board.convertNotation(move);
				gc.setFill(this.board.getBoard()[pos[0]][pos[1]] == null ? Color.YELLOW : Color.BLUE);
				gc.fillOval((this.viewPoint == Color.BLACK ? 7-pos[0] : pos[0])*SQUARE_SIZE+SQUARE_SIZE/3.0, (this.viewPoint == Color.BLACK ? 7-pos[1] : pos[1])*SQUARE_SIZE+SQUARE_SIZE/3.0, SQUARE_SIZE/3.0, SQUARE_SIZE/3.0);
			}
		}
		
		//gc.setFill(Color.RED);
		//gc.setFont(new Font("Sans-serif", 15));
		//gc.fillText(String.format("FPS: %d\n%s", fps, this.board.getBoardInfo()), 30, 230);
		gc.restore();
		
		gc.setFill(Color.BLACK);
		int bm = this.board.getMaterial(Color.BLACK);
		int wm = this.board.getMaterial(Color.WHITE);
		int diff = wm-bm;
		if (diff < 0) gc.fillText(Integer.toString(-diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? SPACE/2.0 : HEIGHT-SPACE*0.6);
		if (diff > 0) gc.fillText(Integer.toString(diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? HEIGHT-SPACE*0.6 : SPACE/2.0);
		
		List<Piece> black = this.board.getMaterialList(Color.BLACK);
		List<Piece> white = this.board.getMaterialList(Color.WHITE);
		gc.save();
		for (int i = 0; i < black.size(); i++){
			Piece piece = black.get(i);
			Piece prev = i == 0 ? null : black.get(i-1);
			gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.05, this.viewPoint == Color.WHITE ? SPACE/2.0 : HEIGHT-SPACE*0.6, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
		}
		gc.restore();
		gc.save();
		for (int i = 0; i < white.size(); i++){
			Piece piece = white.get(i);
			Piece prev = i == 0 ? null : white.get(i-1);
			gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.05, this.viewPoint == Color.WHITE ? HEIGHT-SPACE*0.6 : SPACE/2.0, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
		}
		gc.restore();

		for (Map.Entry<String, String> entry : this.hold.entrySet()){
			if (entry.getKey() == null || entry.getValue() == null) continue;
			int[] h1 = Board.convertNotation(entry.getKey());
			int[] h2 = Board.convertNotation(entry.getValue());
			
			gc.save();
			gc.setLineWidth(9);
			gc.setGlobalAlpha(0.6);
			gc.setStroke(Color.ORANGE);
			double rad;
			
			// Knight
			if (Math.abs(h2[0]-h1[0])*Math.abs(h2[1]-h1[1]) == 2){
				gc.strokeLine(h1[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h1[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0, h1[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0);
				gc.strokeLine(h1[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0, h2[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0);
				rad = Math.atan2(0, h2[0]-h1[0]);
			} else {
				gc.strokeLine(h1[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h1[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0, h2[0]*SQUARE_SIZE+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE/2.0);
				rad = Math.atan2(h2[1]-h1[1], h2[0]-h1[0]);
			}
			
			gc.setFill(Color.ORANGE);
			gc.translate(h2[0]*SQUARE_SIZE+SQUARE_SIZE*0.5, h2[1]*SQUARE_SIZE+SPACE+SQUARE_SIZE*0.5);
			gc.rotate(Math.toDegrees(rad));
			gc.fillPolygon(new double[]{-SQUARE_SIZE*0.3, -SQUARE_SIZE*0.3, SQUARE_SIZE*0.3}, new double[]{-SQUARE_SIZE*0.3, SQUARE_SIZE*0.3, 0}, 3);
			
			gc.restore();
		}
		
		gc.fillText("Eval: "+this.eval, WIDTH*0.7, HEIGHT-SPACE*0.7);
		
		if (this.gameFinished || Server.clients.size() == 1){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
			if (this.gameFinished) this.client = null;
		}
	}
	
	public static void playSound(Media media){
		AudioClip player = new AudioClip(media.getSource());
		player.play();
	}
	
	public static void main(String[] args) throws IOException{
		if (args.length >= 1){
			String col = args[0];
			if (col.equals("WHITE")){
				startColor = Color.WHITE;
			} else {
				startColor = Color.BLACK;
			}
		}
		
		launch(args);
		
		/*Board board = new Board();
		System.out.println(board);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		do {
			System.out.print("Move: ");
			line = reader.readLine();
			if (!line.equals("")){
				board.move(line.split(" ")[0], line.split(" ")[1]);
				System.out.println("---");
				System.out.println(board);
			}
		} while (!line.equals(""));
		reader.close();*/
		
		//Engine engine = new Engine();
		//engine.writeCommand("d");
		//System.out.println(engine.getBestMove(board.getFEN(), 100));
		
		//System.exit(0);
	}
}
