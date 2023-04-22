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
	private Color player = Color.WHITE;
	private int fifty = 0, moves = 1;
	private String enPassant = null;
	private boolean[] canCastle = new boolean[4];
	
	public Board(String fen){
		this.board = new Piece[8][8];
		canCastle[0] = true;
		canCastle[1] = true;
		canCastle[2] = true;
		canCastle[3] = true;
		setupBoard(fen);
	}
	
	public void setupBoard(String fen){
		String[] data = fen.split(" ");
		int r = 0;
		for (String row : data[0].split("/")){
			char[] p = row.toCharArray();
			int pos = 0;
			for (int i = 0; i < 8; i++){
				try {
					int n = Integer.parseInt(Character.toString(p[pos]));
					i += n-1;
				} catch (NumberFormatException ex){
					Piece piece = new Piece(Piece.getType(String.valueOf(p[pos])), Character.isUpperCase(p[pos]) ? Color.WHITE : Color.BLACK, i, r);
					this.board[i][r] = piece;
					if (piece.getType().getName() == Piece.PIECE_KING){
						if (piece.getColor() == Color.WHITE){
							this.whiteKing = piece;
						} else {
							this.blackKing = piece;
						}
					}
				}
				pos++;
			}
			r++;
		}
		
		this.player = data[1].equals("w") ? Color.WHITE : Color.BLACK;
		char[] c = data[2].toCharArray();
		for (int i = 0; i < c.length; i++){
			if (i == 0 && String.valueOf(c[i]).equals("-")){
				this.whiteLeftCastleAllowed = false;
				this.whiteRightCastleAllowed = false;
				if (c.length == 1){
					this.blackLeftCastleAllowed = false;
					this.blackRightCastleAllowed = false;
				}
			} else if (String.valueOf(c[i]).equals("-")){
				this.blackLeftCastleAllowed = false;
				this.blackRightCastleAllowed = false;
			} else {
				String d = String.valueOf(c[i]);
				if (d.equals("Q")) this.whiteLeftCastleAllowed = true;
				if (d.equals("K")) this.whiteRightCastleAllowed = true;
				if (d.equals("q")) this.blackLeftCastleAllowed = true;
				if (d.equals("k")) this.blackRightCastleAllowed = true;
			}
		}
		
		this.canCastle = new boolean[]{this.whiteLeftCastleAllowed, this.whiteRightCastleAllowed, this.blackLeftCastleAllowed, this.blackRightCastleAllowed};
		
		String ep = String.valueOf(data[3]);
		if (!ep.equals("-")) this.enPassant = ep;
		
		this.fifty = Integer.parseInt(String.valueOf(data[4]));
		this.moves = Integer.parseInt(String.valueOf(data[5]));
	}
	
	public boolean move(String pos1, String pos){
		int[] p1 = convertNotation(pos1);
		
		Piece piece = this.board[p1[0]][p1[1]];
		if (piece == null || piece.getColor() != this.player) return false;
		
		List<String> legalMoves = getLegalMoves(piece);
		if (legalMoves.contains(pos)){
			int[] p2 = convertNotation(pos);
			
			Piece[][] backup = createBackup();
			
			Piece capture = this.board[p2[0]][p2[1]];
			if (this.enPassant != null && pos.equals(this.enPassant)){
				capture = this.board[p2[0]][p1[1]];
				this.board[capture.getX()][capture.getY()] = null;
			}
			this.board[p1[0]][p1[1]] = null;
			setPiece(piece, p2[0], p2[1]);
			
			if (canBeCaptured(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) != null){
				restoreBackup(backup);
				return false;
			}
			
			if (capture != null) capture(capture);
			
			if (piece.getType().getName() == Piece.PIECE_KING){
				if (Math.abs(p2[0]-p1[0]) == 2){
					if (p2[0] == 6){
						castleRight(piece.getColor());
					} else if (p2[0] == 2){
						castleLeft(piece.getColor());
					}
				}
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
			
			if (piece.getType().getName() == Piece.PIECE_PAWN){
				this.fifty = 0;
				if ((piece.getColor() == Color.WHITE && piece.getY() == 0) || (piece.getColor() == Color.BLACK && piece.getY() == 7)){
					this.board[piece.getX()][piece.getY()] = new Piece(Piece.Pieces.QUEEN, piece.getColor(), piece.getX(), piece.getY());
				}
				if (Math.abs(p2[1]-p1[1]) == 2){
					this.enPassant = convertPosition(piece.getX(), piece.getColor() == Color.WHITE ? piece.getY()+1 : piece.getY()-1);
				} else {
					this.enPassant = null;
				}
			} else this.fifty++;
			
			if (piece.getColor() == Color.WHITE){
				canCastle[0] = canCastleLeft(Color.WHITE);
				canCastle[1] = canCastleRight(Color.WHITE);
			} else {
				canCastle[2] = canCastleLeft(Color.BLACK);
				canCastle[3] = canCastleRight(Color.BLACK);
			}
			
			this.blackChecks.clear();
			this.whiteChecks.clear();
			for (Piece boardPiece : getPiecesOnBoard()){
				List<String> newLegalMoves = getLegalMoves(boardPiece);
				if (boardPiece.getColor() == Color.WHITE && newLegalMoves.contains(convertPosition(this.blackKing.getX(), this.blackKing.getY()))){
					this.blackChecks.add(boardPiece);
				} else if (piece.getColor() == Color.BLACK && newLegalMoves.contains(convertPosition(this.whiteKing.getX(), this.whiteKing.getY()))){
					this.whiteChecks.add(boardPiece);
				}
			}

			if (this.player == Color.BLACK) this.moves++;
			this.player = this.player == Color.WHITE ? Color.BLACK : Color.WHITE;
			
			return true;
		}
		return false;
	}
	
	public void castleRight(Color color){
		int ypos = color == Color.WHITE ? 7 : 0;
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		Piece rook = this.board[7][ypos];
		if (canCastleRight(color)){
			this.board[rook.getX()][rook.getY()] = null;
			setPiece(rook, king.getX()-1, king.getY());
		}
	}
	
	public void castleLeft(Color color){
		int ypos = color == Color.WHITE ? 7 : 0;
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		Piece rook = this.board[0][ypos];
		if (canCastleLeft(color)){
			this.board[rook.getX()][rook.getY()] = null;
			setPiece(rook, king.getX()+1, king.getY());
		}
	}
	
	private boolean canCastleRight(Color color){
		boolean moved = color == Color.WHITE ? this.whiteRightCastleAllowed : this.blackRightCastleAllowed;
		return moved && canCastle(new int[]{5, 6}, new int[]{5, 6}, color);
	}
	
	private boolean canCastleLeft(Color color){
		boolean moved = color == Color.WHITE ? this.whiteLeftCastleAllowed : this.blackLeftCastleAllowed;
		return moved && canCastle(new int[]{2, 3}, new int[]{1, 2, 3}, color);
	}
	
	private boolean canCastle(int[] xpos, int[] checkXpos, Color color){
		int ypos = color == Color.WHITE ? 7 : 0;
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		for (int i = 0; i < xpos.length; i++){
			if (this.board[xpos[i]][ypos] != null && this.board[xpos[i]][ypos].getType().getName() != Piece.PIECE_KING) return false;
		}
		
		Piece[][] backup = createBackup();
		for (int i = 0; i < checkXpos.length; i++){
			this.board[king.getX()][king.getY()] = null;
			setPiece(king, checkXpos[i], ypos);
			if (canBeCaptured(king) != null){
				restoreBackup(backup);
				return false;
			}
			restoreBackup(backup);
		}
		
		return true;
	}
	
	public List<String> getValidMoves(Piece piece){
		List<String> legalMoves = getLegalMoves(piece);
		List<String> validMoves = new ArrayList<>();
		if (piece.getColor() != this.player) return validMoves;
		Piece[][] backup = createBackup();
		for (String move : legalMoves){
			int[] pos = convertNotation(move);
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
			String not1 = convertPosition(piece.getX()-1, piece.getY()+factor);
			String not2 = convertPosition(piece.getX()+1, piece.getY()+factor);
			if (not1 != null && this.board[piece.getX()-1][piece.getY()+factor] != null && this.board[piece.getX()-1][piece.getY()+factor].getColor() != piece.getColor()) result.add(not1);
			if (not2 != null && this.board[piece.getX()+1][piece.getY()+factor] != null && this.board[piece.getX()+1][piece.getY()+factor].getColor() != piece.getColor()) result.add(not2);
			
			// En passant
			if (this.enPassant != null && piece.getY() == convertNotation(this.enPassant)[1]+(piece.getColor() == Color.WHITE ? 1 : -1)){
				result.add(this.enPassant);
			}
		}
		if (piece.getType().getName() == Piece.PIECE_KING){
			if ((piece.getColor() == Color.WHITE && this.canCastle[0]) || (piece.getColor() == Color.BLACK && this.canCastle[2])){
				result.add(convertPosition(piece.getX()-2, piece.getY()));
			} else if ((piece.getColor() == Color.WHITE && this.canCastle[1]) || (piece.getColor() == Color.BLACK && this.canCastle[3])){
				result.add(convertPosition(piece.getX()+2, piece.getY()));
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
						String not = convertPosition(x, y);
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
		this.fifty = 0;
	}
	
	private List<Piece> canBeCaptured(Piece piece){
		List<Piece> pieces = getPiecesOnBoard();
		List<Piece> result = new ArrayList<>();
		String pos = convertPosition(piece.getX(), piece.getY());
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
						int[] pos = convertNotation(move);
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
	
	public static int[] convertNotation(String pos){
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
	
	public static String convertPosition(int x, int y){
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
	
	public String getFEN(){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 8; i++){
			int empty = 0;
			for (int j = 0; j < 8; j++){
				Piece piece = this.board[j][i];
				if (piece == null){
					empty++;
				} else {
					if (empty > 0){
						builder.append(empty);
						empty = 0;
					}
					builder.append(piece.getColor() == Color.WHITE ? piece.getType().getName().toUpperCase() : piece.getType().getName().toLowerCase());
				}
			}
			if (empty > 0) builder.append(empty);
			if (i < 7) builder.append("/");
		}
		
		builder.append(this.player == Color.WHITE ? " w " : " b ");
		
		if (!this.whiteLeftCastleAllowed && !this.whiteRightCastleAllowed){
			builder.append("-");
		} else {
			if (this.whiteRightCastleAllowed) builder.append("K");
			if (this.whiteLeftCastleAllowed) builder.append("Q");
		}
		if (!this.blackLeftCastleAllowed && !this.blackRightCastleAllowed){
			builder.append("-");
		} else {
			if (this.blackRightCastleAllowed) builder.append("k");
			if (this.blackLeftCastleAllowed) builder.append("q");
		}
		builder.append(" ");
		
		builder.append(String.format("%s %d %d", this.enPassant == null ? "-" : this.enPassant, this.fifty, this.moves));
		
		return builder.toString();
	}
	
	public boolean isCheckMate(Color color){
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		return canBeCaptured(king) != null && !canKingMove(king);
	}
	
	public boolean isDraw(){
		if (this.fifty >= 50) return true;
		List<Piece> pieces = getPiecesOnBoard();
		int whitePieces = pieces.stream().filter(piece -> piece.getColor() == Color.WHITE).mapToInt(p -> p.getType().getValue()).sum();
		int blackPieces = pieces.stream().filter(piece -> piece.getColor() == Color.BLACK).mapToInt(p -> p.getType().getValue()).sum();
		List<String> whiteLegalMoves = new ArrayList<>();
		List<String> blackLegalMoves = new ArrayList<>();
		for (Piece piece : pieces){
			if (piece.getColor() == Color.WHITE){
				whiteLegalMoves.addAll(getValidMoves(piece));
			} else {
				blackLegalMoves.addAll(getValidMoves(piece));
			}
		}
		boolean whiteDraw = whitePieces == 0 || whitePieces == 3;
		boolean blackDraw = blackPieces == 0 || blackPieces == 3;
		if (whiteDraw && blackDraw) return true;
		if ((canBeCaptured(this.blackKing) == null && !canKingMove(this.blackKing) && blackLegalMoves.size() == 0 && this.player == Color.BLACK) || (canBeCaptured(this.whiteKing) == null && !canKingMove(this.whiteKing)) && whiteLegalMoves.size() == 0 && this.player == Color.WHITE){
			return true;
		}
		return false;
	}
	
	public String getBoardInfo(){
		int whiteSum = this.whiteCaptured.stream().mapToInt(p -> p.getType().getValue()).sum();
		int blackSum = this.blackCaptured.stream().mapToInt(p -> p.getType().getValue()).sum();
		return String.format("B:%d W:%d - BK:%s WK:%s - BCK:%s WCK:%s\nChecks: %s %s\n",
			blackSum, whiteSum, canBeCaptured(this.blackKing) != null, canBeCaptured(this.whiteKing) != null,
			isCheckMate(Color.BLACK), isCheckMate(Color.WHITE),
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
