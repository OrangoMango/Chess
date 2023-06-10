package com.orangomango.chess;

import javafx.scene.paint.Color;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

public class Board{
	private Piece[][] board;
	private boolean blackRightCastleAllowed = true, whiteRightCastleAllowed = true;
	private boolean blackLeftCastleAllowed = true, whiteLeftCastleAllowed = true;
	private List<Piece> blackCaptured = new ArrayList<>();
	private List<Piece> whiteCaptured = new ArrayList<>();
	private int whiteExtraMaterial, blackExtraMaterial;
	private List<Piece> whiteChecks = new ArrayList<>();
	private List<Piece> blackChecks = new ArrayList<>();
	private Piece whiteKing, blackKing;
	private Color player = Color.WHITE;
	private int fifty = 0, movesN = 1;
	private String enPassant = null;
	private boolean[] canCastle = new boolean[4];
	private Map<String, Integer> states = new HashMap<>();
	private List<String> moves = new ArrayList<>();
	public String playerA = System.getProperty("user.name"), playerB = "BLACK";
	private volatile long whiteTime, blackTime;
	private long lastTime, gameTime;
	private int increment;

	public Board(String fen, long time, int increment){
		this.board = new Piece[8][8];
		setupBoard(fen);
		this.gameTime = time;
		this.increment = increment;
		this.whiteTime = time;
		this.blackTime = time;
		this.lastTime = System.currentTimeMillis();
	}

	public void tick(){
		if (this.movesN == 1){
			this.lastTime = System.currentTimeMillis();
			return;
		}
		long time = System.currentTimeMillis()-this.lastTime;
		if (this.player == Color.WHITE){
			this.whiteTime -= time;
		} else {
			this.blackTime -= time;
		}
		this.whiteTime = Math.max(this.whiteTime, 0);
		this.blackTime = Math.max(this.blackTime, 0);
		this.lastTime = System.currentTimeMillis();
	}

	public long getGameTime(){
		return this.gameTime;
	}

	public int getIncrementTime(){
		return this.increment;
	}

	private void setupBoard(String fen){
		String[] data = fen.split(" ");
		this.whiteKing = null;
		this.blackKing = null;
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
		
		this.canCastle[0] = canCastleLeft(Color.WHITE);
		this.canCastle[1] = canCastleRight(Color.WHITE);
		this.canCastle[2] = canCastleLeft(Color.BLACK);
		this.canCastle[3] = canCastleRight(Color.BLACK);
		
		String ep = String.valueOf(data[3]);
		if (!ep.equals("-")) this.enPassant = ep;
		
		this.fifty = Integer.parseInt(String.valueOf(data[4]));
		this.movesN = Integer.parseInt(String.valueOf(data[5]));
		
		this.states.clear();
		this.states.put(getFEN().split(" ")[0], 1);
		
		setupCaptures(Color.WHITE);
		setupCaptures(Color.BLACK);

		this.blackChecks.clear();
		this.whiteChecks.clear();
		for (Piece boardPiece : getPiecesOnBoard()){
			List<String> newLegalMoves = getLegalMoves(boardPiece);
			if (boardPiece.getColor() == Color.WHITE && newLegalMoves.contains(convertPosition(this.blackKing.getX(), this.blackKing.getY()))){
				this.blackChecks.add(boardPiece);
			} else if (boardPiece.getColor() == Color.BLACK && newLegalMoves.contains(convertPosition(this.whiteKing.getX(), this.whiteKing.getY()))){
				this.whiteChecks.add(boardPiece);
			}
		}

		if (this.whiteKing == null || this.blackKing == null) throw new IllegalStateException("Missing king");
		if (getAttackers(this.player == Color.WHITE ? this.blackKing : this.whiteKing) != null) throw new IllegalStateException("King is in check");
	}
	
	private void setupCaptures(Color color){
		List<Piece> pieces = getPiecesOnBoard().stream().filter(piece -> piece.getColor() == color).collect(Collectors.toList());
		List<Piece> captures = color == Color.WHITE ? this.blackCaptured : this.whiteCaptured;
		int pawns = (int)pieces.stream().filter(piece -> piece.getType().getName() == Piece.PIECE_PAWN).count();
		int rooks = (int)pieces.stream().filter(piece -> piece.getType().getName() == Piece.PIECE_ROOK).count();
		int knights = (int)pieces.stream().filter(piece -> piece.getType().getName() == Piece.PIECE_KNIGHT).count();
		int bishops = (int)pieces.stream().filter(piece -> piece.getType().getName() == Piece.PIECE_BISHOP).count();
		int queens = (int)pieces.stream().filter(piece -> piece.getType().getName() == Piece.PIECE_QUEEN).count();
		for (int i = 0; i < 8-pawns; i++){
			captures.add(new Piece(Piece.Pieces.PAWN, color, -1, -1));
		}
		for (int i = 0; i < 2-rooks; i++){
			captures.add(new Piece(Piece.Pieces.ROOK, color, -1, -1));
		}
		for (int i = 0; i < 2-knights; i++){
			captures.add(new Piece(Piece.Pieces.KNIGHT, color, -1, -1));
		}
		for (int i = 0; i < 2-bishops; i++){
			captures.add(new Piece(Piece.Pieces.BISHOP, color, -1, -1));
		}
		if (queens == 0){
			captures.add(new Piece(Piece.Pieces.QUEEN, color, -1, -1));
		}
		
		for (int i = 0; i < -(2-rooks); i++){
			capture(new Piece(Piece.Pieces.PAWN, color, -1, -1));
			promote(color, Piece.Pieces.ROOK);
		}
		for (int i = 0; i < -(2-knights); i++){
			capture(new Piece(Piece.Pieces.PAWN, color, -1, -1));
			promote(color, Piece.Pieces.KNIGHT);
		}
		for (int i = 0; i < -(2-bishops); i++){
			capture(new Piece(Piece.Pieces.PAWN, color, -1, -1));
			promote(color, Piece.Pieces.BISHOP);
		}
		for (int i = 0; i < -(1-queens); i++){
			capture(new Piece(Piece.Pieces.PAWN, color, -1, -1));
			promote(color, Piece.Pieces.QUEEN);
		}
	}
	
	public int getMovesN(){
		return this.movesN;
	}

	public List<String> getMoves(){
		return this.moves;
	}
	
	public boolean move(String pos1, String pos, String prom){
		if (pos1 == null || pos == null) return false;
		int[] p1 = convertNotation(pos1);
		
		Piece piece = this.board[p1[0]][p1[1]];
		if (piece == null || piece.getColor() != this.player) return false;
		
		List<String> legalMoves = getLegalMoves(piece);
		if (legalMoves.contains(pos)){
			int[] p2 = convertNotation(pos);
			
			Piece[][] backup = createBackup();
			List<Piece> identical = new ArrayList<>();
			for (Piece p : getPiecesOnBoard()){
				if (p != piece && p.getType() == piece.getType() && p.getColor() == piece.getColor()){
					if (getValidMoves(p).contains(pos)) identical.add(p);
				}
			}
			
			Piece capture = this.board[p2[0]][p2[1]];
			if (this.enPassant != null && pos.equals(this.enPassant)){
				capture = this.board[p2[0]][p1[1]];
				this.board[capture.getX()][capture.getY()] = null;
			}
			this.board[p1[0]][p1[1]] = null;
			setPiece(piece, p2[0], p2[1]);
			
			if (getAttackers(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) != null){
				restoreBackup(backup);
				return false;
			}
			
			if (capture != null) capture(capture);
			
			boolean castle = false;
			if (piece.getType().getName() == Piece.PIECE_KING){
				if (Math.abs(p2[0]-p1[0]) == 2){
					if (p2[0] == 6){
						castleRight(piece.getColor());
						castle = true;
					} else if (p2[0] == 2){
						castleLeft(piece.getColor());
						castle = true;
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
					if (this.whiteRightCastleAllowed && p1[0] == 7){
						this.whiteRightCastleAllowed = false;
					} else if (this.whiteLeftCastleAllowed && p1[0] == 0){
						this.whiteLeftCastleAllowed = false;
					}
				} else {
					if (this.blackRightCastleAllowed && p1[0] == 7){
						this.blackRightCastleAllowed = false;
					} else if (this.blackLeftCastleAllowed && p1[0] == 0){
						this.blackLeftCastleAllowed = false;
					}
				}
			}
			
			if (piece.getType().getName() == Piece.PIECE_PAWN){
				this.fifty = 0;
				if ((piece.getColor() == Color.WHITE && piece.getY() == 0) || (piece.getColor() == Color.BLACK && piece.getY() == 7)){
					Piece.Pieces promotion = Piece.getType(prom);
					this.board[piece.getX()][piece.getY()] = new Piece(promotion, piece.getColor(), piece.getX(), piece.getY());
					capture(piece);
					promote(piece.getColor(), promotion);
				}
				if (Math.abs(p2[1]-p1[1]) == 2){
					this.enPassant = convertPosition(piece.getX(), piece.getColor() == Color.WHITE ? piece.getY()+1 : piece.getY()-1);
				} else {
					this.enPassant = null;
				}
			} else {
				this.fifty++;
				this.enPassant = null;
			}
			
			this.canCastle[0] = canCastleLeft(Color.WHITE);
			this.canCastle[1] = canCastleRight(Color.WHITE);
			this.canCastle[2] = canCastleLeft(Color.BLACK);
			this.canCastle[3] = canCastleRight(Color.BLACK);
			
			this.blackChecks.clear();
			this.whiteChecks.clear();
			boolean check = false;
			for (Piece boardPiece : getPiecesOnBoard()){
				List<String> newLegalMoves = getLegalMoves(boardPiece);
				if (boardPiece.getColor() == Color.WHITE && newLegalMoves.contains(convertPosition(this.blackKing.getX(), this.blackKing.getY()))){
					this.blackChecks.add(boardPiece);
					check = true;
				} else if (boardPiece.getColor() == Color.BLACK && newLegalMoves.contains(convertPosition(this.whiteKing.getX(), this.whiteKing.getY()))){
					this.whiteChecks.add(boardPiece);
					check = true;
				}
			}
			
			if (check){
				MainApplication.playSound(MainApplication.CHECK_SOUND);
			}

			if (this.movesN > 1){
				if (this.player == Color.WHITE){
					this.whiteTime += this.increment*1000;
				} else {
					this.blackTime += this.increment*1000;
				}
			}

			if (this.player == Color.BLACK) this.movesN++;
			this.player = this.player == Color.WHITE ? Color.BLACK : Color.WHITE;
			
			String fen = getFEN().split(" ")[0];
			this.states.put(fen, this.states.getOrDefault(fen, 0)+1);
			
			this.moves.add(moveToString(piece, pos1, pos, check, capture != null, prom, castle, identical));
			
			if (capture != null){
				MainApplication.playSound(MainApplication.CAPTURE_SOUND);
			} else if (castle){
				MainApplication.playSound(MainApplication.CASTLE_SOUND);
			} else {
				MainApplication.playSound(MainApplication.MOVE_SOUND);
			}

			MainApplication.vibrator.vibrate(125);

			return true;
		}
		return false;
	}
	
	private String moveToString(Piece piece, String start, String pos, boolean check, boolean capture, String prom, boolean castle, List<Piece> identical){
		int[] coord = convertNotation(start);
		String output = "";
		if (piece.getType().getName() == Piece.PIECE_PAWN){
			if (capture){
				output = String.valueOf(start.charAt(0))+"x"+pos;
			} else {
				output = pos;
			}
			if (prom != null) output += "="+prom.toUpperCase();
		} else if (castle){
			output = piece.getX() == 2 ? "O-O-O" : "O-O";
		} else {
			String extra = "";
			if (identical.size() >= 2){
				extra = start;
			} else if (identical.size() == 1){
				Piece other = identical.get(0);
				if (coord[0] != other.getX()){
					extra = String.valueOf(start.charAt(0));
				} else if (coord[1] != other.getY()){
					extra = String.valueOf(start.charAt(1));
				}
			}
			output = piece.getType().getName().toUpperCase()+extra+(capture ? "x" : "")+pos;
		}
		if (isCheckMate(piece.getColor() == Color.WHITE ? Color.BLACK : Color.WHITE)){
			output += "#";
		} else if (check){
			output += "+";
		}
		return output;
	}

	public int getTime(Color color){
		return color == Color.WHITE ? (int)this.whiteTime : (int)this.blackTime;
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
		return moved && getAttackers(color == Color.WHITE ? this.whiteKing : this.blackKing) == null && canCastle(new int[]{5, 6}, new int[]{5, 6}, color);
	}
	
	private boolean canCastleLeft(Color color){
		boolean moved = color == Color.WHITE ? this.whiteLeftCastleAllowed : this.blackLeftCastleAllowed;
		return moved && getAttackers(color == Color.WHITE ? this.whiteKing : this.blackKing) == null && canCastle(new int[]{2, 3}, new int[]{1, 2, 3}, color);
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
			if (getAttackers(king) != null){
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
			int oldY = piece.getY();
			this.board[piece.getX()][oldY] = null;
			setPiece(piece, pos[0], pos[1]);
			if (move.equals(this.enPassant)){
				int x = convertNotation(this.enPassant)[0];
				this.board[x][oldY] = null;

			}
			if (getAttackers(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) == null){
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
			if (this.enPassant != null && piece.getY() == convertNotation(this.enPassant)[1]+(piece.getColor() == Color.WHITE ? 1 : -1) && Math.abs(piece.getX()-convertNotation(this.enPassant)[0]) == 1){
				result.add(this.enPassant);
			}
		}
		if (piece.getType().getName() == Piece.PIECE_KING){
			if ((piece.getColor() == Color.WHITE && this.canCastle[0]) || (piece.getColor() == Color.BLACK && this.canCastle[2])){
				result.add(convertPosition(piece.getX()-2, piece.getY()));
			}
			if ((piece.getColor() == Color.WHITE && this.canCastle[1]) || (piece.getColor() == Color.BLACK && this.canCastle[3])){
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
	
	private void promote(Color color, Piece.Pieces type){
		List<Piece> list = color == Color.BLACK ? this.whiteCaptured : this.blackCaptured;
		Iterator<Piece> iterator = list.iterator();
		boolean removed = false;
		while (iterator.hasNext()){
			Piece piece = iterator.next();
			if (piece.getType() == type){
				iterator.remove();
				removed = true;
				break;
			}
		}
		if (!removed){
			if (color == Color.WHITE){
				this.whiteExtraMaterial += type.getValue();
			} else {
				this.blackExtraMaterial += type.getValue();
			}
		}
	}
	
	private List<Piece> getAttackers(Piece piece){
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
	
	private boolean canKingMove(Color color){
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		Piece[][] backup = createBackup();
		
		// Check if king has any legal moves
		for (int x = king.getX()-1; x < king.getX()+2; x++){
			for (int y = king.getY()-1; y < king.getY()+2; y++){
				if (x >= 0 && y >= 0 && x < 8 && y < 8){
					Piece piece = this.board[x][y];
					if (piece == null || piece.getColor() != king.getColor()){
						this.board[king.getX()][king.getY()] = null;
						setPiece(king, x, y);
						if (getAttackers(king) == null){
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
			List<Piece> canCapture = getAttackers(checks.get(0));
			if (canCapture != null){
				for (Piece piece : canCapture){
					this.board[piece.getX()][piece.getY()] = null;
					setPiece(piece, checks.get(0).getX(), checks.get(0).getY());
					if (getAttackers(king) == null){
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
						if (getAttackers(piece.getColor() == Color.WHITE ? this.whiteKing : this.blackKing) == null){
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
	
	public String getPGN(){
		StringBuilder builder = new StringBuilder();
		SimpleDateFormat format = new SimpleDateFormat("YYYY/MM/dd");
		Date date = new Date();
		builder.append("[Event \""+String.format("%s vs %s", this.playerA, this.playerB)+"\"]\n");
		builder.append("[Site \"com.orangomango.chess\"]\n");
		builder.append("[Date \""+format.format(date)+"\"]\n");
		builder.append("[Round \"1\"]\n");
		builder.append("[White \""+this.playerA+"\"]\n");
		builder.append("[Black \""+this.playerB+"\"]\n");
		String result = "*";
		if (isDraw()){
			result = "½-½";
		} else if (isCheckMate(Color.WHITE)){
			result = "0-1";
		} else if (isCheckMate(Color.BLACK)){
			result = "1-0";
		}
		builder.append("[Result \""+result+"\"]\n\n");
		for (int i = 0; i < this.moves.size(); i++){
			if (i % 2 == 0) builder.append((i/2+1)+". ");
			builder.append(this.moves.get(i)+" ");
		}
		builder.append(result);
		return builder.toString();
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
		
		boolean no = false;
		if (!this.whiteLeftCastleAllowed && !this.whiteRightCastleAllowed){
			builder.append("-");
			no = true;
		} else {
			if (this.whiteRightCastleAllowed) builder.append("K");
			if (this.whiteLeftCastleAllowed) builder.append("Q");
		}
		if (!this.blackLeftCastleAllowed && !this.blackRightCastleAllowed){
			if (!no) builder.append("-");
		} else {
			if (this.blackRightCastleAllowed) builder.append("k");
			if (this.blackLeftCastleAllowed) builder.append("q");
		}
		builder.append(" ");
		
		builder.append(String.format("%s %d %d", this.enPassant == null ? "-" : this.enPassant, this.fifty, this.movesN));
		
		return builder.toString();
	}
	
	private boolean isCheckMate(Color color){
		Piece king = color == Color.WHITE ? this.whiteKing : this.blackKing;
		return getAttackers(king) != null && !canKingMove(color);
	}
	
	private boolean isDraw(){
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
		boolean whiteDraw = whitePieces == 0 || (whitePieces == 3 && pieces.stream().filter(piece -> piece.getColor() == Color.WHITE).count() == 2);
		boolean blackDraw = blackPieces == 0 || (blackPieces == 3 && pieces.stream().filter(piece -> piece.getColor() == Color.BLACK).count() == 2);
		if (whiteDraw && blackDraw) return true;
		if ((getAttackers(this.blackKing) == null && !canKingMove(Color.BLACK) && blackLegalMoves.size() == 0 && this.player == Color.BLACK) || (getAttackers(this.whiteKing) == null && !canKingMove(Color.WHITE)) && whiteLegalMoves.size() == 0 && this.player == Color.WHITE){
			return true;
		}
		if (this.states.values().contains(3)) return true;
		return false;
	}

	public boolean isGameFinished(){
		return isCheckMate(Color.WHITE) || isCheckMate(Color.BLACK) || isDraw() || this.whiteTime <= 0 || this.blackTime <= 0;
	}
	
	public String getBoardInfo(){
		int whiteSum = getMaterial(Color.WHITE);
		int blackSum = getMaterial(Color.BLACK);
		return String.format("B:%d W:%d - BK:%s WK:%s - BCK:%s WCK:%s\nChecks: %s %s\n",
			blackSum, whiteSum, getAttackers(this.blackKing) != null, getAttackers(this.whiteKing) != null,
			isCheckMate(Color.BLACK), isCheckMate(Color.WHITE),
			this.blackChecks, this.whiteChecks);
	}
	
	public List<Piece> getMaterialList(Color color){
		List<Piece> list = new ArrayList<>(color == Color.WHITE ? this.whiteCaptured : this.blackCaptured);
		list.sort((p1, p2) -> Integer.compare(p1.getType().getValue(), p2.getType().getValue()));
		return list;
	}
	
	public int getMaterial(Color color){
		return getMaterialList(color).stream().mapToInt(p -> p.getType().getValue()).sum()+(color == Color.WHITE ? this.whiteExtraMaterial : this.blackExtraMaterial);
	}
	
	public Color getPlayer(){
		return this.player;
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
