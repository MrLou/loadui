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

import java.lang.ref.WeakReference;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.RegionBuilder;
import javafx.util.Duration;

import javax.annotation.Nonnull;

import com.eviware.loadui.api.counter.CounterHolder;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.ui.fx.util.Properties;

public abstract class PlaybackPanel<T extends CounterDisplay, C extends CanvasItem> extends HBox
{
	public final static String TIME_LABEL = "Time";
	public final static String REQUESTS_LABEL = "Sent";
	public final static String FAILURES_LABEL = "Failures";

	private final UpdateDisplays updateDisplays = new UpdateDisplays( this );

	protected final C canvas;

	protected final T time;
	protected final T requests;
	protected final T failures;

	protected final PlayButton playButton;

	public PlaybackPanel( @Nonnull final C canvas )
	{
		this.canvas = canvas;

		setAlignment( Pos.CENTER );
		setMaxHeight( 27 );
		playButton = new PlayButton( canvas );

		time = timeCounter();
		requests = timeRequests();
		failures = timeFailures();

		updateDisplays.timeline.play();
	}

	protected abstract T timeCounter();

	protected abstract T timeRequests();

	protected abstract T timeFailures();

	protected final EventHandler<ActionEvent> resetCounters = new EventHandler<ActionEvent>()
	{
		@Override
		public void handle( ActionEvent e )
		{
			canvas.triggerAction( CounterHolder.COUNTER_RESET_ACTION );
		}
	};

	protected final EventHandler<ActionEvent> openLimitsDialog = new EventHandler<ActionEvent>()
	{
		@Override
		public void handle( ActionEvent e )
		{
			new LimitsDialog( PlaybackPanel.this, canvas ).show();
		}
	};

	protected Button resetButton()
	{
		return ButtonBuilder.create().text( "Reset" ).style( "-fx-font-size: 10px; " ).onAction( resetCounters ).build();
	}

	protected static Separator separator()
	{
		return new Separator( Orientation.VERTICAL );
	}

	final protected Image image( String name )
	{
		return new Image( getClass().getResourceAsStream( name ) );
	}

	protected ToggleButton linkScenarioButton( final SceneItem scenario )
	{
		final ToggleButton linkButton = ToggleButtonBuilder
				.create()
				.id( "link-scenario" )
				.graphic(
						HBoxBuilder
								.create()
								.children( RegionBuilder.create().styleClass( "graphic" ).build(),
										RegionBuilder.create().styleClass( "secondary-graphic" ).build() ).build() ).build();

        final Property<Boolean> linkedProperty = Properties.convert( scenario.followProjectProperty() );
		linkButton.setSelected( linkedProperty.getValue() );

        linkButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                //System.out.println("\n\n ---> LINK BUTTON CHANGED <---");
                //System.out.println(scenario.getLabel() +" :>> scenario.followProperty: " + scenario.followProjectProperty() + ", linkedProperty: " + linkedProperty.getValue() + " <--> linkedButton.isSelected: " + linkButton.isSelected());
            }
        });

        scenario.isRunning();

        linkedProperty.bindBidirectional(linkButton.selectedProperty());
        //System.out.println("\n\n ---> VIEW WITH NEW LINK-BUTTON <---");
        //System.out.println(scenario.getLabel() +" :>> scenario.followProperty: " + scenario.followProjectProperty() + ", linkedProperty: " + linkedProperty.getValue() + " <--> linkedButton.isSelected: " + linkButton.isSelected());

		return linkButton;
	}

	private static class UpdateDisplays implements EventHandler<ActionEvent>
	{
		private final WeakReference<? extends PlaybackPanel<?, ?>> ref;
		private final Timeline timeline;

		private UpdateDisplays( PlaybackPanel<?, ?> panel )
		{
			ref = new WeakReference<PlaybackPanel<?, ?>>( panel );
			timeline = new Timeline( new KeyFrame( Duration.millis( 500 ), this ) );
			timeline.setCycleCount( Timeline.INDEFINITE );
		}

		@Override
		public void handle( ActionEvent event )
		{
			PlaybackPanel<?, ?> panel = ref.get();
			if( panel != null )
			{
				panel.time.setValue( panel.canvas.getCounter( CanvasItem.TIMER_COUNTER ).get() );
				panel.requests.setValue( panel.canvas.getCounter( CanvasItem.REQUEST_COUNTER ).get() );
				panel.failures.setValue( panel.canvas.getCounter( CanvasItem.FAILURE_COUNTER ).get() );
			}
			else
			{
				timeline.stop();
			}
		}
	}
}
