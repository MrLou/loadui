/*
 * Copyright 2011 eviware software ab
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EventObject;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.loadui.api.events.BaseEvent;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.model.ProjectRef;
import com.eviware.loadui.config.ProjectReferenceConfig;
import com.eviware.loadui.impl.property.AttributeHolderSupport;
import com.eviware.loadui.util.events.EventSupport;

public class ProjectRefImpl implements ProjectRef
{
	public static final Logger log = LoggerFactory.getLogger( ProjectRefImpl.class );

	private final WorkspaceItemImpl workspace;
	private final ProjectReferenceConfig config;
	private final EventSupport eventSupport = new EventSupport();
	private final AttributeHolderSupport attributeHolderSupport;
	private File projectFile;
	private ProjectItem project;

	public ProjectRefImpl( WorkspaceItemImpl workspace, ProjectReferenceConfig config ) throws IOException
	{
		this.workspace = workspace;
		this.config = config;
		if( config.getAttributes() == null )
			config.addNewAttributes();

		attributeHolderSupport = new AttributeHolderSupport( config.getAttributes() );
		projectFile = new File( config.getProjectFile() );
		if( isEnabled() )
			loadProject();
	}

	public ProjectReferenceConfig getConfig()
	{
		return config;
	}

	@Override
	public String getLabel()
	{
		if( project != null )
			config.setLabel( project.getLabel() );
		return config.getLabel();
	}

	private void setLabel( String label )
	{
		if( label != null && ( getLabel() == null || !getLabel().equals( label ) ) )
		{
			config.setLabel( label );
			eventSupport.fireEvent( new BaseEvent( this, LABEL ) );
		}
	}

	@Override
	public ProjectItem getProject()
	{
		return project;
	}

	@Override
	public File getProjectFile()
	{
		return projectFile;
	}

	@Override
	public boolean isEnabled()
	{
		return config.getEnabled();
	}

	private void loadProject() throws IOException
	{
		if( project != null )
			return;

		try
		{
			project = ProjectItemImpl.loadProject( workspace, projectFile );
			setLabel( project.getLabel() );
			fireEvent( new BaseEvent( this, LOADED ) );
			workspace.projectLoaded( project );
		}
		catch( XmlException e )
		{
			throw new IOException( e );
		}
	}

	@Override
	public void setEnabled( boolean enabled ) throws IOException
	{
		if( isEnabled() != enabled )
		{
			try
			{
				if( enabled )
					loadProject();
				else
				{
					project.release();
					project = null;
					eventSupport.fireEvent( new BaseEvent( this, UNLOADED ) );
				}
				config.setEnabled( enabled );
			}
			catch( IOException e )
			{
				log.error( "Unable to load Project: " + projectFile.getAbsolutePath(), e );
				throw e;
			}
		}
	}

	@Override
	public void delete( boolean deleteFile )
	{
		if( deleteFile )
		{
			if( project != null )
			{
				project.delete();
			}
			else
			{
				log.info( "Removing project '{}' from Workspace and deleting the file '{}'.", getLabel(), projectFile );
				projectFile.delete();
				workspace.removeProject( this );
			}
		}
		else
		{
			log.info( "Removing project '{}' from Workspace.", getLabel() );
			workspace.removeProject( this );
		}
	}

	@Override
	public <T extends EventObject> void addEventListener( Class<T> type, EventHandler<T> listener )
	{
		eventSupport.addEventListener( type, listener );
	}

	@Override
	public <T extends EventObject> void removeEventListener( Class<T> type, EventHandler<T> listener )
	{
		eventSupport.removeEventListener( type, listener );
	}

	@Override
	public void fireEvent( EventObject event )
	{
		eventSupport.fireEvent( event );
	}

	@Override
	public void clearEventListeners()
	{
		eventSupport.clearEventListeners();
	}

	@Override
	public String getAttribute( String key, String defaultValue )
	{
		return attributeHolderSupport.getAttribute( key, defaultValue );
	}

	@Override
	public void setAttribute( String key, String value )
	{
		attributeHolderSupport.setAttribute( key, value );
	}

	@Override
	public void removeAttribute( String key )
	{
		attributeHolderSupport.removeAttribute( key );
	}

	@Override
	public Collection<String> getAttributes()
	{
		return attributeHolderSupport.getAttributes();
	}
}