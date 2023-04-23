package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class MainApplication extends Application{
	private static final double WIDTH = 600;
	private static final double HEIGHT = 800;
	private volatile int frames, fps;
	private static final int FPS = 40;
	
	private Board board;
	private Engine engine;
	private String currentSelection;
	private List<String> currentMoves;
	private boolean gameFinished = false;
	private volatile String eval;
	private volatile PieceAnimation animation;
	
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

		stage.setTitle("Chess");
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);
		// startpos: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
		this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		this.engine = new Engine();
		
		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.gameFinished) return;
				int x = (int)(e.getX()/75);
				int y = (int)((e.getY()-100)/75);
				String not = Board.convertPosition(x, y);
				if (not != null){
					if (this.currentSelection != null){
						this.animation = new PieceAnimation(this.currentSelection, not, () -> {
							boolean ok = this.board.move(this.currentSelection, not);
							this.currentSelection = null;
							this.currentMoves = null;
							this.animation = null;
							//if (ok) makeEngineMove();
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
			} else if (e.getButton() == MouseButton.MIDDLE){
				makeEngineMove();
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
	}
	
	private void makeEngineMove(){
		new Thread(() -> {
			String output = this.engine.getBestMove(board.getFEN(), 150);
			if (output != null){
				this.animation = new PieceAnimation(output.split(" ")[0], output.split(" ")[1], () -> {
					this.board.move(output.split(" ")[0], output.split(" ")[1]);
					this.animation = null;
				});
				this.animation.start();
			}
		}).start();
	}
	
	private void update(GraphicsContext gc){
		this.gameFinished = this.board.isCheckMate(Color.WHITE) || this.board.isCheckMate(Color.BLACK) || this.board.isDraw();
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.CYAN);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.save();
		gc.translate(0, 100);
		Piece[][] pieces = this.board.getBoard();
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				gc.setFill((i+7*j) % 2 == 0 ? Color.WHITE : Color.GREEN);
				if (this.currentSelection != null){
					int[] pos = Board.convertNotation(this.currentSelection);
					if (i == pos[0] && j == pos[1]){
						gc.setFill(Color.RED);
					}
				}
				gc.fillRect(i*75, j*75, 75, 75);
			}
		}
		
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				Piece piece = pieces[i][j];
				Point2D pos = new Point2D(i, j);
				if (this.animation != null && this.animation.getStartNotation().equals(Board.convertPosition(i, j))){
					pos = this.animation.getPosition();
				}
				if (piece != null){
					if (piece.getType().getName() == Piece.PIECE_KING){
						if ((piece.getColor() == Color.WHITE && this.board.getCheckingPieces(Color.WHITE).size() > 0) || (piece.getColor() == Color.BLACK && this.board.getCheckingPieces(Color.BLACK).size() > 0)){
							gc.setFill(Color.BLUE);
							gc.fillOval(pos.getX()*75, pos.getY()*75, 75, 75);
						}
					}
					gc.drawImage(piece.getImage(), pos.getX()*75, pos.getY()*75);
				}
			}
		}
		
		if (this.currentMoves != null){
			for (String move : this.currentMoves){
				int[] pos = Board.convertNotation(move);
				gc.setFill(this.board.getBoard()[pos[0]][pos[1]] == null ? Color.YELLOW : Color.BLUE);
				gc.fillOval(pos[0]*75+25, pos[1]*75+25, 25, 25);
			}
		}
		
		//gc.setFill(Color.RED);
		//gc.setFont(new Font("Sans-serif", 15));
		//gc.fillText(String.format("FPS: %d\n%s", fps, this.board.getBoardInfo()), 30, 230);
		
		if (this.gameFinished){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
		}
		gc.restore();
		
		gc.setFill(Color.BLACK);
		int bm = this.board.getMaterial(Color.BLACK);
		int wm = this.board.getMaterial(Color.WHITE);
		int diff = wm-bm;
		if (diff < 0) gc.fillText(Integer.toString(-diff), 30, 50);
		if (diff > 0) gc.fillText(Integer.toString(diff), 30, 750);
		
		List<Piece> black = this.board.getMaterialList(Color.BLACK);
		List<Piece> white = this.board.getMaterialList(Color.WHITE);
		gc.save();
		for (int i = 0; i < black.size(); i++){
			Piece piece = black.get(i);
			Piece prev = i == 0 ? null : black.get(i-1);
			gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? 15 : 33, 0);
			gc.drawImage(piece.getImage(), 30, 60, 30, 30);
		}
		gc.restore();
		gc.save();
		for (int i = 0; i < white.size(); i++){
			Piece piece = white.get(i);
			Piece prev = i == 0 ? null : white.get(i-1);
			gc.translate(prev != null && prev.getType().getValue() == piece.getType().getValue() ? 15 : 33, 0);
			gc.drawImage(piece.getImage(), 30, 750, 30, 30);
		}
		gc.restore();
		
		gc.fillText("Eval: "+this.eval, 500, 770);
	}
	
	public static void main(String[] args) throws IOException{
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
