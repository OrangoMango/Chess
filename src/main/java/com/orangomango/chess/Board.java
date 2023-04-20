package com.orangomango.chess;

import java.util.*;

public class Board{
	private Piece[][] board;
	
	public Board(){
		this.board = new Piece[8][8];
		setupBoard();
	}
	
	public void setupBoard(){
		// Pawns
		for (int i = 0; i < 8; i++){
			this.board[i][1] = new Piece(Piece.Pieces.PAWN, Piece.COLOR_BLACK, i, 1);
			this.board[i][6] = new Piece(Piece.Pieces.PAWN, Piece.COLOR_WHITE, i, 6);
		}
		
		// Rooks
		this.board[0][0] = new Piece(Piece.Pieces.ROOK, Piece.COLOR_BLACK, 0, 0);
		this.board[7][0] = new Piece(Piece.Pieces.ROOK, Piece.COLOR_BLACK, 7, 0);
		this.board[0][7] = new Piece(Piece.Pieces.ROOK, Piece.COLOR_WHITE, 0, 7);
		this.board[7][7] = new Piece(Piece.Pieces.ROOK, Piece.COLOR_WHITE, 7, 7);
		
		// Knights
		this.board[1][0] = new Piece(Piece.Pieces.KNIGHT, Piece.COLOR_BLACK, 1, 0);
		this.board[6][0] = new Piece(Piece.Pieces.KNIGHT, Piece.COLOR_BLACK, 6, 0);
		this.board[1][7] = new Piece(Piece.Pieces.KNIGHT, Piece.COLOR_WHITE, 1, 7);
		this.board[6][7] = new Piece(Piece.Pieces.KNIGHT, Piece.COLOR_WHITE, 6, 7);
		
		// Bishops
		this.board[2][0] = new Piece(Piece.Pieces.BISHOP, Piece.COLOR_BLACK, 2, 0);
		this.board[5][0] = new Piece(Piece.Pieces.BISHOP, Piece.COLOR_BLACK, 5, 0);
		this.board[2][7] = new Piece(Piece.Pieces.BISHOP, Piece.COLOR_WHITE, 2, 7);
		this.board[5][7] = new Piece(Piece.Pieces.BISHOP, Piece.COLOR_WHITE, 5, 7);
		
		// Queens
		this.board[3][0] = new Piece(Piece.Pieces.QUEEN, Piece.COLOR_BLACK, 3, 0);
		this.board[3][7] = new Piece(Piece.Pieces.QUEEN, Piece.COLOR_WHITE, 3, 7);
		
		// Kings
		this.board[4][0] = new Piece(Piece.Pieces.KING, Piece.COLOR_BLACK, 4, 0);
		this.board[4][7] = new Piece(Piece.Pieces.KING, Piece.COLOR_WHITE, 4, 7);
	}
	
	public void move(String pos1, String pos){
		int[] p1 = convertPosition(pos1);
		int[] p2 = convertPosition(pos);
		
		Piece piece = this.board[p1[0]][p1[1]];
		int extraMove = 0;
		if (piece.getType().getName().equals(Piece.PIECE_PAWN)){
			if ((piece.getColor() == Piece.COLOR_WHITE && p2[1] > piece.getY()) || (piece.getColor() == Piece.COLOR_BLACK && p2[1] < piece.getY())){
				return;
			} else if ((piece.getColor() == Piece.COLOR_WHITE ? piece.getY()-1 : piece.getY()+1) == p2[1] && Math.abs(piece.getX()-p2[0]) == 1){
				Piece capture = this.board[p2[0]][p2[1]];
				if (capture == null){
					return;
				} else {
					this.board[p1[0]][p1[1]] = null;
					this.board[p2[0]][p2[1]] = piece;
					piece.setPos(p2[0], p2[1]);
				}
			} else if (piece.getX() != p2[0]){
				return;
			}
			if ((piece.getColor() == Piece.COLOR_WHITE && piece.getY() == 6) || (piece.getColor() == Piece.COLOR_BLACK && piece.getY() == 1)){
				extraMove = 1;
			}
		}
		int[] dir = piece.getType().getDirections();
		for (int i = 0; i < dir.length; i++){			
			int[][] comb = null;
			if (dir[i] == Piece.MOVE_DIAGONAL){
				comb = new int[][]{{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
			} else if (dir[i] == Piece.MOVE_HORIZONTAL){
				comb = new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
			} else if (dir[i] == Piece.MOVE_KNIGHT){
				comb = new int[][]{{-2, -1}, {-1, -2}, {2, -1}, {1, -2}, {1, 2}, {2, 1}, {-1, 2}, {-2, 1}};
			}
			if (comb != null){
				for (int c = 0; c < 4; c++){
					Piece lastPiece = null;
					for (int j = 1; j <= piece.getType().getAmount()+extraMove; j++){
						int x = piece.getX()+comb[c][0]*j;
						int y = piece.getY()+comb[c][1]*j;
						String res = convertNotation(x, y);
						if (res != null){
							if (this.board[x][y] != null){
								lastPiece = this.board[x][y];
							}
							if (res.equals(pos)){
								if (lastPiece == null){
									this.board[p1[0]][p1[1]] = null;
									this.board[x][y] = piece;
									piece.setPos(x, y);
								} else if (lastPiece.getColor() != piece.getColor()){
									this.board[p1[0]][p1[1]] = null;
									this.board[lastPiece.getX()][lastPiece.getY()] = piece;
									piece.setPos(lastPiece.getX(), lastPiece.getY());
								}
							}
						}
					}
				}
			}
		}
	}
	
	private int[] convertPosition(String pos){
		char[] c = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
		char[] data = pos.toCharArray();
		int x = -1;
		for (int i = 0; i < 8; i++){
			if (c[i] == data[0]){
				x = i;
				break;
			}
		}
		int y = 8-Integer.parseInt(String.valueOf(data[1]));
		if (x < 0 || y < 0 || y > 7){
			return null;
		} else {
			return new int[]{x, y};
		}
	}
	
	private String convertNotation(int x, int y){
		char[] c = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
		if (x < 0 || y < 0 || x >= 8 || y >= 8){
			return null;
		} else {
			return c[x]+Integer.toString(8-y);
		}
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 8; i++){ // y
			for (int j = 0; j < 8; j++){ // x
				builder.append(this.board[j][i]+" ");
			}
			builder.append("\n");
		}
		return builder.toString();
	}
}
