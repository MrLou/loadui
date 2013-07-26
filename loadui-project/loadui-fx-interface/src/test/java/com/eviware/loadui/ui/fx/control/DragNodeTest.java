/*
 * Copyright 2013 SmartBear Software
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.ui.fx.control;

import com.eviware.loadui.test.categories.GUITest;
import com.eviware.loadui.ui.fx.api.input.DraggableEvent;
import com.eviware.loadui.ui.fx.util.test.FXScreenController;
import com.eviware.loadui.ui.fx.util.test.FXTestUtils;
import com.eviware.loadui.ui.fx.util.test.GuiTest;
import com.eviware.loadui.ui.fx.views.window.Overlay;
import com.eviware.loadui.ui.fx.views.window.OverlayHolder;
import com.google.common.util.concurrent.SettableFuture;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.GroupBuilder;
import javafx.scene.Node;
import javafx.scene.SceneBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static javafx.beans.binding.Bindings.when;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(GUITest.class)
public class DragNodeTest
{
	private static final SettableFuture<Stage> stageFuture = SettableFuture.create();
	private static DragNode dragNode;
	private static Stage stage;
	private static GuiTest controller;

	public static class DragNodeTestApp extends Application
	{
		@Override
		public void start( Stage primaryStage ) throws Exception
		{
			Rectangle dragRect = RectangleBuilder.create().id( "dragrect" ).width( 25 ).height( 25 ).layoutX( 80 )
					.fill( Color.BLUE ).build();

			dragNode = DragNode.install( dragRect, RectangleBuilder.create().id( "dragnode" ).width( 25 ).height( 25 )
					.fill( Color.GREEN ).build() );

			Rectangle dropRect = RectangleBuilder.create().id( "droprect" ).width( 50 ).height( 50 ).layoutX( 100 )
					.layoutY( 100 ).build();
			dropRect.fillProperty()
					.bind( when( dragNode.acceptableProperty() ).then( Color.GREEN ).otherwise( Color.RED ) );

			Group root = GroupBuilder.create().children( dropRect, dragRect ).build();
			final RootNode s = new RootNode( root );
			primaryStage.setScene( SceneBuilder.create().width( 300 ).height( 200 ).root( s ).build() );

			primaryStage.show();

			stageFuture.set( primaryStage );
		}
	}

	@BeforeClass
	public static void createWindow() throws Throwable
	{
		controller = GuiTest.wrap( new FXScreenController() );
		FXTestUtils.launchApp( DragNodeTestApp.class );
		stage = stageFuture.get( 5, TimeUnit.SECONDS );
		GuiTest.targetWindow( stage );
		FXTestUtils.bringToFront( stage );
	}

	@Test
	public void shouldDragAndRelease()
	{
		assertFalse( dragNode.isDragging() );

		GuiTest.MouseMotion dragging = controller.drag( dragNode.getDragSource() ).by( 200, 50 );

		assertTrue( dragNode.isDragging() );

		dragging.drop();

		assertFalse( dragNode.isDragging() );
	}

	@Test
	public void shouldAcceptAndDrop() throws InterruptedException
	{
		Node dropArea = stage.getScene().lookup( "#droprect" );
		final CountDownLatch dropLatch = new CountDownLatch( 1 );

		dropArea.addEventHandler( DraggableEvent.ANY, new EventHandler<DraggableEvent>()
		{
			@Override
			public void handle( DraggableEvent event )
			{
				System.out.println( "RECEIVING EVENT" );
				if( event.getEventType() == DraggableEvent.DRAGGABLE_ENTERED )
				{
					event.accept();
				}
				else if( event.getEventType() == DraggableEvent.DRAGGABLE_DROPPED )
				{
					dropLatch.countDown();
				}
				event.consume();
			}
		} );

		assertFalse( dragNode.isAcceptable() );

		GuiTest.MouseMotion dragging = controller.drag( dragNode.getDragSource() ).via( dropArea );

		assertTrue( dragNode.isAcceptable() );

		dragging.drop();

		dropLatch.await( 2, TimeUnit.SECONDS );
	}

	static class RootNode extends Group implements OverlayHolder
	{
		private Overlay overlay = new Overlay(); //GroupBuilder.create().id( "overlay" ).build();

		RootNode( Node child )
		{
			getChildren().addAll( child, overlay );
		}

		@Override
		public Overlay getOverlay()
		{
			return overlay;
		}
	}
}
