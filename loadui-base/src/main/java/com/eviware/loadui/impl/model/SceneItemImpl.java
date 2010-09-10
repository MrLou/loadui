/*
 * Copyright 2010 eviware software ab
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.impl.model;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.eviware.loadui.api.addressable.AddressableRegistry;
import com.eviware.loadui.api.component.categories.TriggerCategory;
import com.eviware.loadui.api.counter.CounterHolder;
import com.eviware.loadui.api.counter.CounterSynchronizer;
import com.eviware.loadui.api.events.ActionEvent;
import com.eviware.loadui.api.events.CollectionEvent;
import com.eviware.loadui.api.events.EventFirer;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.events.BaseEvent;
import com.eviware.loadui.api.events.PropertyEvent;
import com.eviware.loadui.api.events.RemoteActionEvent;
import com.eviware.loadui.api.events.TerminalEvent;
import com.eviware.loadui.api.events.TerminalMessageEvent;
import com.eviware.loadui.api.messaging.MessageEndpoint;
import com.eviware.loadui.api.model.CanvasItem;
import com.eviware.loadui.api.model.ComponentItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.AgentItem;
import com.eviware.loadui.api.model.SceneItem;
import com.eviware.loadui.api.model.WorkspaceItem;
import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.summary.MutableSummary;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.api.terminal.Terminal;
import com.eviware.loadui.api.terminal.TerminalMessage;
import com.eviware.loadui.config.SceneItemConfig;
import com.eviware.loadui.impl.counter.CounterSupport;
import com.eviware.loadui.impl.counter.RemoteAggregatedCounterSupport;
import com.eviware.loadui.impl.summary.MutableChapterImpl;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseDataSummarySection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionDataSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionMetricsSection;
import com.eviware.loadui.impl.summary.sections.TestCaseExecutionNotablesSection;
import com.eviware.loadui.impl.terminal.ConnectionImpl;
import com.eviware.loadui.impl.terminal.TerminalHolderSupport;
import com.eviware.loadui.util.BeanInjector;

public class SceneItemImpl extends CanvasItemImpl<SceneItemConfig> implements SceneItem
{
	private final static String INCREMENT_VERSION = "incrementVersion";

	private final ProjectItem project;
	private final Set<OutputTerminal> exports = new HashSet<OutputTerminal>();
	private final ProjectListener projectListener;
	private final WorkspaceListener workspaceListener;
	private final TerminalHolderSupport terminalHolderSupport;
	private final InputTerminal stateTerminal;
	private final Map<AgentItem, Map<Object, Object>> remoteStatistics = new HashMap<AgentItem, Map<Object, Object>>();
	private long version;
	private boolean propagate = true;
	private boolean awaitingSummary = false;

	private MessageEndpoint messageEndpoint = null;

	private final Property<Boolean> followProject;

	public SceneItemImpl( ProjectItem project, SceneItemConfig config )
	{
		super( config,
				"controller".equals( System.getProperty( "loadui.instance" ) ) ? new RemoteAggregatedCounterSupport(
						BeanInjector.getBean( CounterSynchronizer.class ) ) : new CounterSupport() );
		this.project = project;
		version = getConfig().getVersion().longValue();

		followProject = createProperty( FOLLOW_PROJECT_PROPERTY, Boolean.class, true );

		AddressableRegistry addressableRegistry = BeanInjector.getBean( AddressableRegistry.class );

		terminalHolderSupport = new TerminalHolderSupport( this );

		stateTerminal = terminalHolderSupport
				.createInput( TriggerCategory.STATE_TERMINAL, "Controls TestCase execution." );

		for( String exportId : getConfig().getExportedTerminalArray() )
			exports.add( ( OutputTerminal )addressableRegistry.lookup( exportId ) );

		workspaceListener = "controller".equals( System.getProperty( "loadui.instance" ) ) ? new WorkspaceListener()
				: null;

		projectListener = "controller".equals( System.getProperty( "loadui.instance" ) ) ? new ProjectListener() : null;
	}

	@Override
	public void init()
	{
		super.init();
		addEventListener( BaseEvent.class, new SelfListener() );
		if( project != null )
		{
			project.addEventListener( ActionEvent.class, projectListener );
			project.getWorkspace().addEventListener( PropertyEvent.class, workspaceListener );
			propagate = project.getWorkspace().isLocalMode();
		}
	}

	@Override
	public ProjectItem getProject()
	{
		return project;
	}

	@Override
	public void exportTerminal( OutputTerminal terminal )
	{
		if( exports.add( terminal ) )
		{
			getConfig().addExportedTerminal( terminal.getId() );
			fireCollectionEvent( EXPORTS, CollectionEvent.Event.ADDED, terminal );
		}
	}

	@Override
	public void unexportTerminal( OutputTerminal terminal )
	{
		if( exports.remove( terminal ) )
		{
			for( int i = 0; i < getConfig().sizeOfExportedTerminalArray(); i++ )
			{
				if( terminal.getId().equals( getConfig().getExportedTerminalArray( i ) ) )
				{
					getConfig().removeExportedTerminal( i );
					break;
				}
			}
			fireCollectionEvent( EXPORTS, CollectionEvent.Event.REMOVED, terminal );
		}
	}

	@Override
	public Collection<OutputTerminal> getExportedTerminals()
	{
		return Collections.unmodifiableSet( exports );
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

	private void incrVersion()
	{
		getConfig().setVersion( BigInteger.valueOf( ++version ) );
	}

	@Override
	public void release()
	{
		if( project != null )
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
	public Terminal getTerminalByLabel( String label )
	{
		return terminalHolderSupport.getTerminalByLabel( label );
	}

	@Override
	public void handleTerminalEvent( InputTerminal input, TerminalEvent event )
	{
		if( event instanceof TerminalMessageEvent && input == stateTerminal )
		{
			TerminalMessage message = ( ( TerminalMessageEvent )event ).getMessage();
			if( message.containsKey( TriggerCategory.ENABLED_MESSAGE_PARAM ) )
			{
				Object enabled = message.get( TriggerCategory.ENABLED_MESSAGE_PARAM );
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
	public InputTerminal getStateTerminal()
	{
		return stateTerminal;
	}

	@Override
	public void broadcastMessage( String channel, Object data )
	{
		if( "controller".equals( System.getProperty( "loadui.instance" ) ) )
			getProject().broadcastMessage( this, channel, data );
		else if( messageEndpoint != null )
			messageEndpoint.sendMessage( channel, data );
	}

	@Override
	protected void onComplete( EventFirer source )
	{
		if( "agent".equals( System.getProperty( "loadui.instance" ) ) )
		{
			Map<String, Object> data = new HashMap<String, Object>();
			data.put( AgentItem.SCENE_ID, getId() );
			for( ComponentItem component : getComponents() )
				data.put( component.getId(), component.getBehavior().collectStatisticsData() );
			messageEndpoint.sendMessage( AgentItem.AGENT_CHANNEL, data );
			log.debug( "Sending statistics data from {}", this );
		}
		else if( source == this )
			if( getProject().getAgentsAssignedTo( this ).size() > 0 && !getProject().getWorkspace().isLocalMode() )
				awaitingSummary = true;
			else
				doGenerateSummary();
	}

	public boolean statisticsReady()
	{
		for( AgentItem agent : getProject().getAgentsAssignedTo( this ) )
			if( !remoteStatistics.containsKey( agent ) )
				return false;
		return true;
	}

	public void handleStatisticsData( AgentItem source, Map<Object, Object> data )
	{
		remoteStatistics.put( source, data );
		log.debug( "{} got statistics data from {}", this, source );
		if( !statisticsReady() )
			return;

		for( ComponentItem component : getComponents() )
		{
			String cId = component.getId();
			Map<AgentItem, Object> cData = new HashMap<AgentItem, Object>();
			for( Entry<AgentItem, Map<Object, Object>> e : remoteStatistics.entrySet() )
				cData.put( e.getKey(), e.getValue().get( cId ) );
			component.getBehavior().handleStatisticsData( cData );
		}

		if( awaitingSummary )
			doGenerateSummary();
	}

	@Override
	public void generateSummary( MutableSummary summary )
	{
		MutableChapterImpl chap = ( MutableChapterImpl )summary.addChapter( getLabel() );
		chap.addSection( new TestCaseDataSummarySection( this ) );
		chap.addSection( new TestCaseExecutionDataSection( this ) );
		chap.addSection( new TestCaseExecutionMetricsSection( this ) );
		chap.addSection( new TestCaseExecutionNotablesSection( this ) );
		chap.addSection( new TestCaseDataSection( this ) );
		chap.setDescription( getDescription() );
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
		return "http://www.loadui.org/Working-with-loadUI/agents-and-testcases.html";
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
			else if( EXPORTS.equals( event.getKey() ) )
				fireBaseEvent( INCREMENT_VERSION );
			else if( INCREMENT_VERSION.equals( event.getKey() ) )
				incrVersion();
		}
	}

	private class ProjectListener implements EventHandler<ActionEvent>
	{
		@Override
		public void handleEvent( ActionEvent event )
		{
			if( !isFollowProject() && ( START_ACTION.equals( event.getKey() ) || STOP_ACTION.equals( event.getKey() ) ) )
				return;

			if( propagate && !CounterHolder.COUNTER_RESET_ACTION.equals( event.getKey() ) )
				fireEvent( event );
			else
			{
				fireEvent( new RemoteActionEvent( SceneItemImpl.this, event ) );
				// if( COUNTER_RESET_ACTION.equals( event.getKey() ) )
				fireEvent( event );
			}
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
