/*
*MenubarButton.fx
*
*Created on jun 17, 2010, 15:24:16 em
*/

package com.eviware.loadui.fx.ui.menu;

import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.LayoutInfo;

def armedColor = Color.rgb( 0xb2, 0xb2, 0xb2 );
def unarmedColor = Color.rgb( 0x66, 0x66, 0x66 );
def shadowArmedColor = Color.TRANSPARENT;
def shadowUnarmedColor = Color.rgb( 0xb1, 0xb1, 0xb1 );

public class MenubarButton extends Button {
	public var shape:String;
	
	override var styleClass = "menubar-button";
	
	override var graphic = Group {
		content: [
			SVGPath { layoutX: 2, layoutY: 2, content: bind shape, fill: bind if( armed ) shadowArmedColor else shadowUnarmedColor },
			SVGPath { layoutX: 1, layoutY: 1, content: bind shape, fill: bind if( armed ) armedColor else unarmedColor },
		]
	}
	
	override var layoutInfo = LayoutInfo { width: 24, height: 24 }
}