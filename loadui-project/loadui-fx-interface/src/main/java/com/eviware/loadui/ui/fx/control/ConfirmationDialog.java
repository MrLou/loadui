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

import com.eviware.loadui.ui.fx.api.intent.IntentEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorBuilder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import javax.annotation.Nonnull;

import com.sun.javafx.PlatformUtil;

// TODO: This class needs a JavaDoc'd builder.
public class ConfirmationDialog extends ButtonDialog
{
	private final Button confirmButton;
	private final Button cancelButton;

	public ConfirmationDialog( @Nonnull final Node owner, @Nonnull String header, @Nonnull String actionButtonLabel )
	{
		this( owner, header, actionButtonLabel, false, false );
	}

	public ConfirmationDialog( @Nonnull final Node owner, @Nonnull String header, @Nonnull String actionButtonLabel,
			boolean separateButtons, boolean forceConfirmOnEnter )
	{
		super( owner, header );
		addStyleClass( "confirmation-dialog" );

		confirmButton = ButtonBuilder.create().text( actionButtonLabel ).id( "default" ).defaultButton( true )
				.alignment( Pos.BOTTOM_RIGHT ).onAction( new EventHandler<ActionEvent>()
				{
					@Override
					public void handle( ActionEvent event )
					{
						close();
					}
				} ).build();

		cancelButton = ButtonBuilder.create().text( "Cancel" ).id( "cancel" ).cancelButton( true )
				.alignment( Pos.BOTTOM_RIGHT ).onAction( new EventHandler<ActionEvent>()
				{
					@Override
					public void handle( ActionEvent event )
					{
						close();
					}
				} ).build();
				
		if( separateButtons )
		{
			Separator buttonSeparator = SeparatorBuilder.create().style( "visibility: hidden;" ).maxWidth( 4 )
					.minWidth( 4 ).build();
			HBox.setHgrow( buttonSeparator, javafx.scene.layout.Priority.ALWAYS );
			getButtons().setAll( cancelButton, buttonSeparator, confirmButton );
		}
		else if( PlatformUtil.isMac() )
			getButtons().setAll( cancelButton, confirmButton );
		else
			getButtons().setAll( confirmButton, cancelButton );

		if( forceConfirmOnEnter )
			forceConfirmOnEnter();

        handleIntentEvents();
    }

    private void handleIntentEvents() {
        addEventHandler(IntentEvent.INTENT_SAVE, new EventHandler<IntentEvent<?>>() {
            @Override
            public void handle(IntentEvent<?> event) {
                if(event.getArg() == ConfirmationDialog.class)
                    confirm();

            }
        });
    }

    private void forceConfirmOnEnter()
	{
		addEventFilter( KeyEvent.ANY, new EventHandler<KeyEvent>()
		{
			@Override
			public void handle( KeyEvent e )
			{
				if( e.getCode() == KeyCode.ENTER )
				{
					confirm();
					e.consume();
				}
			}
		} );
	}

	public ObjectProperty<EventHandler<ActionEvent>> onConfirmProperty()
	{
		return confirmButton.onActionProperty();
	}

	public EventHandler<ActionEvent> getOnConfirm()
	{
		return confirmButton.onActionProperty().get();
	}

	public void setOnConfirm( EventHandler<ActionEvent> value )
	{
		confirmButton.onActionProperty().set( value );
	}

	public ObjectProperty<EventHandler<ActionEvent>> onCancelProperty()
	{
		return cancelButton.onActionProperty();
	}

	public EventHandler<ActionEvent> getOnCancel()
	{
		return cancelButton.onActionProperty().get();
	}

	public void setOnCancel( EventHandler<ActionEvent> value )
	{
		cancelButton.onActionProperty().set( value );
	}

	public BooleanProperty confirmDisableProperty()
	{
		return confirmButton.disableProperty();
	}

	public boolean isConfirmDisable()
	{
		return confirmButton.isDisable();
	}

	public void setConfirmDisable( boolean disable )
	{
		confirmButton.setDisable( disable );
	}

	public StringProperty confirmationTextProperty()
	{
		return confirmButton.textProperty();
	}

	public void confirm()
	{
        if(!confirmButton.isDisabled())
		    confirmButton.fire();
	}
}
