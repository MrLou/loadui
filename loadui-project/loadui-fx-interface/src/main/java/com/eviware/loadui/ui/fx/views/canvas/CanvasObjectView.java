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
package com.eviware.loadui.ui.fx.views.canvas;

import com.eviware.loadui.api.model.CanvasObjectItem;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.api.terminal.TerminalHolder;
import com.eviware.loadui.api.traits.Deletable;
import com.eviware.loadui.ui.fx.api.intent.IntentEvent;
import com.eviware.loadui.ui.fx.util.FXMLUtils;
import com.eviware.loadui.ui.fx.util.Properties;
import com.eviware.loadui.ui.fx.views.canvas.terminal.InputTerminalView;
import com.eviware.loadui.ui.fx.views.canvas.terminal.OutputTerminalView;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eviware.loadui.ui.fx.util.ObservableLists.*;

public abstract class CanvasObjectView extends StackPane implements Deletable
{
	protected static final Logger log = LoggerFactory.getLogger( CanvasObjectView.class );

	private static final Function<InputTerminal, InputTerminalView> INPUT_TERMINAL_TO_VIEW = new Function<InputTerminal, InputTerminalView>()
	{
		@Override
		public InputTerminalView apply( final InputTerminal terminal )
		{
			InputTerminalView terminalView = new InputTerminalView( terminal );
			HBox.setHgrow( terminalView, Priority.ALWAYS );
			return terminalView;
		}
	};

	private static final Function<OutputTerminal, OutputTerminalView> OUTPUT_TERMINAL_TO_VIEW = new Function<OutputTerminal, OutputTerminalView>()
	{
		@Override
		public OutputTerminalView apply( final OutputTerminal terminal )
		{
			OutputTerminalView terminalView = new OutputTerminalView( terminal );
			HBox.setHgrow( terminalView, Priority.ALWAYS );
			return terminalView;
		}
	};

	private final CanvasObjectItem canvasObject;
	private final ObservableList<InputTerminalView> inputTerminals;
	private final ObservableList<OutputTerminalView> outputTerminals;

	@FXML
	protected Label canvasObjectLabel;
	@FXML
	protected Pane inputTerminalPane;
	@FXML
	protected Pane outputTerminalPane;
	@FXML
	protected MenuButton menuButton;
	@FXML
	protected BorderPane topBar;
	@FXML
	protected HBox buttonBar;
	@FXML
	protected StackPane content;

	public CanvasObjectView( CanvasObjectItem canvasObject )
	{
		this.canvasObject = canvasObject;
		inputTerminals = transform(
				fx( ofCollection( canvasObject, TerminalHolder.TERMINALS, InputTerminal.class,
						Iterables.filter( canvasObject.getTerminals(), InputTerminal.class ) ) ), INPUT_TERMINAL_TO_VIEW );

		outputTerminals = transform(
				fx( ofCollection( canvasObject, TerminalHolder.TERMINALS, OutputTerminal.class,
						Iterables.filter( canvasObject.getTerminals(), OutputTerminal.class ) ) ), OUTPUT_TERMINAL_TO_VIEW );

		setStyle( "-fx-header-color: " + canvasObject.getColor() + ";" );

		FXMLUtils
				.load( this, this, CanvasObjectView.class.getResource( CanvasObjectView.class.getSimpleName() + ".fxml" ) );
	}

	@FXML
	protected void initialize()
	{
		canvasObjectLabel.textProperty().bind( Properties.forLabel( canvasObject ) );

		Bindings.bindContent( inputTerminalPane.getChildren(), inputTerminals );
		Bindings.bindContent( outputTerminalPane.getChildren(), outputTerminals );

		addEventHandler( MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>()
		{
			@Override
			public void handle( MouseEvent event )
			{
				if( event.isPrimaryButtonDown() )
				{
					toFront();
				}
			}
		} );

		addEventHandler( MouseEvent.ANY, new EventHandler<MouseEvent>()
		{
			@Override
			public void handle( MouseEvent event )
			{
				if( event.getEventType() == MouseEvent.MOUSE_CLICKED )
					requestFocus();
				
				event.consume();
			}
		} );

	}

	public CanvasObjectItem getCanvasObject()
	{
		return canvasObject;
	}

	public ObservableList<OutputTerminalView> getOutputTerminalViews()
	{
		return outputTerminals;
	}

	public ObservableList<InputTerminalView> getInputTerminalViews()
	{
		return inputTerminals;
	}

	@Override
	public void delete()
	{
		//FIXME check if this is ever actually called!!
		// The CanvasObject will usually be a ComponentItemImpl which knows how to delete() itself
		log.debug( "Deleting the CanvasObjectView from Canvas " + canvasObject.getCanvas().getLabel() );
		fireEvent( IntentEvent.create( IntentEvent.INTENT_DELETE, canvasObject ) );
	}

}
