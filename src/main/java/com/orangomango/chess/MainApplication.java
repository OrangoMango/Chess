package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.*;
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
		// rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
		// 5Rk1/p3r1pp/4N1b1/1p1pP3/3p4/7P/PP4P1/6K1 b - - 2 29
		this.board = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		this.engine = new Engine();
		stage.setOnCloseRequest(e -> engine.writeCommand("quit"));
		
		canvas.setOnMousePressed(e -> {
			if (this.gameFinished) return;
			int x = (int)(e.getX()/75);
			int y = (int)((e.getY()-100)/75);
			String not = Board.convertPosition(x, y);
			if (not != null){
				if (this.currentSelection != null){
					if (this.board.move(this.currentSelection, not)){
						String output = engine.getBestMove(board.getFEN(), 100);
						this.board.move(output.split(" ")[0], output.split(" ")[1]);
					}
					this.currentSelection = null;
					this.currentMoves = null;
				} else if (this.board.getBoard()[x][y] != null){
					this.currentSelection = not;
					this.currentMoves = this.board.getValidMoves(this.board.getBoard()[x][y]);
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
		
		stage.setResizable(false);
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
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
				Piece piece = pieces[i][j];
				if (piece != null){
					if (piece.getType().getName() == Piece.PIECE_KING){
						if ((piece.getColor() == Color.WHITE && this.board.getCheckingPieces(Color.WHITE).size() > 0) || (piece.getColor() == Color.BLACK && this.board.getCheckingPieces(Color.BLACK).size() > 0)){
							gc.setFill(Color.BLUE);
							gc.fillOval(i*75, j*75, 75, 75);
						}
					}
					gc.drawImage(piece.getImage(), i*75, j*75);
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
		
		gc.setFill(Color.RED);
		gc.setFont(new Font("Sans-serif", 15));
		gc.fillText(String.format("FPS: %d\n%s", fps, this.board.getBoardInfo()), 30, 230);
		
		if (this.gameFinished){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(0, 0, WIDTH, HEIGHT);
			gc.restore();
		}
		gc.restore();
		
		gc.setFill(Color.BLACK);
		gc.fillText(Integer.toString(this.board.getMaterial(Color.BLACK)), 30, 50);
		gc.fillText(Integer.toString(this.board.getMaterial(Color.WHITE)), 30, 750);
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
