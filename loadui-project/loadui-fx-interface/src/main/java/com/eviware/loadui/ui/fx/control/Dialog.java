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

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static com.eviware.loadui.ui.fx.util.EventUtils.forwardIntentsFrom;
import static javafx.beans.binding.Bindings.bindContent;

public class Dialog extends Stage
{
	public static final String INVALID_CLASS = "invalid";

	private final Pane rootPane;
	private final Window parentWindow;

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger( Dialog.class );

	public Dialog( @Nonnull final Node owner, @Nonnull String title )
	{
		rootPane = VBoxBuilder.create().styleClass( "dialog" ).minWidth( 300 ).build();

		Scene scene = new Scene( rootPane );
		setScene( scene );

		final Scene ownerScene = owner.getScene();
		bindContent( scene.getStylesheets(), ownerScene.getStylesheets() );
		parentWindow = ownerScene.getWindow();

		setWindowProperties( title );
		forwardIntentsFrom( this ).to( owner );
		centerWindow();
	}

	private void setWindowProperties( String title )
	{
		setResizable( false );
		initStyle( StageStyle.UTILITY );
		initModality( Modality.APPLICATION_MODAL );
		initOwner( parentWindow );
		setTitle( title );
	}

	private void centerWindow()
	{
		addEventHandler( WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>()
		{
			@Override
			public void handle( WindowEvent arg0 )
			{
				setX( getCenterXOfParentWindow() - getWidth() / 2 );
				setY( getCenterYOfParentWindow() - getHeight() / 2 );
			}
		} );
	}

	protected void addStyleClass( String styleClass )
	{
		rootPane.getStyleClass().add( styleClass );
	}

	protected Node lookup( String selector )
	{
		return rootPane.lookup( selector );
	}

	private double getCenterYOfParentWindow()
	{
		return parentWindow.getY() + parentWindow.getHeight() / 2;
	}

	private double getCenterXOfParentWindow()
	{
		return parentWindow.getX() + parentWindow.getWidth() / 2;
	}

	public ObservableList<Node> getItems()
	{
		return rootPane.getChildren();
	}
}
