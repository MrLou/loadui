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
package com.eviware.loadui.api.model;

import com.eviware.loadui.api.component.ComponentCreationException;
import com.eviware.loadui.api.component.ComponentDescriptor;
import com.eviware.loadui.api.counter.CounterHolder;
import com.eviware.loadui.api.property.Property;
import com.eviware.loadui.api.statistics.StatisticHolder;
import com.eviware.loadui.api.summary.Summary;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Base ModelItem which holds ComponentItems and Connections.
 *
 * @author dain.nilsson
 */
public interface CanvasItem extends ModelItem, CounterHolder, StatisticHolder
{
	// events
	public static final String COMPONENTS = CanvasItem.class.getName() + "@components";
	public static final String CONNECTIONS = CanvasItem.class.getName() + "@connections";
	public static final String SUMMARY = CanvasItem.class.getName() + "@summary";
	public static final String LIMITS = CanvasItem.class.getName() + "@limits";
	public static final String RUNNING = CanvasItem.class.getName() + "@running";

	// properties
	public static final String ABORT_ON_FINISH_PROPERTY = CanvasItem.class.getSimpleName() + ".abortOnFinish";

	public static final String START_ACTION = "START";
	public static final String STOP_ACTION = "STOP";
	public static final String COMPLETE_ACTION = "COMPLETE";
	public static final String READY_ACTION = "READY";

	public static final String TIMER_COUNTER = "Time";
	public static final String SAMPLE_COUNTER = "Samples";
	public static final String REQUEST_COUNTER = "Requests";
	public static final String REQUEST_FAILURE_COUNTER = "Failed Requests";
	public static final String ASSERTION_COUNTER = "Assertions";
	public static final String ASSERTION_FAILURE_COUNTER = "Failed Assertions";
	public static final String FAILURE_COUNTER = "Failures";

	public static final String REQUEST_FAILURE_VARIABLE = "Request Failures";
	public static final String ASSERTION_FAILURE_VARIABLE = "Assertion Failures";
	public static final String FAILURE_VARIABLE = "Failures";
	public static final String REQUEST_VARIABLE = "Requests";

	/**
	 * Get the ProjectItem which this CanvasItem belongs to. If this CanvasItem
	 * is a ProjectItem itself, then it will return itself. Note that if this
	 * CanvasItem is a SceneItem deployed on an Agent, then it will not have a
	 * ProjectItem and will return null.
	 *
	 * @return The ProjectItem which this CanvasItem belongs to.
	 */
	@Nonnull
	public ProjectItem getProject();

	/**
	 * Get the sub CanvasItems that this CanvasItem contains, if any.
	 */
	@Nonnull
	public Collection<? extends SceneItem> getChildren();

	/**
	 * Check whether the item has been changes since the last save.
	 *
	 * @return True if it has been changed
	 */
	public boolean isDirty();

	/**
	 * Adds a new ComponentItem to this CanvasItem.
	 *
	 * @param label The label to give the new component.
	 * @param descriptor The ComponentDescriptor to create a component from.
	 * @return The newly created ComponentItem.
	 * @throws ComponentCreationException
	 */
	@Nonnull
	public ComponentItem createComponent( @Nonnull String label, @Nonnull ComponentDescriptor descriptor )
			throws ComponentCreationException;

	/**
	 * Get the child components.
	 *
	 * @return A Collection of all the contained Components.
	 */
	@Nonnull
	public Collection<? extends ComponentItem> getComponents();

	/**
	 * Convenience method for finding a child ComponentItem with the given label.
	 * Returns null if no such ComponentItem exists.
	 *
	 * @param label
	 * @return
	 */
	@Nullable
	public ComponentItem getComponentByLabel( @Nonnull String label );

	/**
	 * Gets the connections in this canvas.
	 *
	 * @return A Collection of the Connections in this CanvasItem.
	 */
	@Nonnull
	public Collection<? extends Connection> getConnections();

	/**
	 * Connects an OutputTerminal to an InputTerminal, creating a new Connection.
	 *
	 * @param output The OutputTerminal to connect from.
	 * @param input The InputTerminal to connect to.
	 * @return The newly created Connection.
	 */
	@Nonnull
	public Connection connect( @Nonnull OutputTerminal output, @Nonnull InputTerminal input );

	/**
	 * Gets the current running state of the CanvasItem.
	 *
	 * @return True if the canvas is running, false if it is stopped.
	 */
	public boolean isRunning();

	/**
	 * Gets whether the cavas item has been started or not
	 *
	 * @return True if the canvas has been started, even if it is paused, false
	 *         if it is stopped.
	 */
	public boolean isStarted();

	/**
	 * Gets whether the cavas item has been completed or not
	 *
	 * @return True if the canvas has been completed, false if it is not.
	 */
	public boolean isCompleted();

	/**
	 * Set a limit for a Counter. When the given counter reaches the limit set,
	 * the CanvasItem is stopped.
	 *
	 * @param counterName
	 * @param counterValue
	 */
	public void setLimit( @Nonnull String counterName, long counterValue );

	/**
	 * Get the currently set limit for the given Counter, or -1 if no limit has
	 * been set.
	 *
	 * @param counterName
	 * @return
	 */
	public long getLimit( @Nonnull String counterName );

	/**
	 * Creates and returns the latest Summary. If no Summary is available, this will return null
	 *
	 * @return latest summary or null if none
	 */
	@Nullable
	public Summary getSummary();

	/**
	 * Creates and returns a duplicate of the given CanvasObjectItem (which must
	 * already be a child of the CanvasItem).
	 *
	 * @param obj The child CanvasObjectItem to duplicate.
	 * @return The new copy of the given object.
	 */
	@Nonnull
	public CanvasObjectItem duplicate( @Nonnull CanvasObjectItem obj );

	/**
	 * Used for checking if there were any errors when loading the component.
	 *
	 * @return True if any errors occurred.
	 */
	public boolean isLoadingError();

	/**
	 * Triggers cancel messages for any Components within that are in a busy
	 * state.
	 */
	public void cancelComponents();

	/**
	 * Determines whether ongoing requests should be aborted on finish
	 */
	public boolean isAbortOnFinish();

	/**
	 * Determines whether ongoing requests should be aborted on finish
	 */
	public Property<Boolean> abortOnFinishProperty();

	/**
	 * Used to define if ongoing requests should be aborted on finish
	 *
	 * @param abort True to abort ongoing requests, false otherwise
	 */
	public void setAbortOnFinish( boolean abort );
}
