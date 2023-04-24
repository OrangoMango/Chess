package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import javafx.scene.media.*;

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
	private boolean onTheBoard = false;
	private boolean engineMove = false;
	
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

		stage.setTitle("Chess - "+(this.viewPoint == Color.WHITE ? "WHITE" : "BLACK"));
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);
		// startpos: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
		this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		this.engine = new Engine();
		
		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.onTheBoard)) return;
				int x = (int)(e.getX()/SQUARE_SIZE);
				int y = (int)((e.getY()-SPACE)/SQUARE_SIZE);
				if (this.viewPoint == Color.BLACK){
					x = 7-x;
					y = 7-y;
				}
				String not = Board.convertPosition(x, y);
				if (not != null){
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
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				System.out.println(this.board.getFEN());
				System.out.println(this.board.getPGN());
			} else if (e.getButton() == MouseButton.MIDDLE){
				if (this.gameFinished || (this.board.getPlayer() != this.viewPoint && !this.onTheBoard)) return;
				makeEngineMove();
			}
			
		});
		
		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.S){
				Server server = new Server("192.168.1.247", 1234);
			} else if (e.getCode() == KeyCode.C){
				this.client = new Client("192.168.1.247", 1234, this.viewPoint);
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
			} else if (e.getCode() == KeyCode.R){
				this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
			} else if (e.getCode() == KeyCode.A){
				this.onTheBoard = !this.onTheBoard;
			} else if (e.getCode() == KeyCode.Z){
				this.engineMove = !this.engineMove;
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
			try {
				while (true){
					this.eval = this.engine.getEval(this.board.getFEN());
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
		
		/*new Thread(() -> {
			try {
				Thread.sleep(1000);
				while (!this.gameFinished){
					makeEngineMove();
					Thread.sleep(1000);
				}
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		}).start();*/
	}
	
	private void makeEngineMove(){
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
		
		gc.fillText("Eval: "+this.eval, WIDTH*0.7, HEIGHT-SPACE*0.7);
		
		if (this.gameFinished){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
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
