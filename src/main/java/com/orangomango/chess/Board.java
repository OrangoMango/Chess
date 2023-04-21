package com.orangomango.chess;

import javafx.scene.paint.Color;
import java.util.*;
import java.util.stream.Collectors;

public class Board{
	private Piece[][] board;
	private boolean blackRightCastleAllowed = true, whiteRightCastleAllowed = true;
	private boolean blackLeftCastleAllowed = true, whiteLeftCastleAllowed = true;
	private List<Piece> blackCaptured = new ArrayList<>();
	private List<Piece> whiteCaptured = new ArrayList<>();
	private List<Piece> whiteChecks = new ArrayList<>();
	private List<Piece> blackChecks = new ArrayList<>();
	private Piece whiteKing, blackKing;
	
	public Board(){
		this.board = new Piece[8][8];
		setupBoard();
	}
	
	public void setupBoard(){
		// Pawns
		for (int i = 0; i < 8; i++){
			this.board[i][1] = new Piece(Piece.Pieces.PAWN, Color.BLACK, i, 1);
			this.board[i][6] = new Piece(Piece.Pieces.PAWN, Color.WHITE, i, 6);
		}
		
		// Rooks
		this.board[0][0] = new Piece(Piece.Pieces.ROOK, Color.BLACK, 0, 0);
		this.board[7][0] = new Piece(Piece.Pieces.ROOK, Color.BLACK, 7, 0);
		this.board[0][7] = new Piece(Piece.Pieces.ROOK, Color.WHITE, 0, 7);
		this.board[7][7] = new Piece(Piece.Pieces.ROOK, Color.WHITE, 7, 7);
		
		// Knights
		this.board[1][0] = new Piece(Piece.Pieces.KNIGHT, Color.BLACK, 1, 0);
		this.board[6][0] = new Piece(Piece.Pieces.KNIGHT, Color.BLACK, 6, 0);
		this.board[1][7] = new Piece(Piece.Pieces.KNIGHT, Color.WHITE, 1, 7);
		this.board[6][7] = new Piece(Piece.Pieces.KNIGHT, Color.WHITE, 6, 7);
		
		// Bishops
		this.board[2][0] = new Piece(Piece.Pieces.BISHOP, Color.BLACK, 2, 0);
		this.board[5][0] = new Piece(Piece.Pieces.BISHOP, Color.BLACK, 5, 0);
		this.board[2][7] = new Piece(Piece.Pieces.BISHOP, Color.WHITE, 2, 7);
		this.board[5][7] = new Piece(Piece.Pieces.BISHOP, Color.WHITE, 5, 7);
		
		// Queens
		this.board[3][0] = new Piece(Piece.Pieces.QUEEN, Color.BLACK, 3, 0);
		this.board[3][7] = new Piece(Piece.Pieces.QUEEN, Color.WHITE, 3, 7);
		
		// Kings
		this.blackKing = new Piece(Piece.Pieces.KING, Color.BLACK, 4, 0);
		this.whiteKing = new Piece(Piece.Pieces.KING, Color.WHITE, 4, 7);
		this.board[4][0] = this.blackKing;
		this.board[4][7] = this.whiteKing;
	}
	
	public void move(String pos1, String pos){
		int[] p1 = convertPosition(pos1);
		
		Piece piece = this.board[p1[0]][p1[1]];
		if (piece == null) return;
		
		List<String> legalMoves = getLegalMoves(piece);
		if (legalMoves.contains(pos)){
			int[] p2 = convertPosition(pos);
			
			Piece[][] backup = createBackup();
			
			Piece capture = this.board[p2[0]][p2[1]];
			this.board[p1[0]][p1[1]] = null;
			setPiece(piece, p2[0], p2[1]);
			
			if (canBeCaptured(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) != null){
				restoreBackup(backup);
				return;
			}
			
			if (capture != null) capture(capture);
			
			this.blackChecks.clear();
			this.whiteChecks.clear();
			for (Piece boardPiece : getPiecesOnBoard()){
				List<String> newLegalMoves = getLegalMoves(boardPiece);
				if (boardPiece.getColor() == Color.WHITE && newLegalMoves.contains(convertNotation(this.blackKing.getX(), this.blackKing.getY()))){
					this.blackChecks.add(boardPiece);
				} else if (piece.getColor() == Color.BLACK && newLegalMoves.contains(convertNotation(this.whiteKing.getX(), this.whiteKing.getY()))){
					this.whiteChecks.add(boardPiece);
				}
			}
			
			if (piece.getType().getName() == Piece.PIECE_KING){
				if (piece.getColor() == Color.WHITE){
					this.whiteRightCastleAllowed = false;
					this.whiteLeftCastleAllowed = false;
				} else {
					this.blackRightCastleAllowed = false;
					this.blackLeftCastleAllowed = false;
				}
			}
			
			if (piece.getType().getName() == Piece.PIECE_ROOK){
				if (piece.getColor() == Color.WHITE){
					if (this.whiteRightCastleAllowed && piece.getX() == 7){
						this.whiteRightCastleAllowed = false;
					} else if (this.whiteLeftCastleAllowed && piece.getX() == 0){
						this.whiteLeftCastleAllowed = false;
					}
				} else {
					if (this.blackRightCastleAllowed && piece.getX() == 7){
						this.blackRightCastleAllowed = false;
					} else if (this.blackLeftCastleAllowed && piece.getX() == 0){
						this.blackLeftCastleAllowed = false;
					}
				}
			}
		}
	}
	
	public List<String> getValidMoves(Piece piece){
		List<String> legalMoves = getLegalMoves(piece);
		List<String> validMoves = new ArrayList<>();
		Piece[][] backup = createBackup();
		for (String move : legalMoves){
			int[] pos = convertPosition(move);
			this.board[piece.getX()][piece.getY()] = null;
			setPiece(piece, pos[0], pos[1]);
			if (canBeCaptured(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) == null){
				restoreBackup(backup);
				validMoves.add(move);
			} else {
				restoreBackup(backup);
			}
		}
		return validMoves;
	}

	private List<String> getLegalMoves(Piece piece){
		List<String> result = new ArrayList<>();
		int extraMove = 0;
		if (piece.getType().getName() == Piece.PIECE_PAWN){
			if ((piece.getColor() == Color.WHITE && piece.getY() == 6) || (piece.getColor() == Color.BLACK && piece.getY() == 1)){
				extraMove = 1;
			}
			int factor = piece.getColor() == Color.WHITE ? -1 : 1;
			String not1 = convertNotation(piece.getX()-1, piece.getY()+factor);
			String not2 = convertNotation(piece.getX()+1, piece.getY()+factor);
			if (not1 != null && this.board[piece.getX()-1][piece.getY()+factor] != null && this.board[piece.getX()-1][piece.getY()+factor].getColor() != piece.getColor()) result.add(not1);
			if (not2 != null && this.board[piece.getX()+1][piece.getY()+factor] != null && this.board[piece.getX()+1][piece.getY()+factor].getColor() != piece.getColor()) result.add(not2);
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
				for (int c = 0; c < comb.length; c++){
					for (int j = 1; j <= piece.getType().getAmount()+extraMove; j++){
						int x = piece.getX()+comb[c][0]*j;
						int y = piece.getY()+comb[c][1]*j;
						if (piece.getType().getName() == Piece.PIECE_PAWN){
							if (x != piece.getX() || (piece.getColor() == Color.WHITE && y > piece.getY()) || (piece.getColor() == Color.BLACK && y < piece.getY())){
								continue;
							}
						}
						Piece captured = getPieceAt(x, y);
						String not = convertNotation(x, y);
						if (not != null && (captured == null || captured.getColor() != piece.getColor())){
							if (captured == null || piece.getType().getName() != Piece.PIECE_PAWN || captured.getX() != piece.getX()){
								result.add(not);
							}
						}
						if (captured != null){
							break;
						}
					}
				}
			}
		}
		return result;
	}
	
	private Piece getPieceAt(int x, int y){
		if (x >= 0 && y >= 0 && x < 8 && y < 8){
			return this.board[x][y];
		} else {
			return null;
		}
	}
	
	private void capture(Piece piece){
		if (piece.getColor() == Color.WHITE){
			this.blackCaptured.add(piece);
		} else {
			this.whiteCaptured.add(piece);
		}
	}
	
	private List<Piece> canBeCaptured(Piece piece){
		List<Piece> pieces = getPiecesOnBoard();
		List<Piece> result = new ArrayList<>();
		String pos = convertNotation(piece.getX(), piece.getY());
		for (Piece boardPiece : pieces){
			if (boardPiece.getColor() != piece.getColor() && getLegalMoves(boardPiece).contains(pos)){
				result.add(boardPiece);
			}
		}
		return result.size() == 0 ? null : result;
	}
	
	private boolean canKingMove(Piece king){
		Piece[][] backup = createBackup();
		
		// Check if king has any legal moves
		for (int x = king.getX()-1; x < king.getX()+2; x++){
			for (int y = king.getY()-1; y < king.getY()+2; y++){
				if (x >= 0 && y >= 0 && x < 8 && y < 8){
					Piece piece = this.board[x][y];
					if (piece == null || piece.getColor() != king.getColor()){
						this.board[king.getX()][king.getY()] = null;
						setPiece(king, x, y);
						if (canBeCaptured(king) == null){
							restoreBackup(backup);
							return true;
						} else {
							restoreBackup(backup);
						}
					}
				}
			}
		}
		
		// If there is a single check, check if the piece can be captured or the ckeck can be blocked
		List<Piece> checks = king.getColor() == Color.WHITE ? this.whiteChecks : this.blackChecks;
		if (checks.size() == 1){
			List<Piece> canCapture = canBeCaptured(checks.get(0));
			if (canCapture != null){
				for (Piece piece : canCapture){
					this.board[piece.getX()][piece.getY()] = null;
					setPiece(piece, checks.get(0).getX(), checks.get(0).getY());
					if (canBeCaptured(king) == null){
						restoreBackup(backup);
						return true;
					} else {
						restoreBackup(backup);
					}
				}
			} else {
				List<String> legalMoves = getLegalMoves(checks.get(0));
				List<Piece> pieces = getPiecesOnBoard();
				for (Piece piece : pieces){
					if (piece.getColor() == checks.get(0).getColor()) continue;
					Set<String> intersection = getLegalMoves(piece).stream().distinct().filter(legalMoves::contains).collect(Collectors.toSet());
					List<String> allowed = new ArrayList<>();
					for (String move : intersection){
						int[] pos = convertPosition(move);
						this.board[piece.getX()][piece.getY()] = null;
						setPiece(piece, pos[0], pos[1]);
						if (canBeCaptured(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) == null){
							restoreBackup(backup);
							allowed.add(move);
						} else {
							restoreBackup(backup);
						}
					}
					if (allowed.size() > 0){
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private Piece[][] createBackup(){
		Piece[][] backup = new Piece[8][8];
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				backup[i][j] = this.board[i][j];
			}
		}
		return backup;
	}
	
	private void restoreBackup(Piece[][] backup){
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				Piece piece = backup[i][j];
				if (piece == null){
					this.board[i][j] = null;
				} else {
					setPiece(piece, i, j);
				}
			}
		}
	}
	
	private List<Piece> getPiecesOnBoard(){
		List<Piece> pieces = new ArrayList<>();
		for (int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				if (this.board[i][j] != null) pieces.add(this.board[i][j]);
			}
		}
		return pieces;
	}
	
	private void setPiece(Piece piece, int x, int y){
		this.board[x][y] = piece;
		piece.setPos(x, y);
	}
	
	public static int[] convertPosition(String pos){
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
	
	public static String convertNotation(int x, int y){
		char[] c = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
		if (x < 0 || y < 0 || x > 7 || y > 7){
			return null;
		} else {
			return c[x]+Integer.toString(8-y);
		}
	}
	
	public List<Piece> getCheckingPieces(Color color){
		if (color == Color.WHITE){
			return this.whiteChecks;
		} else {
			return this.blackChecks;
		}
	}
	
	public Piece[][] getBoard(){
		return this.board;
	}
	
	public String getBoardInfo(){
		int whiteSum = 0;
		int blackSum = 0;
		for (Piece p : this.whiteCaptured){
			whiteSum += p.getType().getValue();
		}
		for (Piece p : this.blackCaptured){
			blackSum += p.getType().getValue();
		}
		return String.format("B:%d W:%d - BK:%s WK:%s - BCK:%s WCK:%s\nChecks: %s %s\n",
			blackSum, whiteSum, canBeCaptured(this.blackKing) != null, canBeCaptured(this.whiteKing) != null,
			canBeCaptured(this.blackKing) != null && !canKingMove(this.blackKing), canBeCaptured(this.whiteKing) != null && !canKingMove(this.whiteKing),
			this.blackChecks, this.whiteChecks);
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
		builder.append(getBoardInfo());
		return builder.toString();
	}
}
