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
package com.eviware.loadui.ui.fx.views.scenario;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.ui.fx.api.intent.IntentEvent;
import com.eviware.loadui.ui.fx.control.DragNode;
import com.eviware.loadui.ui.fx.util.UIUtils;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;

public class NewScenarioIcon extends Label
{
	private final VBox vbox;
	public NewScenarioIcon()
	{
		vbox = VBoxBuilder.create().spacing( 6 ).maxWidth( 85 ).alignment( Pos.TOP_LEFT ).children( createIcon(), LabelBuilder.create().alignment( Pos.TOP_LEFT ).id("component").text("VU Scenario").build() ).build();

		getStyleClass().add( "icon" );
		setGraphic( vbox );

		addEventFilter( MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
		{
			@Override
			public void handle( MouseEvent event )
			{
				if( event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 )
				{
					fireEvent( IntentEvent.create( IntentEvent.INTENT_CREATE, ProjectItem.class ) );
				}
			}
		} );

		DragNode.install( vbox, createIcon() ).setData( this );
	}

	private static Node createIcon()
	{
		ImageView icon = new ImageView( UIUtils.getImageFor( SceneItem.class ) );
		icon.setFitWidth( 72 );
		icon.autosize();
		icon.setPreserveRatio( true );
		return icon;
	}

	@Override
	public String toString()
	{
		return "VU Scenario";
	}
}
