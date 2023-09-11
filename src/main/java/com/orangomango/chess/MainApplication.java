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
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.media.*;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;

import java.io.*;
import java.util.*;

import com.orangomango.chess.multiplayer.HttpServer;
import com.orangomango.chess.multiplayer.Server;
import com.orangomango.chess.multiplayer.Client;
import com.orangomango.chess.ui.*;

/**
 * Chess client made in Java/JavaFX
 * @version 2.0
 * @author OrangoMango [https://orangomango.github.io]
 */
public class MainApplication extends Application{
	private static double WIDTH = 850;
	private static double HEIGHT = 600;
	private static int SQUARE_SIZE = (int)(WIDTH*0.05);
	private static Point2D SPACE = new Point2D(WIDTH*0.1, (HEIGHT-SQUARE_SIZE*8)/2);
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
	private boolean dragging = false;
	private UiScreen uiScreen;
	
	private Client client;
	private static Color startColor = Color.WHITE;
	private HttpServer httpServer;
	
	public static Media MOVE_SOUND, CAPTURE_SOUND, CASTLE_SOUND, CHECK_SOUND, ILLEGAL_SOUND, PROMOTE_SOUND;
	private static Image PLAY_BLACK_IMAGE, PLAY_WHITE_IMAGE, LAN_IMAGE, SERVER_IMAGE, TIME_IMAGE, SINGLE_IMAGE, MULTI_IMAGE, BACK_IMAGE;
	
	private static class Premove{
		public String startPos, endPos, prom;
		
		public Premove(String s, String e, String prom){
			this.startPos = s;
			this.endPos = e;
			this.prom = prom;
		}
	}
	
	@Override
	public void start(Stage stage){		
		this.viewPoint = startColor;
		loadSounds();
		loadImages();

		stage.setTitle("Chess v2.0");
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);
		this.board = new Board(STARTPOS, 60000, 0);
		this.engine = new Engine();

		/*this.httpServer = new HttpServer("http://127.0.0.1/paul_home/Chess-server/index.php", "test");
		this.httpServer.setOnRequest((time, p1, p2, prom) -> {
			this.animation = new PieceAnimation(p1, p2, () -> {
				this.board.setTime(this.viewPoint == Color.WHITE ? Color.BLACK : Color.WHITE, time);
				this.board.move(p1, p2, prom);
				this.hold.clear();
				this.moveStart = p1;
				this.moveEnd = p2;
				this.animation = null;
				this.currentSelection = null;
				this.gameFinished = this.board.isGameFinished();
				makePremove();
			});
			this.animation.start();
		});
		this.httpServer.listen();*/

		// UI
		this.uiScreen = buildHomeScreen(gc);
		
		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				String not = getNotation(e);
				Point2D clickPoint = getClickPoint(e.getX(), e.getY());
				int x = (int)(clickPoint.getX()/SQUARE_SIZE);
				int y = (int)(clickPoint.getY()/SQUARE_SIZE);
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
							} else if (this.board.getBoard()[x][y] != null && this.board.getBoard()[x][y].getColor() == this.viewPoint){
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
				} else {
					for (UiObject obj : this.uiScreen.getChildren()){
						if (obj instanceof Clickable){
							((Clickable)obj).click(e.getX(), e.getY());
						}
					}
				}
				/*if (e.getClickCount() == 2){
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
						try {
							String ip = sip.getText().equals("") ? "192.168.1.247" : sip.getText();
							int port = sport.getText().equals("") ? 1234 : Integer.parseInt(sport.getText());
							Server server = new Server(ip, port);
							alert.close();
						} catch (NumberFormatException ex){
							Logger.writeError(ex.getMessage());
						}
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
							Logger.writeError(ex.getMessage());
						}
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
						ClipboardContent cc = new ClipboardContent();
						cc.putString(data.getText());
						Clipboard cb = Clipboard.getSystemClipboard();
						cb.setContent(cc);
					});
					startEngine.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || !this.engine.isRunning());
					startEngine.setOnAction(ev -> {
						this.overTheBoard = true;
						Thread eg = new Thread(() -> {
							try {
								Thread.sleep(2000);
								makeEngineMove(true);
							} catch (InterruptedException ex){
								Logger.writeError(ex.getMessage());
							}
						});
						eg.setDaemon(true);
						eg.start();
						alert.close();
					});
					reset.setOnAction(ev -> {
						try {
							String text = data.getText();
							long time = timeControl.getText().equals("") ? 180000l : Long.parseLong(timeControl.getText().split("\\+")[0])*1000;
							int inc = timeControl.getText().equals("") ? 0 : Integer.parseInt(timeControl.getText().split("\\+")[1]);
							reset(text, time, inc);
							alert.close();
						} catch (Exception ex){
							Logger.writeError(ex.getMessage());
						}
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
					eng.setDisable((this.board.getMovesN() > 1 && !this.gameFinished) || !this.engine.isRunning());
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
				}*/
			} else if (e.getButton() == MouseButton.SECONDARY){
				String h = getNotation(e);
				if (h != null){
					this.currentHold = h;
					if (!this.hold.keySet().contains(h)) this.hold.put(h, new ArrayList<String>());
				}
			} else if (e.getButton() == MouseButton.MIDDLE){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.overTheBoard)) return;
				makeEngineMove(false);
			}
		});
		
		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				this.dragging = true;
				if (this.draggingPiece != null){
					this.dragX = e.getX();
					double oldY = this.dragY;
					this.dragY = e.getY();
					if (this.promotionPiece == null){
						if (this.draggingPiece.getType().getName() == Piece.PIECE_PAWN){
							int y = (int)(getClickPoint(e.getX(), e.getY()).getY()/SQUARE_SIZE);
							if (this.viewPoint == Color.BLACK) y = 7-y;
							Piece prom = new Piece(Piece.Pieces.QUEEN, this.draggingPiece.getColor(), -1, -1);
							if (this.draggingPiece.getColor() == Color.WHITE && y == 0 && this.draggingPiece.getY() == 1){
								this.promotionPiece = prom;
							} else if (this.draggingPiece.getColor() == Color.BLACK && y == 7 && this.draggingPiece.getY() == 6){
								this.promotionPiece = prom;
							}
						}
					} else {
						double y = this.draggingPiece.getColor() == Color.WHITE ? 0 : 8; // Promotion square
						if (this.viewPoint == Color.BLACK) y = 8-y;
						y *= SQUARE_SIZE;
						y += SPACE.getY();
						String[] proms = new String[]{"Q", "R", "B", "N"};
						int difference = (int)(Math.round(e.getY()-y)/SQUARE_SIZE);
						double mouseDifference = this.dragY-oldY;
						if (this.viewPoint == Color.BLACK) mouseDifference *= -1;
						if (mouseDifference == 0 || (mouseDifference < 0 && this.draggingPiece.getColor() == Color.WHITE) || (mouseDifference > 0 && this.draggingPiece.getColor() == Color.BLACK)){
							return;
						} else {
							difference = Math.abs(difference);
							this.promotionPiece = new Piece(Piece.getType(proms[difference % 4]), this.draggingPiece.getColor(), -1, -1);;
						}
					}
				}
			}
		});
		
		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			if (e.getButton() == MouseButton.PRIMARY){
				Point2D clickPoint = getClickPoint(e.getX(), e.getY());
				int x = (int)(clickPoint.getX()/SQUARE_SIZE);
				int y = (int)(clickPoint.getY()/SQUARE_SIZE);
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
			this.dragging = false;
		});
		
		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
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

		stage.widthProperty().addListener((ob, oldV, newV) -> resize((double)newV, HEIGHT, canvas));
		stage.heightProperty().addListener((ob, oldV, newV) -> resize(WIDTH, (double)newV, canvas));

		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("/icon.png")));
		stage.show();
	}

	private UiScreen buildHomeScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		UiButton whiteButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.08, 0.2, 0.2), PLAY_WHITE_IMAGE, () -> this.viewPoint = Color.WHITE);
		UiButton blackButton = new UiButton(uiScreen, gc, new Rectangle2D(0.35, 0.08, 0.2, 0.2), PLAY_BLACK_IMAGE, () -> this.viewPoint = Color.BLACK);
		blackButton.connect(whiteButton, this.viewPoint == Color.BLACK);
		whiteButton.connect(blackButton, this.viewPoint == Color.WHITE);
		UiButton timeButton = new UiButton(uiScreen, gc, new Rectangle2D(0.65, 0.08, 0.25, 0.2), TIME_IMAGE, () -> this.uiScreen = buildClockScreen(gc));
		UiButton singleButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.3, 0.8, 0.2), SINGLE_IMAGE, () -> this.uiScreen = buildStockfishScreen(gc));
		UiButton boardButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.5, 0.8, 0.2), MULTI_IMAGE, () -> System.out.println("Clicked4"));
		UiButton lanButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.7, 0.35, 0.2), LAN_IMAGE, () -> this.uiScreen = buildLanScreen(gc));
		UiButton multiplayerButton = new UiButton(uiScreen, gc, new Rectangle2D(0.55, 0.7, 0.35, 0.2), SERVER_IMAGE, () -> this.uiScreen = buildServerScreen(gc));

		uiScreen.getChildren().add(blackButton);
		uiScreen.getChildren().add(whiteButton);
		uiScreen.getChildren().add(timeButton);
		uiScreen.getChildren().add(singleButton);
		uiScreen.getChildren().add(boardButton);
		uiScreen.getChildren().add(lanButton);
		uiScreen.getChildren().add(multiplayerButton);
		return uiScreen;
	}

	private UiScreen buildClockScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));

		uiScreen.getChildren().add(backButton);
		return uiScreen;
	}

	private UiScreen buildStockfishScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));

		uiScreen.getChildren().add(backButton);
		return uiScreen;
	}

	private UiScreen buildLanScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));
		UiTextField ipField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.1, 0.8, 0.2), "127.0.0.1");
		UiTextField portField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.3, 0.8, 0.2), "1234");
		UiButton startServer = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.5, 0.2, 0.2), TIME_IMAGE, () -> {
			try {
				String ip = ipField.getValue();
				int port = Integer.parseInt(portField.getValue());
				Server server = new Server(ip, port);
				System.out.println("Server started... 0/2 players connected");
			} catch (NumberFormatException ex){
				Logger.writeError(ex.getMessage());
			}
		});
		UiButton connect = new UiButton(uiScreen, gc, new Rectangle2D(0.4, 0.5, 0.2, 0.2), TIME_IMAGE, () -> {
			try {
				String ip = ipField.getValue();
				int port = Integer.parseInt(portField.getValue());
				this.client = new Client(ip, port, this.viewPoint);
				// Set board time
				// ...
				reset("", this.board.getGameTime(), this.board.getIncrementTime()); // Reset the board to startpos
				if (!this.client.isConnected()){
					this.client = null; // Error during the connection
					return;
				}
				System.out.println("connected");
				this.viewPoint = this.client.getColor();
				this.uiScreen = buildHomeScreen(gc);
				this.overTheBoard = false;
				Thread listener = new Thread(() -> {
					while (!this.gameFinished){
						String message = this.client.getMessage();
						if (message == null){
							System.exit(0);
						} else {
							String moveData = message.split(";")[1];
							this.animation = new PieceAnimation(moveData.split(" ")[0], moveData.split(" ")[1], () -> {
								this.board.setTime(this.viewPoint == Color.WHITE ? Color.BLACK : Color.WHITE, Integer.parseInt(message.split(";")[0]));
								String prom = moveData.split(" ").length == 3 ? moveData.split(" ")[2] : null;
								this.board.move(moveData.split(" ")[0], moveData.split(" ")[1], prom);
								this.hold.clear();
								this.moveStart = moveData.split(" ")[0];
								this.moveEnd = moveData.split(" ")[1];
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
			} catch (NumberFormatException ex){
				Logger.writeError(ex.getMessage());
			}
		});

		uiScreen.getChildren().add(backButton);
		uiScreen.getChildren().add(ipField);
		uiScreen.getChildren().add(portField);
		uiScreen.getChildren().add(startServer);
		uiScreen.getChildren().add(connect);
		return uiScreen;
	}

	private UiScreen buildServerScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));
		UiTextField roomField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.1, 0.8, 0.2), "room-"+(int)(Math.random()*100000));

		uiScreen.getChildren().add(backButton);
		uiScreen.getChildren().add(roomField);
		return uiScreen;
	}

	private void resize(double w, double h, Canvas canvas){
		WIDTH = w;
		HEIGHT = h;
		SQUARE_SIZE = (int)Math.min(HEIGHT/8*0.85, WIDTH*0.05);
		SPACE = new Point2D(WIDTH*0.1, (HEIGHT-SQUARE_SIZE*8)/2);
		canvas.setWidth(w);
		canvas.setHeight(h);
		this.uiScreen.setRect(new Rectangle2D(SPACE.getX()*2+SQUARE_SIZE*8, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
	}

	private Point2D getClickPoint(double x, double y){
		return new Point2D(x-SPACE.getX(), y-SPACE.getY());
	}
	
	private boolean makeUserMove(String not, int x, int y, boolean skipAnimation, String promType){
		boolean isProm = isPromotion(this.currentSelection, not);
		String prom = isProm ? promType : null;
		this.animation = new PieceAnimation(this.currentSelection, not, () -> {
			boolean ok = this.board.move(this.currentSelection, not, prom);
			if (this.client != null) this.client.sendMessage(this.board.getTime(this.viewPoint)+";"+this.currentSelection+" "+not+(prom == null ? "" : " "+prom));

			// TEST
			//this.httpServer.sendMove(String.format("%s;%s;%s;%s", this.board.getTime(this.viewPoint), this.currentSelection, not, prom));

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
			MainApplication.playSound(MainApplication.ILLEGAL_SOUND);
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
		if (e.getX() < SPACE.getX() || e.getY() < SPACE.getY()) return null;
		Point2D clickPoint = getClickPoint(e.getX(), e.getY());
		int x = (int)(clickPoint.getX()/SQUARE_SIZE);
		int y = (int)(clickPoint.getY()/SQUARE_SIZE);
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
					if (this.client != null) this.client.sendMessage(this.board.getTime(this.viewPoint)+";"+output.split(" ")[0]+" "+output.split(" ")[1]);
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
				if (this.client != null) this.client.sendMessage(this.board.getTime(this.viewPoint)+";"+pre.startPos+" "+pre.endPos+(pre.prom == null ? "" : " "+pre.prom));
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
		gc.translate(SPACE.getX(), SPACE.getY());
		Piece[][] pieces = this.board.getBoard();
		List<String> pres = getPremoves();

		// Grid
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
		
		// Pieces
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
					if (!(piece == this.draggingPiece && this.dragging)) gc.drawImage(piece.getImage(), pos.getX()*SQUARE_SIZE, pos.getY()*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
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
		
		if (this.draggingPiece != null && this.dragging){
			gc.drawImage(this.promotionPiece == null ? this.draggingPiece.getImage() : this.promotionPiece.getImage(), this.dragX-SPACE.getX()-SQUARE_SIZE/2.0, this.dragY-SPACE.getY()-SQUARE_SIZE/2.0, SQUARE_SIZE, SQUARE_SIZE);
		}
		
		gc.restore();
		
		double wd = SPACE.getY()-SQUARE_SIZE*0.7;
		double bd = SPACE.getY()+SQUARE_SIZE*8.65;
		
		// Captured difference
		gc.setFill(Color.BLACK);
		int bm = this.board.getMaterial(Color.BLACK);
		int wm = this.board.getMaterial(Color.WHITE);
		int diff = wm-bm;
		if (diff < 0) gc.fillText(Integer.toString(-diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? wd : bd);
		if (diff > 0) gc.fillText(Integer.toString(diff), WIDTH*0.05, this.viewPoint == Color.WHITE ? bd : wd);
		
		// Captured pieces
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

		// Arrows
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
					gc.strokeLine(h1[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h1[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0, h1[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0);
					gc.strokeLine(h1[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0, h2[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0);
					rad = Math.atan2(0, h2[0]-h1[0]);
				} else {
					gc.strokeLine(h1[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h1[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0, h2[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE/2.0, h2[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE/2.0);
					rad = Math.atan2(h2[1]-h1[1], h2[0]-h1[0]);
				}
				
				gc.setFill(Color.ORANGE);
				gc.translate(h2[0]*SQUARE_SIZE+SPACE.getX()+SQUARE_SIZE*0.5, h2[1]*SQUARE_SIZE+SPACE.getY()+SQUARE_SIZE*0.5);
				gc.rotate(Math.toDegrees(rad));
				gc.fillPolygon(new double[]{-SQUARE_SIZE*0.3, -SQUARE_SIZE*0.3, SQUARE_SIZE*0.3}, new double[]{-SQUARE_SIZE*0.3, SQUARE_SIZE*0.3, 0}, 3);
				
				gc.restore();
			}
		}
		
		//gc.fillText("Eval: "+this.eval, WIDTH*0.7, HEIGHT-SPACE*0.7);
		
		// Moves played
		int count = 0;
		double wMove = SQUARE_SIZE*1.8;
		double hMove = SQUARE_SIZE*0.6;
		for (int i = Math.max(this.board.getMoves().size()-10, 0); i < this.board.getMoves().size(); i++){
			gc.setStroke(Color.BLACK);
			gc.setFill(i % 2 == 0 ? Color.web("#F58B23") : Color.web("#7D4711"));
			double xp = 10+(count++)*wMove;
			double yp = 30;
			gc.fillRect(xp, yp, wMove, hMove);
			gc.strokeRect(xp, yp, wMove, hMove);
			gc.setFill(Color.BLACK);
			gc.fillText((i/2+1)+"."+this.board.getMoves().get(i), xp+wMove*0.1, yp+hMove*0.75);
		}
		
		// Time remaining
		gc.save();
		gc.setStroke(Color.BLACK);
		double timeWidth = SQUARE_SIZE*2.5;
		double timeHeight = SQUARE_SIZE*0.8;
		double timeX = SPACE.getX()+SQUARE_SIZE*8-timeWidth;
		gc.strokeRect(timeX, wd-timeHeight, timeWidth, timeHeight);
		gc.strokeRect(timeX, bd, timeWidth, timeHeight);
		gc.setFill(Color.BLACK);
		String topText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.BLACK)) : formatTime(this.board.getTime(Color.WHITE));
		String bottomText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.WHITE)) : formatTime(this.board.getTime(Color.BLACK));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setFont(new Font("sans-serif", timeHeight*0.5));
		gc.fillText(topText, timeX+timeWidth/2, wd-timeHeight+timeHeight*0.65);
		gc.fillText(bottomText, timeX+timeWidth/2, bd+timeHeight*0.65);
		gc.restore();

		// UI
		this.uiScreen.render();
		
		if (this.board.getTime(Color.WHITE) == 0 || this.board.getTime(Color.BLACK) == 0) this.gameFinished = true;
		if (this.gameFinished){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
			if (this.gameFinished) this.client = null;
		}
		
		if (!this.gameFinished) this.board.tick();
	}
	
	private static String formatTime(int time){
		int h = time / (60*60*1000);
		int m = time % (60*60*1000) / (60*1000);
		int s = ((time % (60*60*1000)) / 1000) % 60;
		int ms = time % (60*60*1000) % 1000;
		String text = "";
		String msText = m == 0 && s < 30 ? String.format(".%03d", ms) : "";
		if (h > 0){
			return String.format("%d:%d:%02d", h, m, s)+msText;
		} else {
			return String.format("%d:%02d", m, s)+msText;
		}
	}

	private static void loadSounds(){
		MOVE_SOUND = new Media(MainApplication.class.getResource("/move.mp3").toExternalForm());
		CAPTURE_SOUND = new Media(MainApplication.class.getResource("/capture.mp3").toExternalForm());
		CASTLE_SOUND = new Media(MainApplication.class.getResource("/castle.mp3").toExternalForm());
		CHECK_SOUND = new Media(MainApplication.class.getResource("/move-check.mp3").toExternalForm());
		ILLEGAL_SOUND = new Media(MainApplication.class.getResource("/illegal.mp3").toExternalForm());
		PROMOTE_SOUND = new Media(MainApplication.class.getResource("/promote.mp3").toExternalForm());
	}

	private static void loadImages(){
		PLAY_BLACK_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_playblack.png"));
		PLAY_WHITE_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_playwhite.png"));
		LAN_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_lan.png"));
		SERVER_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_server.png"));
		TIME_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_timecontrol.png"));
		SINGLE_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_playstockfish.png"));
		MULTI_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_playboard.png"));
		BACK_IMAGE = new Image(MainApplication.class.getResourceAsStream("/button_back.png"));
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
	}
}
