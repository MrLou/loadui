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

import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.ui.fx.views.canvas.CounterDisplay.Formatting;
import com.eviware.loadui.ui.fx.views.canvas.scenario.ScenarioCounterDisplay;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBoxBuilder;

public class MiniScenarioPlaybackPanel extends PlaybackPanel<CounterDisplay, SceneItem>
{
	private ToggleButton linkButton;
	private Property<Boolean> linkedProperty;

	public MiniScenarioPlaybackPanel( SceneItem scenario )
	{
		super( scenario );

		linkedProperty = getLinkedProperty( scenario );
		linkButton = linkScenarioButton( linkedProperty );

		linkButton.selectedProperty().bindBidirectional( linkedProperty );
		playButton.bindToSelectedProperty( linkButton.disableProperty() );

		getStyleClass().setAll( "mini-playback-panel" );
		setSpacing( 6 );
		setPrefWidth( 310 );

		getChildren().setAll(
				separator(),
				playButton,
				separator(),
				HBoxBuilder.create()
						.alignment( Pos.CENTER )
						.padding( new Insets( 6, 6, 6, 0 ) )
						.children( linkButton )
						.build(),
				time,
				requests,
				failures );
	}

	@Override
	protected CounterDisplay timeCounter()
	{
		return new ScenarioCounterDisplay( TIME_LABEL, Formatting.TIME );
	}

	@Override
	protected CounterDisplay timeRequests()
	{
		return new ScenarioCounterDisplay( REQUESTS_LABEL );
	}

	@Override
	protected CounterDisplay timeFailures()
	{
		return new ScenarioCounterDisplay( FAILURES_LABEL );
	}
}
