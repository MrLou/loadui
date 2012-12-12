package com.eviware.loadui.ui.fx.util.test;

import static com.eviware.loadui.ui.fx.util.test.TestFX.find;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFieldBuilder;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.eviware.loadui.test.categories.GUITest;
import com.google.common.util.concurrent.SettableFuture;

@Category( GUITest.class )
public class ControllerApiTest
{
	private static final TestFX controller = TestFX.wrap( new FXScreenController() );
	private static final SettableFuture<Stage> stageFuture = SettableFuture.create();
	private static Stage stage;

	public static class ControllerApiTestApp extends Application
	{
		@Override
		public void start( Stage primaryStage ) throws Exception
		{
			primaryStage.setScene( SceneBuilder
					.create()
					.root(
							VBoxBuilder
									.create()
									.children( ButtonBuilder.create().id( "button1" ).text( "Button 1" ).build(),
											ButtonBuilder.create().id( "button2" ).text( "Button 2" ).build(),
											TextFieldBuilder.create().id( "text" ).build() ).build() ).build() );
			primaryStage.show();

			stageFuture.set( primaryStage );
		}
	}

	@BeforeClass
	public static void createWindow() throws Throwable
	{
		FXTestUtils.launchApp( ControllerApiTestApp.class );
		stage = stageFuture.get( 5, TimeUnit.SECONDS );
		FXTestUtils.bringToFront( stage );
		TestFX.targetWindow( stage );
	}

	@Test
	public void shouldTypeString()
	{
		String text = "H3llo W0rld.";

		TextField textField = find( "#text" );
		controller.type( "#text", text );

		assertThat( textField.getText(), is( text ) );
	}

	@Test
	public void shouldClickNodes()
	{
		final AtomicInteger counter = new AtomicInteger();
		Button button1 = find( "#button1" );
		button1.setOnAction( new EventHandler<ActionEvent>()
		{
			@Override
			public void handle( ActionEvent arg0 )
			{
				counter.incrementAndGet();
			}
		} );

		Button button2 = find( "#button2" );
		button2.setOnAction( new EventHandler<ActionEvent>()
		{
			@Override
			public void handle( ActionEvent arg0 )
			{
				counter.set( counter.get() * 2 );
			}
		} );

		controller.click( "#button1" ).click( "#button2" ).click( "#button1" );

		assertThat( counter.get(), is( 3 ) );
	}
}
