package com.orangomango.chess;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.*;

public class MainApplication extends Application{
	private static final double WIDTH = 600;
	private static final double HEIGHT = 600;
	private volatile int frames, fps;
	private static final int FPS = 40;
	
	@Override
	public void start(Stage stage){
		Thread counter = new Thread(() -> {
			while (true){
				try {
					this.fps = Math.min(this.frames, FPS);
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
		
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		
		gc.setFill(Color.WHITE);
		gc.fillText(String.format("FPS: %d", fps), 30, 30);
	}
	
	public static void main(String[] args) throws IOException{
		//launch(args);
		
		Board board = new Board();
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
		reader.close();
		System.exit(0);
	}
}
