package com.orangomango.chess;

import javafx.scene.paint.Color;

public class Piece{
	public static final int MOVE_DIAGONAL = 0;
	public static final int MOVE_HORIZONTAL = 1;
	public static final int MOVE_KNIGHT = 2;
	
	public static final String PIECE_PAWN = "p";
	public static final String PIECE_ROOK = "r";
	public static final String PIECE_KNIGHT = "n";
	public static final String PIECE_BISHOP = "b";
	public static final String PIECE_KING = "k";
	public static final String PIECE_QUEEN = "q";

	public static enum Pieces{
		PAWN(PIECE_PAWN, 1, 1, MOVE_HORIZONTAL),
		ROOK(PIECE_ROOK, 8, 5, MOVE_HORIZONTAL),
		KNIGHT(PIECE_KNIGHT, 1, 3, MOVE_KNIGHT),
		BISHOP(PIECE_BISHOP, 8, 3, MOVE_DIAGONAL),
		KING(PIECE_KING, 1, -1, MOVE_HORIZONTAL, MOVE_DIAGONAL),
		QUEEN(PIECE_QUEEN, 8, 9, MOVE_HORIZONTAL, MOVE_DIAGONAL);
		
		private String name;
		private int[] directions;
		private int amount;
		private int value;
		
		private Pieces(String name, int amount, int value, int... directions){
			this.name = name;
			this.amount = amount;
			this.directions = directions;
			this.value = value;
		}
		
		public String getName(){
			return this.name;
		}
		
		public int getAmount(){
			return this.amount;
		}
		
		public int getValue(){
			return this.value;
		}
		
		public int[] getDirections(){
			return this.directions;
		}
	}
	
	private Pieces type;
	private Color color;
	private int x, y;
	
	public Piece(Pieces type, Color color, int x, int y){
		this.type = type;
		this.color = color;
		this.x = x;
		this.y = y;
	}
	
	public void setPos(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public int getX(){
		return this.x;
	}
	
	public int getY(){
		return this.y;
	}
	
	public Pieces getType(){
		return this.type;
	}
	
	public Color getColor(){
		return this.color;
	}
	
	@Override
	public String toString(){
		return String.format("%s(%d)", this.type.getName(), this.color == Color.WHITE ? 0 : 1);
	}
}
