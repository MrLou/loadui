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
package com.eviware.loadui.impl.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.eviware.loadui.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.api.component.categories.OnOffCategory;
import com.eviware.loadui.api.component.categories.SchedulerCategory;
import com.eviware.loadui.api.counter.CounterSynchronizer;
import com.eviware.loadui.api.events.ActionEvent;
import com.eviware.loadui.api.events.BaseEvent;
import com.eviware.loadui.api.events.CollectionEvent;
import com.eviware.loadui.api.events.EventFirer;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.events.PropertyEvent;
import com.eviware.loadui.api.events.RemoteActionEvent;
import com.eviware.loadui.api.events.TerminalEvent;
import com.eviware.loadui.api.events.TerminalMessageEvent;
import com.eviware.loadui.api.execution.Phase;
import com.eviware.loadui.api.execution.TestExecution;
import com.eviware.loadui.api.messaging.MessageEndpoint;
import com.eviware.loadui.api.messaging.SceneCommunication;
import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.summary.MutableSummary;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.api.terminal.Terminal;
import com.eviware.loadui.api.terminal.TerminalMessage;
import com.eviware.loadui.config.SceneItemConfig;
import com.eviware.loadui.impl.counter.AggregatedCounterSupport;
import com.eviware.loadui.impl.counter.RemoteAggregatedCounterSupport;
import com.eviware.loadui.impl.summary.MutableChapterImpl;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSummarySection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionMetricsSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionNotablesSection;
import com.eviware.loadui.impl.terminal.ConnectionImpl;
import com.eviware.loadui.impl.terminal.InputTerminalImpl;
import com.eviware.loadui.impl.terminal.TerminalHolderSupport;
import com.eviware.loadui.util.BeanInjector;
import com.google.common.collect.ImmutableList;

public class SceneItemImpl extends CanvasItemImpl<SceneItemConfig> implements SceneItem
{
	private static final Logger log = LoggerFactory.getLogger( CanvasItemImpl.class );

	public static SceneItemImpl newInstance( ProjectItem project, SceneItemConfig config )
	{
		log.debug( "Got project: " + project );
		SceneItemImpl object = new SceneItemImpl( project, config );
		object.init();
		object.postInit();

		return object;
	}

	private final static String INCREMENT_VERSION = "incrementVersion";

	//On agents, this field is null.
	@CheckForNull
	private final ProjectItem project;

	private final ProjectListener projectListener;
	private final WorkspaceListener workspaceListener;
	private final TerminalHolderSupport terminalHolderSupport;
	private final InputTerminal stateTerminal;
	private final Map<AgentItem, Map<Object, Object>> remoteStatistics = new ConcurrentHashMap<>();
	private long version;
	private boolean propagate = true;
	private boolean wasLocalModeWhenStarted = true;

	private MessageEndpoint messageEndpoint = null;

	private final Property<Boolean> followProject;

	private SceneItemImpl( @Nonnull ProjectItem project, SceneItemConfig config )
	{
		super( config, LoadUI.isController() ? new RemoteAggregatedCounterSupport(
				BeanInjector.getBean( CounterSynchronizer.class ) ) : new AggregatedCounterSupport() );
		this.project = project;
		version = getConfig().getVersion().longValue();

		followProject = createProperty( FOLLOW_PROJECT_PROPERTY, Boolean.class, true );

		terminalHolderSupport = new TerminalHolderSupport( this );

		final InputTerminalImpl stateTerminalImpl = terminalHolderSupport.createInput( OnOffCategory.STATE_TERMINAL,
				OnOffCategory.STATE_TERMINAL_LABEL, OnOffCategory.STATE_TERMINAL_DESCRIPTION );
		stateTerminalImpl.setLikeFunction( new ComponentContext.LikeFunction()
		{
			@Override
			public boolean call( OutputTerminal output )
			{
				return output.getMessageSignature().containsKey( SchedulerCategory.ENABLED_MESSAGE_PARAM );
			}
		} );

		stateTerminal = stateTerminalImpl;

		workspaceListener = LoadUI.isController() ? new WorkspaceListener() : null;

		projectListener = LoadUI.isController() ? new ProjectListener() : null;
	}

	@Override
	protected void init()
	{
		super.init();
		if( LoadUI.isController() )
		{
			project.addEventListener( ActionEvent.class, projectListener );
			project.getWorkspace().addEventListener( PropertyEvent.class, workspaceListener );
			propagate = project.getWorkspace().isLocalMode();
		}
	}

	@Override
	protected void postInit()
	{
		super.postInit();
		addEventListener( BaseEvent.class, new SelfListener() );
	}

	@Override
	public ProjectItem getProject()
	{
		return project;
	}

	@Override
	public Collection<SceneItem> getChildren()
	{
		return ImmutableList.of();
	}

	@Override
	protected Connection createConnection( OutputTerminal output, InputTerminal input )
	{
		return new ConnectionImpl( getConfig().addNewConnection(), output, input );
	}

	@Override
	public long getVersion()
	{
		return version;
	}

	private void incrementVersion()
	{
		getConfig().setVersion( BigInteger.valueOf( ++version ) );
	}

	@Override
	public void release()
	{
		if( LoadUI.isController() )
		{
			project.removeEventListener( ActionEvent.class, projectListener );
			project.getWorkspace().removeEventListener( PropertyEvent.class, workspaceListener );
		}
		terminalHolderSupport.release();

		super.release();
	}

	@Override
	public void delete()
	{
		for( ComponentItem component : new ArrayList<>( getComponents() ) )
		{
			component.delete();
		}

		for( Terminal terminal : getTerminals() )
			for( Connection connection : terminal.getConnections().toArray(
					new Connection[terminal.getConnections().size()] ) )
				connection.disconnect();

		super.delete();
	}

	@Override
	public boolean isFollowProject()
	{
		return followProject.getValue();
	}

	@Override
	public String getColor()
	{
		return "#4B89E0";
	}

	@Override
	public CanvasItem getCanvas()
	{
		return getProject();
	}

	@Override
	public Collection<Terminal> getTerminals()
	{
		return terminalHolderSupport.getTerminals();
	}

	@Override
	public Terminal getTerminalByName( String name )
	{
		return terminalHolderSupport.getTerminalByName( name );
	}

	@Override
	public void handleTerminalEvent( InputTerminal input, TerminalEvent event )
	{
		if( event instanceof TerminalMessageEvent && input == stateTerminal )
		{
			TerminalMessage message = ( ( TerminalMessageEvent )event ).getMessage();
			if( message.containsKey( OnOffCategory.ENABLED_MESSAGE_PARAM ) )
			{
				Object enabled = message.get( OnOffCategory.ENABLED_MESSAGE_PARAM );
				if( enabled instanceof Boolean )
					triggerAction( ( Boolean )enabled ? START_ACTION : STOP_ACTION );
			}
		}
	}

	@Override
	public void setFollowProject( boolean followProject )
	{
		this.followProject.setValue( followProject );
	}

	@Override
	public Property<Boolean> followProjectProperty()
	{
		return followProject;
	}

	@Override
	public InputTerminal getStateTerminal()
	{
		return stateTerminal;
	}

	@Override
	public void broadcastMessage( String channel, Object data )
	{
		if( LoadUI.isController() )
			getProject().broadcastMessage( this, channel, data );
		else if( messageEndpoint != null )
			messageEndpoint.sendMessage( channel, data );
	}

	@Override
	protected void onComplete( EventFirer source )
	{
		//		if( LoadUI.CONTROLLER.equals( System.getProperty( "loadui.instance" ) ) && source.equals( this ) )
		//		{
		//			// on controller when source is this test case (for both local and
		//			// distributed mode)...add ON_COMPLETE_DONE event listener to this test
		//			// case to listen when it is completed
		//
		//			addEventListener( BaseEvent.class, new EventHandler<BaseEvent>()
		//			{
		//				@Override
		//				public void handleEvent( BaseEvent event )
		//				{
		//					if( event.getKey().equals( ON_COMPLETE_DONE ) )
		//					{
		//						removeEventListener( BaseEvent.class, this );
		//						// if test case is linked and project is not running then do
		//						// generate summary. if project is running it will generate
		//						// summary instead. if test case is not linked generate
		//						// summary for it. (the scenario when project is running and
		//						// the source is test case can occur when limit is set to a
		//						// test case so it finishes before project)
		//						if( !getProject().isRunning() && isFollowProject() || !isFollowProject() )
		//						{
		//							//							generateSummary();
		//						}
		//					}
		//				}
		//			} );
		//		}

		if( !LoadUI.isController() )
		{
			// on agent, application is running in distributed mode, so send
			// data to the controller

			Map<String, Object> data = new HashMap<>();
			data.put( AgentItem.SCENE_ID, getId() );

			for( ComponentItem component : getComponents() )
				data.put( component.getId(), component.getBehavior().collectStatisticsData() );
			messageEndpoint.sendMessage( AgentItem.AGENT_CHANNEL, data );
			log.debug( "Sending statistics data from {}", this );
		}
		else if( wasLocalModeWhenStarted || ( !project.getWorkspace().isLocalMode() && getActiveAgents().isEmpty() ) )
		{
			// on controller, in local mode or in distributed mode with no active
			// agents
			// NOTE: when in distributed mode and there are no active agents scene
			// would never finish and controller won't receive info from it so mark
			// this scene as completed immediately.

			// if source is project, set completed will inform project (over
			// SceneAwaiter) that this test case has finished.
			// if source is this test case than inform its listener defined at the
			// top of this method to generate summary
			setCompleted( true );
		}
	}

	@Override
	public void cancelComponents()
	{
		if( !LoadUI.isController() )
		{
			// on agent. cancel components of this test case on current agent
			super.cancelComponents();
		}
		else
		{
			if( project.getWorkspace().isLocalMode() )
			{
				// local mode, simply cancel components
				super.cancelComponents();
			}
			else
			{
				if( getActiveAgents().size() > 0 )
				{
					// test cases is deployed to one or more agents so send message
					// to all agents to cancel
					broadcastMessage( SceneCommunication.CHANNEL, new Object[] { getId(), Long.toString( getVersion() ),
							SceneCommunication.CANCEL_COMPONENTS } );
				}
				else
				{
					// test case is not deployed to any active agent, so cancel it
					// locally (in case non deployed test cases are executed locally)
					super.cancelComponents();
				}
			}
		}
	}

	public boolean isStatisticsReady()
	{
		for( AgentItem agent : getActiveAgents() )
			if( !remoteStatistics.containsKey( agent ) )
				return false;
		return true;
	}

	public int getRemoteStatisticsCount()
	{
		return remoteStatistics.size();
	}

	public void handleStatisticsData( AgentItem source, Map<Object, Object> data )
	{
		remoteStatistics.put( source, data );
		log.debug( "{} got statistics data from {}", this, source );
		if( !isStatisticsReady() )
			return;

		for( ComponentItem component : getComponents() )
		{
			String cId = component.getId();
			Map<AgentItem, Object> cData = new HashMap<>();
			for( Entry<AgentItem, Map<Object, Object>> e : remoteStatistics.entrySet() )
				cData.put( e.getKey(), e.getValue().get( cId ) );
			component.getBehavior().handleStatisticsData( cData );
		}

		setCompleted( true );
	}

	@Override
	public void appendToSummary( MutableSummary mutableSummary )
	{
		MutableChapterImpl chap = ( MutableChapterImpl )mutableSummary.addChapter( getLabel() );
		chap.addSection( new TestCaseDataSummarySection( this ) );
		chap.addSection( new TestCaseExecutionDataSection( this ) );
		chap.addSection( new TestCaseExecutionMetricsSection( this ) );
		chap.addSection( new TestCaseExecutionNotablesSection( this ) );
		chap.addSection( new TestCaseDataSection( this ) );
		chap.setDescription( getDescription() );

		for( ComponentItem component : getComponents() )
			component.generateSummary( chap );
	}

	@Override
	protected void reset()
	{
		super.reset();
		remoteStatistics.clear();
	}

	public void setMessageEndpoint( MessageEndpoint messageEndpoint )
	{
		this.messageEndpoint = messageEndpoint;
	}

	@Override
	protected void setRunning( boolean running )
	{
		super.setRunning( running );

		if( running )
		{
			wasLocalModeWhenStarted = LoadUI.isController() ? project.getWorkspace().isLocalMode() : true;
		}

		fireBaseEvent( ACTIVITY );
	}

	@Override
	public boolean isActive()
	{
		return isRunning();
	}

	@Override
	public String getHelpUrl()
	{
		return "http://www.loadui.org/Working-with-loadUI/scenarios.html";
	}

	@Override
	public ModelItemType getModelItemType()
	{
		return ModelItemType.SCENARIO;
	}

	private Collection<AgentItem> getActiveAgents()
	{
		ArrayList<AgentItem> agents = new ArrayList<>();
		for( AgentItem agent : getProject().getAgentsAssignedTo( this ) )
			if( agent.isReady() )
				agents.add( agent );
		return agents;
	}

	@Override
	public boolean isAffectedByExecutionTask( TestExecution execution )
	{
		CanvasItem startedCanvas = execution.getCanvas();
		log.debug( "startedCanvas==this: " + Boolean.toString( startedCanvas == this ) + " getProject()==startedCanvas: "
				+ Boolean.toString( getProject() == startedCanvas ) + " isFollowProject(): "
				+ Boolean.toString( isFollowProject() ) );
		return startedCanvas == this || ( getProject() == startedCanvas && isFollowProject() );
	}

	@Override
	protected void onExecutionTask( TestExecution execution, Phase phase )
	{
		if( isAffectedByExecutionTask( execution ) )
		{
			log.debug( "STARTING !!!" );
			super.onExecutionTask( execution, phase );
		}
	}

	private class SelfListener implements EventHandler<BaseEvent>
	{
		@Override
		public void handleEvent( BaseEvent event )
		{
			// The reason we use a separate event to increment the version is to
			// ensure that all listeners of the other events get executed prior to
			// incrementing the version.
			if( event instanceof CollectionEvent )
				fireBaseEvent( INCREMENT_VERSION );
			else if( event instanceof ActionEvent && event.getSource() == SceneItemImpl.this && !propagate )
				fireEvent( new RemoteActionEvent( SceneItemImpl.this, ( ActionEvent )event ) );
			else if( LABEL.equals( event.getKey() ) )
				fireBaseEvent( INCREMENT_VERSION );
			else if( INCREMENT_VERSION.equals( event.getKey() ) )
				incrementVersion();
		}
	}

	private class ProjectListener implements EventHandler<ActionEvent>
	{
		@Override
		public void handleEvent( ActionEvent event )
		{
			if( !isFollowProject() && ( START_ACTION.equals( event.getKey() ) || STOP_ACTION.equals( event.getKey() ) ) )
				return;

			if( !propagate || COUNTER_RESET_ACTION.equals( event.getKey() ) || COMPLETE_ACTION.equals( event.getKey() ) )
				fireEvent( new RemoteActionEvent( SceneItemImpl.this, event ) );

			fireEvent( event );
		}
	}

	private class WorkspaceListener implements EventHandler<PropertyEvent>
	{
		@Override
		public void handleEvent( PropertyEvent event )
		{
			if( WorkspaceItem.LOCAL_MODE_PROPERTY.equals( event.getProperty().getKey() ) )
			{
				propagate = ( Boolean )event.getProperty().getValue();
				// if( isRunning() )
				// {
				// ActionEvent startAction = new ActionEvent( SceneItemImpl.this,
				// START_ACTION );
				// if( !propagate )
				// fireEvent( new RemoteActionEvent( SceneItemImpl.this, startAction
				// ) );
				// else
				// fireEvent( startAction );
				// }
			}
		}
	}

}
