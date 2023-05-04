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
import java.text.SimpleDateFormat;

import com.orangomango.chess.multiplayer.Server;
import com.orangomango.chess.multiplayer.Client;

public class MainApplication extends Application{
	public static final int SQUARE_SIZE = 55;
	private static final int SPACE = 130;
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
	
	private Map<String, List<String>> hold = new HashMap<>();
	private String currentHold;
	private String moveStart, moveEnd;
	private List<Premove> premoves = new ArrayList<>();
	
	private Client client;
	private static Color startColor = Color.WHITE;
	
	public static final Media MOVE_SOUND = new Media(MainApplication.class.getResource("/move.mp3").toExternalForm());
	public static final Media CAPTURE_SOUND = new Media(MainApplication.class.getResource("/capture.mp3").toExternalForm());
	public static final Media CASTLE_SOUND = new Media(MainApplication.class.getResource("/castle.mp3").toExternalForm());
	public static final Media CHECK_SOUND = new Media(MainApplication.class.getResource("/notify.mp3").toExternalForm());
	
	private static class Premove{
		public String startPos, endPos;
		
		public Premove(String s, String e){
			this.startPos = s;
			this.endPos = e;
		}
	}
	
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
		this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 3*60*1000);
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
					if (this.gameFinished){
						return;
					} else if (this.board.getPlayer() != this.viewPoint && (this.engineMove || !this.overTheBoard)){
						if (this.currentSelection == null){
							if (this.board.getBoard()[x][y] == null && !getPremoves().contains(not)){
								this.premoves.clear();
							} else if (this.board.getBoard()[x][y].getColor() == this.viewPoint){
								this.currentSelection = not;
							}
						} else {
							this.premoves.add(new Premove(this.currentSelection, not));
							this.currentSelection = null;
						}
					} else {
						boolean showMoves = false;
						if (this.currentSelection != null){
							this.animation = new PieceAnimation(this.currentSelection, not, () -> {
								boolean ok = this.board.move(this.currentSelection, not);
								if (this.client != null) this.client.sendMessage(this.currentSelection+" "+not);
								this.moveStart = this.currentSelection;
								this.moveEnd = not;
								this.currentSelection = null;
								this.currentMoves = null;
								this.hold.clear();
								this.animation = null;
								this.gameFinished = this.board.isGameFinished();
								if (ok && this.engineMove) makeEngineMove();
							});
							Piece piece = this.board.getBoard()[Board.convertNotation(this.currentSelection)[0]][Board.convertNotation(this.currentSelection)[1]];
							if (this.board.getValidMoves(piece).contains(not)){
								this.animation.start();
							} else {
								this.currentSelection = null;
								this.currentMoves = null;
								this.animation = null;
								if (this.board.getBoard()[x][y] != null) showMoves = true;
							}
						} else if (this.board.getBoard()[x][y] != null){
							showMoves = true;
						}
						if (showMoves){
							this.currentSelection = not;
							this.currentMoves = this.board.getValidMoves(this.board.getBoard()[x][y]);
						}
					}
				} else if (e.getClickCount() == 2){
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
										this.hold.clear();
										this.moveStart = message.split(" ")[0];
										this.moveEnd = message.split(" ")[1];
										this.animation = null;
										this.currentSelection = null;
										this.gameFinished = this.board.isGameFinished();
										makePremove();
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
									Thread.sleep(2000);
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
					this.currentHold = h;
					if (!this.hold.keySet().contains(h)) this.hold.put(h, new ArrayList<String>());
				}
			} else if (e.getButton() == MouseButton.MIDDLE){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.overTheBoard)) return;
				makeEngineMove();
			}
			
		});
		
		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			if (h != null){
				String f = this.currentHold;
				this.currentHold = null;
				if (f != null){
					if (f.equals(h)){
						this.hold.clear();
					} else {
						this.hold.get(f).add(h);
					}
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
					if (this.gameFinished) continue;
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
		this.board = new Board(fen, 3*60*1000);
		this.gameFinished = false;
		this.moveStart = null;
		this.moveEnd = null;
		this.hold.clear();
		this.premoves.clear();
		this.currentHold = null;
	}
	
	private void makeEngineMove(){
		if (this.gameFinished) return;
		new Thread(() -> {
			String output = this.engine.getBestMove(board.getFEN(), 700);
			if (output != null){
				this.animation = new PieceAnimation(output.split(" ")[0], output.split(" ")[1], () -> {
					this.board.move(output.split(" ")[0], output.split(" ")[1]);
					if (this.client != null) this.client.sendMessage(output.split(" ")[0]+" "+output.split(" ")[1]);
					this.hold.clear();
					this.currentSelection = null;
					this.moveStart = output.split(" ")[0];
					this.moveEnd = output.split(" ")[1];
					this.animation = null;
					this.gameFinished = this.board.isGameFinished();
					makePremove();
				});
				this.animation.start();
			}
		}).start();
	}
	
	private List<String> getPremoves(){
		List<String> pres = new ArrayList<>();
		for (Premove p : this.premoves){
			pres.add(p.startPos);
			pres.add(p.endPos);
		}
		return pres;
	}
	
	private void makePremove(){
		if (this.gameFinished || this.premoves.size() == 0){
			this.premoves.clear();
			return;
		}
		Premove pre = this.premoves.remove(0);
		this.animation = new PieceAnimation(pre.startPos, pre.endPos, () -> {
			boolean ok = this.board.move(pre.startPos, pre.endPos);
			if (ok){
				if (this.client != null) this.client.sendMessage(pre.startPos+" "+pre.endPos);
				this.hold.clear();
				this.moveStart = pre.startPos;
				this.moveEnd = pre.endPos;
				this.gameFinished = this.board.isGameFinished();
				if (this.engineMove) makeEngineMove();
			} else {
				this.premoves.clear();
			}
			this.animation = null;
		});
		this.animation.start();
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.CYAN);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.save();
		gc.translate(0, SPACE);
		Piece[][] pieces = this.board.getBoard();
		List<String> pres = getPremoves();
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				gc.setFill((i+7*j) % 2 == 0 ? Color.WHITE : Color.GREEN);
				String not = Board.convertPosition(this.viewPoint == Color.BLACK ? 7-i : i, this.viewPoint == Color.BLACK ? 7-j : j);
				if (this.currentSelection != null){
					int[] pos = Board.convertNotation(this.currentSelection);
					if (i == (this.viewPoint == Color.BLACK ? 7-pos[0] : pos[0]) && j == (this.viewPoint == Color.BLACK ? 7-pos[1] : pos[1])){
						gc.setFill(Color.RED);
					}
				}
				if (pres.contains(not)){
					gc.setFill(Color.BLUE);
				}
				gc.fillRect(i*SQUARE_SIZE, j*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
				if (not.equals(this.moveStart) || not.equals(this.moveEnd)){
					gc.setFill(Color.YELLOW);
					gc.setGlobalAlpha(0.6);
					gc.fillRect(i*SQUARE_SIZE, j*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
					gc.setGlobalAlpha(1.0);
				}
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
				String text = "";
				if (j == 7){
					text += String.valueOf(Board.convertPosition(this.viewPoint == Color.BLACK ? 7-i : i, this.viewPoint == Color.BLACK ? 7-j : j).toCharArray()[0]);
				}
				if (i == 0){
					text += String.valueOf(Board.convertPosition(this.viewPoint == Color.BLACK ? 7-i : i, this.viewPoint == Color.BLACK ? 7-j : j).toCharArray()[1]);
				}
				gc.setFill(Color.BLACK);
				gc.fillText(text, i*SQUARE_SIZE+SQUARE_SIZE*0.1, (j+1)*SQUARE_SIZE-SQUARE_SIZE*0.1);
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
		
		double wd = SPACE*0.6;
		double bd = HEIGHT-SPACE*0.6;
		
		gc.setFill(Color.BLACK);
		int bm = this.board.getMaterial(Color.BLACK);
		int wm = this.board.getMaterial(Color.WHITE);
		int diff = wm-bm;
		if (diff < 0) gc.fillText(Integer.toString(-diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? wd : bd);
		if (diff > 0) gc.fillText(Integer.toString(diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? bd : wd);
		
		List<Piece> black = this.board.getMaterialList(Color.BLACK);
		List<Piece> white = this.board.getMaterialList(Color.WHITE);
		gc.save();
		for (int i = 0; i < black.size(); i++){
			Piece piece = black.get(i);
			Piece prev = i == 0 ? null : black.get(i-1);
			if (i > 0) gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.05, this.viewPoint == Color.WHITE ? wd : bd, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
		}
		gc.restore();
		gc.save();
		for (int i = 0; i < white.size(); i++){
			Piece piece = white.get(i);
			Piece prev = i == 0 ? null : white.get(i-1);
			if (i > 0) gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.05, this.viewPoint == Color.WHITE ? bd : wd, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
		}
		gc.restore();

		for (Map.Entry<String, List<String>> entry : this.hold.entrySet()){
			if (entry.getKey() == null || entry.getValue() == null) continue;
			for (String value : entry.getValue()){
				int[] h1 = Board.convertNotation(entry.getKey());
				int[] h2 = Board.convertNotation(value);
				
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
		}
		
		gc.fillText("Eval: "+this.eval, WIDTH*0.7, HEIGHT-SPACE*0.7);
		
		int count = 0;
		for (int i = Math.max(this.board.getMoves().size()-7, 0); i < this.board.getMoves().size(); i++){
			gc.setStroke(Color.BLACK);
			gc.setFill(i % 2 == 0 ? Color.web("#F58B23") : Color.web("#7D4711"));
			double w = WIDTH/7;
			double h = SPACE*0.2;
			double xp = 0+(count++)*w;
			double yp = SPACE*0.15;
			gc.fillRect(xp, yp, w, h);
			gc.strokeRect(xp, yp, w, h);
			gc.strokeText(this.board.getMoves().get(i), xp+w*0.1, yp+h*0.75);
		}
		
		gc.setStroke(Color.BLACK);
		double timeWidth = WIDTH*0.3;
		double timeHeight = SPACE*0.25;
		gc.strokeRect(WIDTH*0.65, this.viewPoint == Color.WHITE ? wd : bd, timeWidth, timeHeight);
		gc.strokeRect(WIDTH*0.65, this.viewPoint == Color.WHITE ? bd : wd, timeWidth, timeHeight);
		gc.setFill(Color.BLACK);
		String topText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.BLACK)) : formatTime(this.board.getTime(Color.WHITE));
		String bottomText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.WHITE)) : formatTime(this.board.getTime(Color.BLACK));
		gc.fillText(topText, WIDTH*0.65+timeWidth*0.1, (this.viewPoint == Color.WHITE ? wd : bd)+timeHeight*0.75);
		gc.fillText(bottomText, WIDTH*0.65+timeWidth*0.1, (this.viewPoint == Color.WHITE ? bd : wd)+timeHeight*0.75);
		
		if (this.gameFinished || Server.clients.size() == 1){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
			if (this.gameFinished) this.client = null;
		}
		
		this.board.tick();
		
		if (this.board.getTime(Color.WHITE) <= 0 || this.board.getTime(Color.BLACK) <= 0){
			this.gameFinished = this.board.isGameFinished();
		}
	}
	
	private static String formatTime(int time){
		int h = time / (60*60*1000);
		int m = time % (60*60*1000) / (60*1000);
		int s = ((time % (60*60*1000)) / 1000) % 60;
		int ms = time % (60*60*1000) % 1000;
		String text = "";
		if (h > 0){
			return String.format("%d:%d:%d.%d", h, m, s, ms);
		} else {
			return String.format("%d:%d.%d", m, s, ms);
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
