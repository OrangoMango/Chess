package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Clipboard;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.media.*;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;

import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.extras.webtext.HtmlText;
import dev.webfx.stack.ui.controls.dialog.DialogUtil;
import dev.webfx.stack.ui.controls.dialog.DialogCallback;

import java.util.*;

import com.orangomango.chess.multiplayer.HttpServer;
import com.orangomango.chess.ui.*;

/**
 * Chess client made in Java/JavaFX
 * @version 2.0
 * @author OrangoMango [https://orangomango.github.io]
 */
public class MainApplication extends Application{
	private static boolean LANDSCAPE = true;
	private static double WIDTH = Screen.getPrimary().getVisualBounds().getWidth();
	private static double HEIGHT = Screen.getPrimary().getVisualBounds().getHeight();
	private static int SQUARE_SIZE = (int)(LANDSCAPE ? WIDTH*0.05 : WIDTH*0.09);
	private static Point2D SPACE = new Point2D(LANDSCAPE ? WIDTH*0.15 : WIDTH*0.18, (HEIGHT-SQUARE_SIZE*8)/2);
	private static final int FPS = 40;
	private static final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	public static CanvasPane MAIN_SCENE;
	
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
	private String gameOverText;
	
	private static Color startColor = Color.WHITE;
	private HttpServer httpServer;
	private boolean showBoard = LANDSCAPE;
	
	public static Media MOVE_SOUND, CAPTURE_SOUND, CASTLE_SOUND, CHECK_SOUND, ILLEGAL_SOUND, PROMOTE_SOUND, CHECKMATE_SOUND, DRAW_SOUND;
	private static Image PLAY_BLACK_IMAGE, PLAY_WHITE_IMAGE, LAN_IMAGE, SERVER_IMAGE, TIME_IMAGE, SINGLE_IMAGE, MULTI_IMAGE, BACK_IMAGE, CONNECT_CLIENT_IMAGE, START_SERVER_IMAGE, EDIT_IMAGE, SAVE_IMAGE, HTTP_IMAGE;
	
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
		MAIN_SCENE = new CanvasPane(canvas, (w, h) -> resize(w, h, canvas));
		pane.getChildren().add(MAIN_SCENE);
		this.board = new Board(STARTPOS, 600000, 0);
		this.engine = new Engine("https://stockfish.online/api/stockfish.php");

		// UI
		this.uiScreen = buildHomeScreen(gc);

		stage.setOnCloseRequest(e -> {
			if (this.httpServer != null){
				this.httpServer.delete();
			}
		});
		
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
				if (not != null && !this.gameFinished && this.showBoard){
					if (this.board.getPlayer() != this.viewPoint && (this.engineMove || !this.overTheBoard)){
						if (this.currentSelection == null){
							if (getPremoves().contains(not) || (this.board.getBoard()[x][y] != null && this.board.getBoard()[x][y].getColor() == this.viewPoint)){
								this.currentSelection = not;
							} else if (this.board.getBoard()[x][y] == null){
								this.premoves.clear();
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
					if (!this.uiScreen.isDisabled()){
						for (UiObject obj : this.uiScreen.getChildren()){
							if (obj instanceof Clickable){
								((Clickable)obj).click(e.getX(), e.getY());
							}
						}
					}

					if (!LANDSCAPE && !this.uiScreen.getRect().contains(e.getX(), e.getY())){
                        this.showBoard = !this.showBoard;
                    }
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				String h = getNotation(e);
				if (h != null && !this.gameFinished){
					this.currentHold = h;
					if (!this.hold.keySet().contains(h)) this.hold.put(h, new ArrayList<String>());
				}
			}
		});
		
		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.gameFinished) return;
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
							this.promotionPiece = new Piece(Piece.getType(proms[difference % 4]), this.draggingPiece.getColor(), -1, -1);
						}
					}
				}
			}
		});

		if (!LANDSCAPE){
			canvas.setOnScroll(e -> {
				this.showBoard = e.getDeltaY() < 0;
			});
		}
		
		canvas.setOnMouseReleased(e -> {
			String h = getNotation(e);
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
			int x = (int)(clickPoint.getX()/SQUARE_SIZE);
			int y = (int)(clickPoint.getY()/SQUARE_SIZE);
			if (this.viewPoint == Color.BLACK){
				x = 7-x;
				y = 7-y;
			}
			if (e.getButton() == MouseButton.PRIMARY){
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
							List<String> list = this.hold.get(f);
							if (list != null) list.add(h);
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

		calculateEval();

		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.getIcons().add(new Image(Resource.toUrl("/images/icon.png", MainApplication.class)));
		stage.show();
	}

	private void calculateEval(){
		this.engine.getEval(this.board.getFEN(), data -> {
			this.eval = data;
		});
	}

	private UiScreen buildHomeScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(LANDSCAPE ? SPACE.getX()*1.5+SQUARE_SIZE*8 : SPACE.getX()*1.5, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		uiScreen.setDisabled(!this.gameFinished && (this.board.getMoves().size() > 0 || this.httpServer != null));
		UiButton whiteButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.08, 0.2, 0.2), PLAY_WHITE_IMAGE, () -> this.viewPoint = Color.WHITE);
		UiButton blackButton = new UiButton(uiScreen, gc, new Rectangle2D(0.35, 0.08, 0.2, 0.2), PLAY_BLACK_IMAGE, () -> this.viewPoint = Color.BLACK);
		blackButton.connect(whiteButton, this.viewPoint == Color.BLACK);
		whiteButton.connect(blackButton, this.viewPoint == Color.WHITE);
		UiButton timeButton = new UiButton(uiScreen, gc, new Rectangle2D(0.65, 0.08, 0.25, 0.2), TIME_IMAGE, () -> this.uiScreen = buildClockScreen(gc));
		UiButton singleButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.3, 0.8, 0.2), SINGLE_IMAGE, () -> {
			this.overTheBoard = false;
			this.engineMove = true;
		});
		UiButton boardButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.5, 0.8, 0.2), MULTI_IMAGE, () -> {
			this.overTheBoard = true;
			this.engineMove = false;
		});
		singleButton.connect(boardButton, this.engineMove);
		boardButton.connect(singleButton, this.overTheBoard);
		UiButton lanButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.7, 0.35, 0.2), LAN_IMAGE, () -> {
			// Not supported error
		});
		UiButton multiplayerButton = new UiButton(uiScreen, gc, new Rectangle2D(0.55, 0.7, 0.35, 0.2), SERVER_IMAGE, () -> this.uiScreen = buildServerScreen(gc));
		UiButton editBoard = new UiButton(uiScreen, gc, new Rectangle2D(0.425, 0.875, 0.15, 0.15), EDIT_IMAGE, () -> {
			HtmlText header = new HtmlText("<b>Edit board</b>");
			TextField fenField = new TextField(this.board.getFEN());
			TextArea pgnField = new TextArea(this.board.getPGN());
			pgnField.setMinHeight(300);
			GridPane layout = new GridPane();
			layout.setHgap(5);
			layout.setVgap(5);
			layout.setPadding(new Insets(5, 5, 5, 5));
			layout.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			layout.add(header, 0, 0, 2, 1);
			layout.add(fenField, 0, 1, 2, 1);
			layout.add(pgnField, 0, 2, 2, 1);
			Button copyFen = new Button("Copy FEN");
			Button copyPgn = new Button("Copy PGN");
			Button loadFen = new Button("Load FEN");
			Button cancel = new Button("Cancel");
			cancel.setCancelButton(true);
			layout.add(new HBox(5, copyFen, copyPgn, loadFen, cancel), 0, 3, 2, 1);

			DialogCallback callback = DialogUtil.showModalNodeInGoldLayout(layout, MAIN_SCENE);

			copyFen.setOnAction(e -> {
				callback.closeDialog();
				ClipboardContent cc = new ClipboardContent();
				cc.putString(fenField.getText());
				Clipboard cb = Clipboard.getSystemClipboard();
				cb.setContent(cc);
			});

			copyPgn.setOnAction(e -> {
				ClipboardContent cc = new ClipboardContent();
				cc.putString(pgnField.getText());
				Clipboard cb = Clipboard.getSystemClipboard();
				cb.setContent(cc);
				callback.closeDialog();
			});

			loadFen.setOnAction(e -> {
				try {
					reset(fenField.getText(), this.board.getGameTime(), this.board.getIncrementTime());
					callback.closeDialog();
				} catch (IllegalStateException|ArrayIndexOutOfBoundsException ex){
					// ERROR
				}
			});

			cancel.setOnAction(e -> callback.closeDialog());
		});

		uiScreen.getChildren().add(blackButton);
		uiScreen.getChildren().add(whiteButton);
		uiScreen.getChildren().add(timeButton);
		uiScreen.getChildren().add(singleButton);
		uiScreen.getChildren().add(boardButton);
		uiScreen.getChildren().add(lanButton);
		uiScreen.getChildren().add(multiplayerButton);
		uiScreen.getChildren().add(editBoard);
		return uiScreen;
	}

	private UiScreen buildClockScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(LANDSCAPE ? SPACE.getX()*1.5+SQUARE_SIZE*8 : SPACE.getX()*1.5, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		uiScreen.setDisabled(!this.gameFinished && (this.board.getMoves().size() > 0 || this.httpServer != null));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));
		UiTextField timeField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.1, 0.8, 0.2), "600");
		UiTextField incrementField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.3, 0.8, 0.2), "0");
		UiButton saveButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.5, 0.8, 0.2), SAVE_IMAGE, () -> {
			long time = Long.parseLong(timeField.getValue())*1000;
			int inc = Integer.parseInt(incrementField.getValue());
			reset(STARTPOS, time, inc);
			this.uiScreen = buildHomeScreen(gc);
		});

		uiScreen.getChildren().add(backButton);
		uiScreen.getChildren().add(timeField);
		uiScreen.getChildren().add(incrementField);
		uiScreen.getChildren().add(saveButton);
		return uiScreen;
	}

	private UiScreen buildServerScreen(GraphicsContext gc){
		UiScreen uiScreen = new UiScreen(gc, new Rectangle2D(LANDSCAPE ? SPACE.getX()*1.5+SQUARE_SIZE*8 : SPACE.getX()*1.5, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
		uiScreen.setDisabled(!this.gameFinished && (this.board.getMoves().size() > 0 || this.httpServer != null));
		UiButton backButton = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.8, 0.2, 0.2), BACK_IMAGE, () -> this.uiScreen = buildHomeScreen(gc));
		UiTextField roomField = new UiTextField(uiScreen, gc, new Rectangle2D(0.1, 0.1, 0.8, 0.2), "room-"+(int)(Math.random()*100000));
		UiButton connect = new UiButton(uiScreen, gc, new Rectangle2D(0.1, 0.3, 0.8, 0.2), HTTP_IMAGE, () -> {
			this.httpServer = new HttpServer("https://mangogamesid.000webhostapp.com/chess-server/index.php", roomField.getValue(), this.viewPoint == Color.WHITE ? "WHITE" : "BLACK", () -> {
				if (this.httpServer.isFull()){
					this.httpServer = null;
					return; //System.exit(0);
				}
				this.viewPoint = this.httpServer.getColor();
				this.httpServer.getHeader(header -> {
					if (header == null){
						this.httpServer.sendHeader(this.board.getFEN()+";"+this.board.getGameTime()+"+"+this.board.getIncrementTime());	
					} else {
						reset(header.split(";")[0], Long.parseLong(header.split(";")[1].split("\\+")[0]), Integer.parseInt(header.split(";")[1].split("\\+")[1]));
					}
					this.overTheBoard = false;
					this.engineMove = false;
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
							if (this.gameFinished){
								this.httpServer.delete();
							}
							calculateEval();
							makePremove();
						});
						this.animation.start();
					});
					this.httpServer.listen();
					this.uiScreen = buildHomeScreen(gc);
				});
			});
		});

		uiScreen.getChildren().add(backButton);
		uiScreen.getChildren().add(roomField);
		uiScreen.getChildren().add(connect);
		return uiScreen;
	}

	private void resize(double w, double h, Canvas canvas){
		WIDTH = w;
		HEIGHT = h;
		LANDSCAPE = w > h;
		SQUARE_SIZE = LANDSCAPE ? (int)Math.min(HEIGHT/8*0.6, WIDTH*0.05) : (int)(WIDTH*0.09);
		SPACE = new Point2D(LANDSCAPE ? WIDTH*0.15 : WIDTH*0.18, (HEIGHT-SQUARE_SIZE*8)/2);
		canvas.setWidth(w);
		canvas.setHeight(h);
		this.uiScreen.setRect(new Rectangle2D(LANDSCAPE ? SPACE.getX()*1.5+SQUARE_SIZE*8 : SPACE.getX()*1.5, SPACE.getY(), SQUARE_SIZE*6, SQUARE_SIZE*8));
	}

	private Point2D getClickPoint(double x, double y){
		return new Point2D(x-SPACE.getX(), y-SPACE.getY());
	}
	
	private boolean makeUserMove(String not, int x, int y, boolean skipAnimation, String promType){
		boolean isProm = isPromotion(this.currentSelection, not);
		String prom = isProm ? promType : null;
		this.animation = new PieceAnimation(this.currentSelection, not, () -> {
			boolean ok = this.board.move(this.currentSelection, not, prom);
			if (this.httpServer != null){
				this.httpServer.sendMove(MainApplication.format("%s;%s;%s;%s", this.board.getTime(this.viewPoint), this.currentSelection, not, prom));
			}
			this.moveStart = this.currentSelection;
			this.moveEnd = not;
			this.currentSelection = null;
			this.currentMoves = null;
			this.hold.clear();
			this.animation = null;
			this.gameFinished = this.board.isGameFinished();
			calculateEval();
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
		int[] pos = Board.convertNotation(a);
		Piece piece = this.board.getBoard()[pos[0]][pos[1]];
		if (piece == null){ // This might be a premove
			int[] pos2 = Board.convertNotation(b);
			if (pos2[1] < pos[1]){
				for (int i = 0; i < 8; i++){
					Piece p = this.board.getBoard()[pos[0]][i];
					if (p != null && p.getType().getName() == Piece.PIECE_PAWN){
						piece = p;
						break;
					}
				}
			} else if (pos2[1] > pos[1]){
				for (int i = 7; i >= 0; i--){
					Piece p = this.board.getBoard()[pos[0]][i];
					if (p != null && p.getType().getName() == Piece.PIECE_PAWN){
						piece = p;
						break;
					}
				}
			}
		}
		if (piece == null) return false; // If it's still null ...
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
	
	private void reset(String fen, long time, int inc){
		this.board = new Board(fen, time, inc);
		this.gameFinished = this.board.isGameFinished();
		this.moveStart = null;
		this.moveEnd = null;
		this.hold.clear();
		this.premoves.clear();
		this.currentHold = null;
		this.currentMoves = null;
		this.gameOverText = null;
	}
	
	private void makeEngineMove(boolean game){
		if (this.gameFinished) return;
		Scheduler.scheduleDelay(1000, () -> {
			this.engine.getBestMove(this.board, output -> {
				if (output == null) return;
				this.animation = new PieceAnimation(output.split(" ")[0], output.split(" ")[1], () -> {
					String prom = output.split(" ").length == 3 ? output.split(" ")[2] : null;
					this.board.move(output.split(" ")[0], output.split(" ")[1], prom);
					this.hold.clear();
					this.currentSelection = null;
					this.moveStart = output.split(" ")[0];
					this.moveEnd = output.split(" ")[1];
					this.animation = null;
					this.gameFinished = this.board.isGameFinished();
					calculateEval();
					makePremove();
					if (game) makeEngineMove(true);
				});
				this.animation.start();
			});
		});		
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
				if (this.httpServer != null){
					this.httpServer.sendMove(MainApplication.format("%s;%s;%s;%s", this.board.getTime(this.viewPoint), pre.startPos, pre.endPos, pre.prom));
				}
				calculateEval();
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
		if (diff < 0) gc.fillText(Integer.toString(-diff), WIDTH*0.135, this.viewPoint == Color.WHITE ? wd : bd);
		if (diff > 0) gc.fillText(Integer.toString(diff), WIDTH*0.135, this.viewPoint == Color.WHITE ? bd : wd);
		
		// Captured pieces
		List<Piece> black = this.board.getMaterialList(Color.BLACK);
		List<Piece> white = this.board.getMaterialList(Color.WHITE);
		gc.save();
		for (int i = 0; i < black.size(); i++){
			Piece piece = black.get(i);
			Piece prev = i == 0 ? null : black.get(i-1);
			if (i > 0) gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.135, this.viewPoint == Color.WHITE ? wd : bd, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
		}
		gc.restore();
		gc.save();
		for (int i = 0; i < white.size(); i++){
			Piece piece = white.get(i);
			Piece prev = i == 0 ? null : white.get(i-1);
			if (i > 0) gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? SQUARE_SIZE/4.0 : SQUARE_SIZE/2.0+SQUARE_SIZE/10.0, 0);
			gc.drawImage(piece.getImage(), WIDTH*0.135, this.viewPoint == Color.WHITE ? bd : wd, SQUARE_SIZE/2.0, SQUARE_SIZE/2.0);
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
		
		gc.save();
		gc.setTextAlign(TextAlignment.RIGHT);
		gc.fillText((this.httpServer == null ? "" : "Room: "+this.httpServer.getRoom()+", ")+"Eval: "+this.eval, WIDTH*0.95, HEIGHT*0.9);
		gc.restore();
		
		// Moves played
		int count = 0;
		double wMove = SQUARE_SIZE*2;
		double hMove = SQUARE_SIZE*0.75;
		int movesAmount = LANDSCAPE ? (int)(HEIGHT/hMove) : (int)(WIDTH/wMove);
		for (int i = Math.max(this.board.getMoves().size()-movesAmount, 0); i < this.board.getMoves().size(); i++){
			gc.setStroke(Color.BLACK);
			gc.setFill(i % 2 == 0 ? Color.web("#F58B23") : Color.web("#7D4711"));
			double xp, yp;
			if (LANDSCAPE){
				xp = 10;
				yp = 30+(count++)*hMove;
			} else {
				xp = 10+(count++)*wMove;
				yp = 30;
			}
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
		gc.setLineWidth(2);
		gc.strokeRoundRect(timeX, wd-timeHeight, timeWidth, timeHeight, 7, 7);
		gc.strokeRoundRect(timeX, bd, timeWidth, timeHeight, 7, 7);
		gc.setFill(Color.BLACK);
		String topText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.BLACK)) : formatTime(this.board.getTime(Color.WHITE));
		String bottomText = this.viewPoint == Color.WHITE ? formatTime(this.board.getTime(Color.WHITE)) : formatTime(this.board.getTime(Color.BLACK));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setFont(new Font("sans-serif", timeHeight*0.5));
		gc.fillText(topText, timeX+timeWidth/2, wd-timeHeight+timeHeight*0.65);
		gc.fillText(bottomText, timeX+timeWidth/2, bd+timeHeight*0.65);
		gc.restore();
		
		if (this.board.getTime(Color.WHITE) == 0 || this.board.getTime(Color.BLACK) == 0) this.gameFinished = true;
		if (this.gameFinished){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(SPACE.getX(), SPACE.getY(), SQUARE_SIZE*8, SQUARE_SIZE*8);
			gc.restore();
			if (this.gameFinished){
				if (this.httpServer != null) this.httpServer.stop();
				this.httpServer = null;
				if (this.gameOverText == null){
					this.gameOverText = this.board.getGameFinishedMessage();
				} else {
					gc.save();
					gc.setFill(Color.WHITE);
					gc.setFont(new Font("sans-serif", SQUARE_SIZE*0.5));
					gc.setTextAlign(TextAlignment.CENTER);
					gc.fillText(this.gameOverText, SPACE.getX()+SQUARE_SIZE*4, SPACE.getY()+SQUARE_SIZE*4);
					gc.restore();
				}
			}
		}

		// UI
		this.uiScreen.setDisabled(!this.gameFinished && (this.board.getMoves().size() > 0 || this.httpServer != null));
		if (!showBoard || LANDSCAPE) this.uiScreen.render();
		
		if (!this.gameFinished) this.board.tick();
	}
	
	private static String formatTime(int time){
		int h = time / (60*60*1000);
		int m = time % (60*60*1000) / (60*1000);
		int s = ((time % (60*60*1000)) / 1000) % 60;
		int ms = time % (60*60*1000) % 1000;
		String text = "";
		String msText = m == 0 && s < 30 ? MainApplication.format(".%03d", ms) : "";
		if (h > 0){
			return MainApplication.format("%d:%d:%02d", h, m, s)+msText;
		} else {
			return MainApplication.format("%d:%02d", m, s)+msText;
		}
	}

	private static void loadSounds(){
		MOVE_SOUND = new Media(Resource.toUrl("/audio/move.mp3", MainApplication.class));
		CAPTURE_SOUND = new Media(Resource.toUrl("/audio/capture.mp3", MainApplication.class));
		CASTLE_SOUND = new Media(Resource.toUrl("/audio/castle.mp3", MainApplication.class));
		CHECK_SOUND = new Media(Resource.toUrl("/audio/move-check.mp3", MainApplication.class));
		ILLEGAL_SOUND = new Media(Resource.toUrl("/audio/illegal.mp3", MainApplication.class));
		PROMOTE_SOUND = new Media(Resource.toUrl("/audio/promote.mp3", MainApplication.class));
		CHECKMATE_SOUND = new Media(Resource.toUrl("/audio/game-end.mp3", MainApplication.class));
		DRAW_SOUND = new Media(Resource.toUrl("/audio/game-draw.mp3", MainApplication.class));
	}

	private static void loadImages(){
		PLAY_BLACK_IMAGE = new Image(Resource.toUrl("/images/button_playblack.png", MainApplication.class));
		PLAY_WHITE_IMAGE = new Image(Resource.toUrl("/images/button_playwhite.png", MainApplication.class));
		LAN_IMAGE = new Image(Resource.toUrl("/images/button_lan.png", MainApplication.class));
		SERVER_IMAGE = new Image(Resource.toUrl("/images/button_server.png", MainApplication.class));
		TIME_IMAGE = new Image(Resource.toUrl("/images/button_timecontrol.png", MainApplication.class));
		SINGLE_IMAGE = new Image(Resource.toUrl("/images/button_playstockfish.png", MainApplication.class));
		MULTI_IMAGE = new Image(Resource.toUrl("/images/button_playboard.png", MainApplication.class));
		BACK_IMAGE = new Image(Resource.toUrl("/images/button_back.png", MainApplication.class));
		CONNECT_CLIENT_IMAGE = new Image(Resource.toUrl("/images/button_connectclient.png", MainApplication.class));
		START_SERVER_IMAGE = new Image(Resource.toUrl("/images/button_startserver.png", MainApplication.class));
		EDIT_IMAGE = new Image(Resource.toUrl("/images/button_editboard.png", MainApplication.class));
		SAVE_IMAGE = new Image(Resource.toUrl("/images/button_save.png", MainApplication.class));
		HTTP_IMAGE = new Image(Resource.toUrl("/images/button_http.png", MainApplication.class));
	}
	
	public static void playSound(Media media){
		AudioClip player = new AudioClip(media.getSource());
		player.play();
	}

	public static String format(String text, Object... objects){
		// %s %d %0<x>d
		char[] chars = text.toCharArray();
		StringBuilder output = new StringBuilder();
		int count = 0;
		for (int i = 0; i < chars.length; i++){
			char c = chars[i];
			if (c == '%'){
				char next = chars[i+1];
				if (next == 's' || next == 'd'){
					output.append(objects[count]);
					i += 1;
				} else if (next == '0'){
					if (objects[count] != null){
						int n = Integer.parseInt(String.valueOf(chars[i+2]));
						for (int j = 0; j < n-objects[count].toString().length(); j++){
							output.append("0");
						}
					}
					output.append(objects[count]);
					i += 3;
				}
				count++;
			} else {
				output.append(c);
			}
		}
		return output.toString();
	}
	
	public static void main(String[] args){
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
