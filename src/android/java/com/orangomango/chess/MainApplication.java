package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.layout.*;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.media.*;
import javafx.scene.control.*;

import java.lang.reflect.*;
import java.nio.file.*;
import android.os.Build;
import javafxports.android.FXActivity;
import android.media.MediaPlayer;
import android.view.View;
import android.os.Vibrator;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import java.io.*;
import java.util.*;

import com.orangomango.chess.multiplayer.Server;
import com.orangomango.chess.multiplayer.Client;

public class MainApplication extends Application{
	//public static final int SQUARE_SIZE = 65; //55;
	//private static final int SPACE = 120; //60;
	private static final double WIDTH = Screen.getPrimary().getVisualBounds().getWidth(); //SQUARE_SIZE*8;
	private static final double HEIGHT = Screen.getPrimary().getVisualBounds().getHeight(); //SPACE*2+SQUARE_SIZE*8;
	public static final int SQUARE_SIZE = (int)(WIDTH/8);
	private static final int SPACE = (int)(HEIGHT*0.25);
	private volatile int frames, fps;
	private static final int FPS = 40;
	private static final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
	private Board board;
	private Engine engine;
	private String currentSelection;
	private List<String> currentMoves;
	private volatile boolean gameFinished = false;
	private volatile String eval;
	private volatile PieceAnimation animation;
	private Color viewPoint;
	private boolean overTheBoard = true;
	private boolean engineMove = false;

	private Map<String, List<String>> hold = new HashMap<>();
	private String currentHold;
	private String moveStart, moveEnd;
	private List<Premove> premoves = new ArrayList<>();
	private Piece draggingPiece;
	private double dragX, dragY;
	private Piece promotionPiece;
	
	private Client client;
	private static Color startColor = Color.WHITE;
	
	private static Map<String, MediaPlayer> players = new HashMap<>();
	public static final String MOVE_SOUND = "move.mp3";
	public static final String CAPTURE_SOUND = "capture.mp3";
	public static final String  CASTLE_SOUND = "castle.mp3";
	public static final String CHECK_SOUND = "notify.mp3";

	public static Vibrator vibrator = (Vibrator)FXActivity.getInstance().getSystemService(Context.VIBRATOR_SERVICE);
	private static ClipboardManager clipboard = (ClipboardManager)FXActivity.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
	
	private static class Premove{
		public String startPos, endPos, prom;
		
		public Premove(String s, String e, String prom){
			this.startPos = s;
			this.endPos = e;
			this.prom = prom;
		}
	}

	@Override
       public void start(Stage stage) throws Exception{
		if (Build.VERSION.SDK_INT >= 29){
				Method forName = Class.class.getDeclaredMethod("forName", String.class);
				Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
				Class vmRuntimeClass = (Class) forName.invoke(null, "dalvik.system.VMRuntime");
				Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
				Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[] { String[].class} );
				Object vmRuntime = getRuntime.invoke(null);
				setHiddenApiExemptions.invoke(vmRuntime, (Object[])new String[][]{new String[]{"L"}});
		}
		loadSounds();

		FXActivity.getInstance().runOnUiThread(() -> {
			FXActivity.getInstance().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

			// Clear useless temp files in cache of previous sessions
			for (File f : FXActivity.getInstance().getCacheDir().listFiles()){
					f.delete();
			}
		});

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
		this.board = new Board(STARTPOS, 180000, 0);
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
							boolean isProm = isPromotion(this.currentSelection, not);
							String prom = isProm ? "Q" : null;
							this.premoves.add(new Premove(this.currentSelection, not, prom));
							this.currentSelection = null;
						}
					} else {
						boolean showMoves = false;
						if (this.currentSelection != null){
							showMoves = makeUserMove(not, x, y, false, "Q");
						} else if (this.board.getBoard()[x][y] != null){
							showMoves = true;
						}
						if (showMoves){
							this.currentSelection = not;
							this.currentMoves = this.board.getValidMoves(this.board.getBoard()[x][y]);
							this.draggingPiece = this.board.getBoard()[x][y];
							this.dragX = e.getX();
							this.dragY = e.getY();
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
					TextField timeControl = new TextField();
					timeControl.setPromptText("180+0");
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
						try {
							String ip = cip.getText().equals("") ? "192.168.1.247" : cip.getText();
							int port = cport.getText().equals("") ? 1234 : Integer.parseInt(cport.getText());
							this.client = new Client(ip, port, this.viewPoint);
							if (Server.clients.size() == 1){
								reset(data.getText(), this.board.getGameTime(), this.board.getIncrementTime());
							}
							if (!this.client.isConnected()){
								this.client = null;
								return;
							}
							this.viewPoint = this.client.getColor();
							this.overTheBoard = false;
							Thread listener = new Thread(() -> {
								while (!this.gameFinished){
									String message = this.client.getMessage();
									if (message == null){
										System.exit(0);
									} else {
										this.animation = new PieceAnimation(message.split(" ")[0], message.split(" ")[1], () -> {
											String prom = message.split(" ").length == 3 ? message.split(" ")[2] : null;
											this.board.move(message.split(" ")[0], message.split(" ")[1], prom);
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
						} catch (NumberFormatException ex){
							//Logger.writeError(ex.getMessage());
						}
						this.viewPoint = this.client.getColor();
						Thread listener = new Thread(() -> {
							while (!this.gameFinished){
								String message = this.client.getMessage();
								if (message == null){
									System.exit(0);
								} else {
									this.animation = new PieceAnimation(message.split(" ")[0], message.split(" ")[1], () -> {
										String prom = message.split(" ").length == 3 ? message.split(" ")[2] : null;
										this.board.move(message.split(" ")[0], message.split(" ")[1], prom);
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
					eng.setDisable(!this.engine.isRunning());
					eng.setSelected(this.engineMove);
					eng.setOnAction(ev -> {
						this.overTheBoard = true;
						otb.setSelected(true);
						this.engineMove = eng.isSelected();
						otb.setDisable(this.engineMove);
						if (this.engineMove && this.board.getPlayer() != this.viewPoint){
							makeEngineMove(false);
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
						ClipData clip = ClipData.newPlainText("board", data.getText());
						clipboard.setPrimaryClip(clip);
					});
					startEngine.setDisable(!this.engine.isRunning());
					startEngine.setOnAction(ev -> {
						this.overTheBoard = true;
						Thread eg = new Thread(() -> {
							try {
								Thread.sleep(2000);
								makeEngineMove(true);
							} catch (InterruptedException ex){
								ex.printStackTrace();
							}
						});
						eg.setDaemon(true);
						eg.start();
						alert.close();
					});
					reset.setOnAction(ev -> {
						String text = data.getText();
						long time = timeControl.getText().equals("") ? 180000l : Long.parseLong(timeControl.getText().split("\\+")[0])*1000;
						int inc = timeControl.getText().equals("") ? 0 : Integer.parseInt(timeControl.getText().split("\\+")[1]);
						reset(text, time, inc);
						alert.close();
					});
					HBox serverInfo = new HBox(5, sip, sport, startServer);
					HBox clientInfo = new HBox(5, cip, cport, connect);
					HBox whiteBlack = new HBox(5, w, b);
					HBox rs = new HBox(5, reset, startEngine);
					serverInfo.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					clientInfo.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					whiteBlack.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					timeControl.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					rs.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					eng.setDisable(this.board.getMovesN() > 1 && !this.gameFinished);
					otb.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || this.client != null);
					layout.add(serverInfo, 0, 0);
					layout.add(clientInfo, 0, 1);
					layout.add(timeControl, 0, 2);
					layout.add(eng, 0, 3);
					layout.add(otb, 0, 4);
					layout.add(whiteBlack, 0, 5);
					layout.add(rs, 0, 6);
					layout.add(copy, 0, 7);
					layout.add(data, 0, 8);
					alert.getDialogPane().setContent(layout);
					alert.showAndWait();
				}
			}
			if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2){
				String h = getNotation(e);
				if (h != null){
					this.currentHold = h;
					if (!this.hold.keySet().contains(h)) this.hold.put(h, new ArrayList<String>());
				}
			}
		});
		
		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.draggingPiece != null){
					this.dragX = e.getX();
					double oldY = this.dragY;
					this.dragY = e.getY();
					if (this.promotionPiece == null){
						if (this.draggingPiece.getType().getName() == Piece.PIECE_PAWN){
							int y = (int) ((e.getY()-SPACE)/SQUARE_SIZE);
							if (this.viewPoint == Color.BLACK) y = 7-y;
							Piece prom = new Piece(Piece.Pieces.QUEEN, this.draggingPiece.getColor(), -1, -1);
							if (this.draggingPiece.getColor() == Color.WHITE && y == 0 && this.draggingPiece.getY() == 1){
								this.promotionPiece = prom;
							} else if (this.draggingPiece.getColor() == Color.BLACK && y == 7 && this.draggingPiece.getY() == 6){
								this.promotionPiece = prom;
							}
						}
					} else if ((e.getY() > oldY && this.draggingPiece.getColor() == Color.WHITE) || (e.getY() < oldY && this.draggingPiece.getColor() == Color.BLACK)){
						double y = this.draggingPiece.getColor() == Color.WHITE ? 0 : 8;
						y *= SQUARE_SIZE;
						y += SPACE;
						String[] proms = new String[]{"Q", "R", "B", "N"};
						int difference = (int)Math.round(e.getY()-y);
						difference /= SQUARE_SIZE;
						if ((difference < 0 && this.draggingPiece.getColor() == Color.WHITE) || (difference > 0 && this.draggingPiece.getColor() == Color.BLACK)){
							return;
						} else {
							difference = Math.abs(difference);
							this.promotionPiece = new Piece(Piece.getType(proms[difference % 4]), this.draggingPiece.getColor(), -1, -1);;
						}
					}
				}
			}
		});

		canvas.setOnTouchPressed(e -> {
			if (e.getTouchPoints().size() == 2){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.overTheBoard)) return;
				makeEngineMove(false);
			}
		});
		
		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			if (e.getButton() == MouseButton.PRIMARY){
				int x = (int)(e.getX()/SQUARE_SIZE);
				int y = (int)((e.getY()-SPACE)/SQUARE_SIZE);
				if (this.viewPoint == Color.BLACK){
					x = 7-x;
					y = 7-y;
				}
				if (this.currentSelection != null && h != null && this.draggingPiece != null && !this.currentSelection.equals(h)){
					makeUserMove(h, x, y, true, this.promotionPiece == null ? null : this.promotionPiece.getType().getName());
				} else {
					this.draggingPiece = null;
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
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
			}
			this.promotionPiece = null;
		});

		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			if (e.getButton() == MouseButton.PRIMARY){
				int x = (int)(e.getX()/SQUARE_SIZE);
				int y = (int)((e.getY()-SPACE)/SQUARE_SIZE);
				if (this.viewPoint == Color.BLACK){
					x = 7-x;
					y = 7-y;
				}
				if (this.currentSelection != null && h != null && this.draggingPiece != null && !this.currentSelection.equals(h)){
					makeUserMove(h, x, y, true, this.promotionPiece == null ? null : this.promotionPiece.getType().getName());
				} else {
					this.draggingPiece = null;
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
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
			}
			this.promotionPiece = null;
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
					String ev = eng.getEval(this.board.getFEN());
					if (ev != null) this.eval = ev;
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
	
	private boolean makeUserMove(String not, int x, int y, boolean skipAnimation, String promType){
		boolean isProm = isPromotion(this.currentSelection, not);
		String prom = isProm ? promType : null;
		this.animation = new PieceAnimation(this.currentSelection, not, () -> {
			boolean ok = this.board.move(this.currentSelection, not, prom);
			if (this.client != null) this.client.sendMessage(this.currentSelection+" "+not+(prom == null ? "" : " "+prom));
			this.moveStart = this.currentSelection;
			this.moveEnd = not;
			this.currentSelection = null;
			this.currentMoves = null;
			this.hold.clear();
			this.animation = null;
			this.gameFinished = this.board.isGameFinished();
			if (ok && this.engineMove) makeEngineMove(false);
		});
		if (skipAnimation) this.animation.setAnimationTime(0);
		Piece piece = this.board.getBoard()[Board.convertNotation(this.currentSelection)[0]][Board.convertNotation(this.currentSelection)[1]];
		if (this.board.getValidMoves(piece).contains(not)){
			this.animation.start();
			this.draggingPiece = null;
		} else {
			this.currentSelection = null;
			this.currentMoves = null;
			this.animation = null;
			this.draggingPiece = null;
			if (this.board.getBoard()[x][y] != null) return true;
		}
		return false;
	}

	private boolean isPromotion(String a, String b){
		Piece piece = this.board.getBoard()[Board.convertNotation(a)[0]][Board.convertNotation(a)[1]];
		if (piece.getType().getName() == Piece.PIECE_PAWN){
			if (piece.getColor() == Color.WHITE && Board.convertNotation(b)[1] == 0){
				return true;
			} else if (piece.getColor() == Color.BLACK && Board.convertNotation(b)[1] == 7){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
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
	
	private void reset(String text, long time, int inc){
		String fen = STARTPOS;
		if (text.startsWith("CUSTOM\n")){
			fen = text.split("\n")[1];
		}
		this.board = new Board(fen, time, inc);
		this.gameFinished = false;
		this.moveStart = null;
		this.moveEnd = null;
		this.hold.clear();
		this.premoves.clear();
		this.currentHold = null;
		this.currentMoves = null;
	}
	
	private void makeEngineMove(boolean game){
		if (this.gameFinished) return;
		new Thread(() -> {
			String output = this.engine.getBestMove(this.board);
			if (output != null){
				this.animation = new PieceAnimation(output.split(" ")[0], output.split(" ")[1], () -> {
					String prom = output.split(" ").length == 3 ? output.split(" ")[2] : null;
					this.board.move(output.split(" ")[0], output.split(" ")[1], prom);
					if (this.client != null) this.client.sendMessage(output.split(" ")[0]+" "+output.split(" ")[1]);
					this.hold.clear();
					this.currentSelection = null;
					this.moveStart = output.split(" ")[0];
					this.moveEnd = output.split(" ")[1];
					this.animation = null;
					this.gameFinished = this.board.isGameFinished();
					makePremove();
					if (game) makeEngineMove(true);
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
			boolean ok = this.board.move(pre.startPos, pre.endPos, pre.prom);
			if (ok){
				if (this.client != null) this.client.sendMessage(pre.startPos+" "+pre.endPos+(pre.prom == null ? "" : " "+pre.prom));
				this.hold.clear();
				this.moveStart = pre.startPos;
				this.moveEnd = pre.endPos;
				this.gameFinished = this.board.isGameFinished();
				if (this.engineMove) makeEngineMove(false);
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
					if (piece != this.draggingPiece) gc.drawImage(piece.getImage(), pos.getX()*SQUARE_SIZE, pos.getY()*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
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
		
		if (this.draggingPiece != null){
			gc.drawImage(this.promotionPiece == null ? this.draggingPiece.getImage() : this.promotionPiece.getImage(), this.dragX-SQUARE_SIZE/2.0, this.dragY-SPACE-SQUARE_SIZE/2.0, SQUARE_SIZE, SQUARE_SIZE);
		}

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
		for (int i = Math.max(this.board.getMoves().size()-6, 0); i < this.board.getMoves().size(); i++){
			gc.setStroke(Color.BLACK);
			gc.setFill(i % 2 == 0 ? Color.web("#F58B23") : Color.web("#7D4711"));
			double w = WIDTH/6;
			double h = SPACE*0.2;
			double xp = 0+(count++)*w;
			double yp = SPACE*0.15;
			gc.fillRect(xp, yp, w, h);
			gc.strokeRect(xp, yp, w, h);
			gc.setFill(Color.BLACK);
			gc.fillText((i/2+1)+"."+this.board.getMoves().get(i), xp+w*0.1, yp+h*0.75);
		}

		gc.setStroke(Color.BLACK);
		double timeWidth = WIDTH*0.3;
		double timeHeight = SPACE*0.25;
		gc.strokeRect(WIDTH*0.65, wd, timeWidth, timeHeight);
		gc.strokeRect(WIDTH*0.65, bd, timeWidth, timeHeight);
		gc.setFill(Color.BLACK);
		String topText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.BLACK)) : formatTime(this.board.getTime(Color.WHITE));
		String bottomText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.WHITE)) : formatTime(this.board.getTime(Color.BLACK));
		gc.fillText(topText, WIDTH*0.65+timeWidth*0.1, wd+timeHeight*0.75);
		gc.fillText(bottomText, WIDTH*0.65+timeWidth*0.1, bd+timeHeight*0.75);

		if (this.gameFinished || Server.clients.size() == 1){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
			if (this.gameFinished) this.client = null;
		}

		if (!this.gameFinished) this.board.tick();

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

	private static void copyFile(String name){
		File file = new File(FXActivity.getInstance().getFilesDir().getAbsolutePath(), name);
		if (!file.exists()){
			try {
				Files.copy(MainApplication.class.getClassLoader().getResourceAsStream(name), file.toPath());
			} catch (IOException ioe){
				ioe.printStackTrace();
			}
		}
    }

	private static void loadSounds(){
		copyFile("capture.mp3");
		copyFile("castle.mp3");
		copyFile("move.mp3");
		copyFile("notify.mp3");

		// Load stockfish
		copyFile("stockfish");
	}

	
	public static void playSound(String media){
		MediaPlayer mp = players.getOrDefault(media, new MediaPlayer());
		boolean first = false;
		if (!players.containsKey(media)){
			players.put(media, mp);
			first = true;
		}
		try {
			if (first){
				mp.setDataSource(FXActivity.getInstance().getFilesDir().getAbsolutePath()+"/"+media);
				mp.prepare();
				mp.setOnCompletionListener(player -> {
					player.release();
					players.remove(media);
				});
			}
			mp.start();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
