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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import com.eviware.loadui.ui.fx.util.test.FXScreenController;
import com.eviware.loadui.ui.fx.util.test.FXTestUtils;
import com.eviware.loadui.ui.fx.util.test.TestFX;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.GroupBuilder;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.loadui.test.categories.GUITest;
import com.google.common.util.concurrent.SettableFuture;

@Category( GUITest.class )
public class ConfirmationDialogTest
{
	private static final SettableFuture<Stage> stageFuture = SettableFuture.create();
	private static Stage stage;
	private static TestFX controller;
	private static Dialog dialog;
	private static Button openDialogButton;
	protected static final Logger log = LoggerFactory.getLogger( ConfirmationDialogTest.class );

	public static class ConfirmationDialogTestApp extends Application
	{
		@Override
		public void start( Stage primaryStage ) throws Exception
		{
			openDialogButton = new Button( "Open dialog" );
			openDialogButton.setOnAction( new EventHandler<ActionEvent>()
			{
				@Override
				public void handle( ActionEvent arg0 )
				{
					dialog.show();
				}
			} );

			primaryStage.setScene( SceneBuilder.create().width( 800 ).height( 600 )
					.root( GroupBuilder.create().children( openDialogButton ).build() ).build() );

			dialog = new ConfirmationDialog( openDialogButton, "My dialog", "I got it!" );

			primaryStage.show();

			stageFuture.set( primaryStage );
		}

	}

	@BeforeClass
	public static void createWindow() throws Throwable
	{
		controller = TestFX.wrap( new FXScreenController() );
		FXTestUtils.launchApp( ConfirmationDialogTestApp.class );
		stage = stageFuture.get( 5, TimeUnit.SECONDS );
		TestFX.targetWindow( stage );
		FXTestUtils.bringToFront( stage );
	}

	@Test
	public void shouldOpen()
	{
		assertFalse( dialog.isShowing() );
		controller.click( openDialogButton );
		assertTrue( dialog.isShowing() );
	}

	@Test
	public void shouldCloseOnCancel()
	{
		controller.click( openDialogButton ).target( dialog ).click( "#cancel" );
		assertFalse( dialog.isShowing() );
	}
}
