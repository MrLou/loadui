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
package com.eviware.loadui.ui.fx.views.project;

import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.ui.fx.api.input.DraggableEvent;
import com.eviware.loadui.ui.fx.api.intent.IntentEvent;
import com.eviware.loadui.ui.fx.util.ObservableLists;
import com.eviware.loadui.ui.fx.views.canvas.CanvasObjectView;
import com.eviware.loadui.ui.fx.views.canvas.CanvasView;
import com.eviware.loadui.ui.fx.views.canvas.scenario.ScenarioView;
import com.eviware.loadui.ui.fx.views.scenario.NewScenarioIcon;
import com.eviware.loadui.util.CanvasItemNameGenerator;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.control.Labeled;

import static com.eviware.loadui.ui.fx.util.ObservableLists.*;

public class ProjectCanvasView extends CanvasView
{
	public static CanvasView forCanvas( CanvasItem canvas )
	{

		if( instance != null )
		{
			instance.release();
		}
		instance = new ProjectCanvasView( canvas );
		return instance;
	}

	private ProjectCanvasView( CanvasItem canvas )
	{
		super( canvas );
		getStyleClass().add( "project-canvas-view" );
	}

	@Override
	protected ObservableList<? extends CanvasObjectView> createCanvasObjects()
	{
		ObservableList<ScenarioView> scenarios = transform(
				fx( ofCollection( getCanvas(), ProjectItem.SCENES, SceneItem.class, getCanvas().getChildren() ) ),
				Functions.compose( new InitializeCanvasObjectView<ScenarioView>(), new Function<SceneItem, ScenarioView>()
				{
					@Override
					public ScenarioView apply( SceneItem input )
					{
						return new ScenarioView( input );
					}
				} ) );

		return ObservableLists.concat( super.createCanvasObjects(), scenarios );
	}

	@Override
	protected ObservableList<? extends Labeled> createToolBoxContent()
	{
		NewScenarioIcon scenarioIcon = new NewScenarioIcon();
		scenarioIcon.setId( "newScenarioIcon" );
		ObservableList<NewScenarioIcon> scenario = FXCollections.observableList( ImmutableList.of( scenarioIcon ) );
		return ObservableLists.concat( scenario, super.createToolBoxContent() );
	}

	@Override
	protected boolean shouldAccept( final Object data )
	{
		return super.shouldAccept( data ) || data instanceof NewScenarioIcon;
	}

	@Override
	protected void handleDrop( final DraggableEvent event )
	{
		if( event.getData() instanceof NewScenarioIcon )
		{
			Point2D position = canvasLayer.sceneToLocal( event.getSceneX(), event.getSceneY() );

			fireEvent( IntentEvent.create( IntentEvent.INTENT_RUN_BLOCKING, new CreateScenarioTask( position ) ) );
			event.consume();
		}
		else
			super.handleDrop( event );
	}

	@Override
	public ProjectItem getCanvas()
	{
		return ( ProjectItem )super.getCanvas();
	}

	public class CreateScenarioTask extends Task<Void>
	{
		Point2D position;

		public CreateScenarioTask( Point2D position )
		{
			this.position = position;
		}

		@Override
		protected Void call() throws Exception
		{
			String name = CanvasItemNameGenerator.generateScenarioName( getCanvas() );
			log.debug( "About to create scenario: " + name );
			SceneItem scenario = getCanvas().createScene( name );
			scenario.setAttribute( "gui.layoutX", String.valueOf( ( int )position.getX() ) );
			scenario.setAttribute( "gui.layoutY", String.valueOf( ( int )position.getY() ) );
			return null;
		}
	}
}
