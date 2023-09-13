package com.orangomango.chess.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

import java.util.*;

public class UiScreen{
	private GraphicsContext gc;
	private Rectangle2D rect;
	private List<UiObject> children = new ArrayList<>();
	private boolean disabled;

	public UiScreen(GraphicsContext gc, Rectangle2D rect){
		this.gc = gc;
		this.rect = rect;
	}

	public List<UiObject> getChildren(){
		return this.children;
	}

	public void setRect(Rectangle2D rect){
		this.rect = rect;
	}

	public Rectangle2D getRect(){
		return this.rect;
	}

	public void setDisabled(boolean v){
		this.disabled = v;
	}

	public boolean isDisabled(){
		return this.disabled;
	}

	public void render(){
		gc.save();
		gc.setGlobalAlpha(0.85);
		gc.setFill(Color.BLUE);
		gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		gc.restore();

		gc.save();
		gc.translate(this.rect.getMinX(), this.rect.getMinY());
		for (UiObject obj : this.children){
			obj.render();
		}
		gc.restore();

		if (this.disabled){
			gc.save();
			gc.setFill(Color.BLACK);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
			gc.restore();
		}
	}
}