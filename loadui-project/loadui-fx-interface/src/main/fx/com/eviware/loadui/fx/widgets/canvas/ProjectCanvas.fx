/* 
 * Copyright 2010 eviware software ab
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
/*
*ProjectCanvas.fx
*
*Created on apr 29, 2010, 10:01:12 fm
*/

package com.eviware.loadui.fx.widgets.canvas;

import com.eviware.loadui.fx.FxUtils.*;
import com.eviware.loadui.fx.ui.dnd.Draggable;
import com.eviware.loadui.fx.widgets.toolbar.ComponentToolbarItem;

import com.eviware.loadui.api.model.ModelItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.api.model.ComponentItem;
import com.eviware.loadui.api.events.CollectionEvent;

import com.eviware.loadui.fx.ui.popup.PopupMenu;
import com.eviware.loadui.fx.ui.popup.SeparatorMenuItem;
import com.eviware.loadui.fx.ui.popup.ActionMenuItem;
import com.eviware.loadui.fx.ui.popup.SubMenuItem;

import java.util.EventObject;
import java.lang.RuntimeException;

import com.eviware.loadui.fx.ui.dialogs.Dialog;
import com.eviware.loadui.fx.MainWindow;
import javafx.scene.text.Text;

import com.eviware.loadui.fx.widgets.toolbar.TestCaseToolbarItem;
import javafx.util.Math;
import java.io.File;
import javafx.scene.Group;

import com.eviware.loadui.fx.util.ImageUtil.*;

public class ProjectCanvas extends Canvas {

	def projectItem:ProjectItem = bind lazy canvasItem as ProjectItem;
	
	override var canvasItem on replace oldCanvas { 
		if(canvasItem != null and not (canvasItem instanceof ProjectItem)) 
			throw new RuntimeException( "ProjectCanvas can only take a ProjectItem!" ) ;
		
		if (canvasItem != null) {
			for(testcase in projectItem.getScenes()){
				addTestCase( testcase );
			}
			refreshTerminals();
		}
	}
	
	public function generateMiniatures() {
	    var projectRef;
	    for(pRef in MainWindow.instance.workspace.getProjectRefs()){
			if(pRef.isEnabled() and pRef.getProject() == canvasItem){
				projectRef= pRef;
				break;
			}
		}
		
		var minX: Number = java.lang.Long.MAX_VALUE;
		var maxX: Number = 0;
		var minY: Number = java.lang.Long.MAX_VALUE;
		var maxY: Number = 0;
		for(c in components){
			if(c.layoutX + c.width > maxX){
				maxX = c.layoutX + c.width;
			}
			if(c.layoutY + c.height > maxY){
				maxY = c.layoutY + c.height;
			}
			if(c.layoutX < minX){
				minX = c.layoutX;
			}
			if(c.layoutY < minY){
				minY = c.layoutY;
			}
		}
		
		if(maxX == 0){
			maxX = 10;
		}
		
		if(maxY == 0){
			maxY = 10;
		}
		
		if(minX == java.lang.Long.MAX_VALUE){
			minX = 0;
		}
		
		if(minY == java.lang.Long.MAX_VALUE){
			minY = 0;
		}
		
		var connImg = nodeToImage(connectionLayer, maxX + 100, maxY + 100);
		var compImg = nodeToImage(componentLayer, maxX + 100, maxY + 100);
	    var img = combineImages(connImg, compImg);
	    img = clipImage(img, minX, minY, maxX + 100 - minX, maxY + 100 - minY);
	    var scale = Math.max(155/img.getWidth(), 0.1);
	    img = scaleImage(img, scale);
	    img = clipImage(img, 0, 0, Math.min(155 - 18 - 4, img.getWidth()), Math.min(100 - 37 - 4, img.getHeight()));

		var base64: String = bufferedImageToBase64(img);
		projectRef.getProject().setAttribute("miniature", base64);
	    projectRef.setAttribute("miniature", base64);
	    
	}
	
	override function acceptFunction( d:Draggable ) {
		if (d instanceof TestCaseToolbarItem) {
			true;
		} else {
			super.acceptFunction( d );
		}
	}
	
	override function onDropFunction( d:Draggable ) {
		//TODO: If 'TestCaseToolbarItem', create a TestCase instead.
		if (d instanceof TestCaseToolbarItem) {
			def sb = d.node.localToScene(d.node.layoutBounds);
			def x = sb.minX;
			def y = sb.minY;
			log.debug( "TestCase dropped at: (\{\}, \{\})", x, y );
			def testcase = createTestCase();
			testcase.setAttribute( "gui.layoutX", "{offsetX + x as Integer}" );
			testcase.setAttribute( "gui.layoutY", "{offsetY + y as Integer}" );
		} else {
			super.onDropFunction( d );
		}
	}
	
	function createTestCase():SceneItem {
		var name = "TestCase";
		var i=0;
		while( sizeof projectItem.getScenes()[c|c.getLabel() == name] > 0 )
			name = "TestCase ({++i})";
			
		if (not MainWindow.instance.workspace.isLocalMode()) {
		    def warning:Dialog = Dialog {
		    	title: "Warning!"
		    	content: Text {
		    		content: "Switch to local mode, or place {name} on an agent in order to run it"
		    	}
		    	okText: "Ok"
		    	onOk: function() {
		    		warning.close();
		    	}
		    	noCancel: true
		    	width: 400
		    	height: 150
		    }
		}

		log.debug( "Creating SceneItem using label: \{\}", name );
		
		projectItem.createScene( name );
		
	}

	function addTestCase( testCase:SceneItem ):Void {
		log.debug( "Adding SceneItem \{\}", testCase );
		def tstc = TestCaseNode.create( testCase, this );
		insert tstc into componentLayer.content;
	}

	override function handleEvent( e:EventObject ) {
		super.handleEvent( e );
		
		if( e instanceof CollectionEvent ) {
			def event = e as CollectionEvent;
			if( ProjectItem.SCENES.equals( event.getKey() ) ) {
				if( event.getEvent() == CollectionEvent.Event.ADDED ) {
					runInFxThread( function() { addTestCase( event.getElement() as SceneItem ); refreshComponents(); } );
				} else {
					runInFxThread( function() { removeModelItem( event.getElement() as SceneItem ); refreshComponents(); } );
				}
			}
		}
	}
}
