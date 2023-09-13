package com.orangomango.chess.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

public abstract class UiObject{
	protected GraphicsContext gc;
	protected Rectangle2D rect;
	protected UiScreen screen;

	public UiObject(UiScreen screen, GraphicsContext gc, Rectangle2D rect){
		this.gc = gc;
		this.rect = rect;
		this.screen = screen;
	}

	protected Rectangle2D getAbsoluteRect(){
		return new Rectangle2D(this.rect.getMinX()*this.screen.getRect().getWidth(), this.rect.getMinY()*this.screen.getRect().getHeight(), this.rect.getWidth()*this.screen.getRect().getWidth(), this.rect.getHeight()*this.screen.getRect().getWidth());
	}

	public abstract void render();
}