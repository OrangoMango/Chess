package com.orangomango.chess;

import javafx.geometry.Point2D;
import javafx.animation.*;
import javafx.util.Duration;

public class PieceAnimation{
	private String start, end;
	private Runnable onFinish;
	private volatile Point2D pos;
	private int animationTime = 15;
	
	public PieceAnimation(String start, String end, Runnable r){
		this.start = start;
		this.end = end;
		this.onFinish = r;
		this.pos = new Point2D(Board.convertNotation(this.start)[0], Board.convertNotation(this.start)[1]);
	}
	
	public void setAnimationTime(int time){
		this.animationTime = time;
	}
	
	public void start(){
		int[] startPoint = Board.convertNotation(this.start);
		int[] endPoint = Board.convertNotation(this.end);
		Timeline loop = new Timeline(new KeyFrame(Duration.millis(this.animationTime), e -> {
			this.pos = new Point2D(this.pos.getX()+(endPoint[0]-startPoint[0])/10.0, this.pos.getY()+(endPoint[1]-startPoint[1])/10.0);
		}));
		loop.setCycleCount(10);
		loop.setOnFinished(e -> this.onFinish.run());
		loop.play();
	}
	
	public String getStartNotation(){
		return this.start;
	}
	
	public String getEndNotation(){
		return this.end;
	}
	
	public Point2D getPosition(){
		return this.pos;
	}
}
