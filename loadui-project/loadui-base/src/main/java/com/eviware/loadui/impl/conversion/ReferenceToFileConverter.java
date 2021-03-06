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
package com.eviware.loadui.impl.conversion;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.api.addressable.AddressableRegistry;
import com.eviware.loadui.api.events.CollectionEvent;
import com.eviware.loadui.api.events.EventHandler;
import com.eviware.loadui.api.events.PropertyEvent;
import com.eviware.loadui.api.messaging.MessageEndpoint;
import com.eviware.loadui.api.messaging.MessageListener;
import com.eviware.loadui.api.model.PropertyHolder;
import com.eviware.loadui.impl.property.Reference;

public class ReferenceToFileConverter implements Converter<Reference, File>, EventHandler<CollectionEvent>
{
	public final static Logger log = LoggerFactory.getLogger( ReferenceToFileConverter.class );

	public final static String CHANNEL = "/" + ReferenceToFileConverter.class.getName();

	private final File storage = new File( System.getProperty( LoadUI.LOADUI_HOME ) + File.separator + "fileStorage" );
	private final FilePropertyListener filePropertyListener = new FilePropertyListener();
	private final Set<File> filesInUse = new HashSet<>();

	private final Map<String, File> files = new HashMap<>();
	private final FileReceiver listener = new FileReceiver();

	private final Set<String> filesInProgress = new HashSet<>();

	public ReferenceToFileConverter( AddressableRegistry addressableRegistry, ScheduledExecutorService executorService )
	{
		addressableRegistry.addEventListener( CollectionEvent.class, this );
		executorService.scheduleAtFixedRate( new RemoveOldFilesTask(), 5, 5, TimeUnit.MINUTES );

		if( !storage.isDirectory() )
			if( !storage.mkdirs() )
				throw new RuntimeException( "Unable to create path: " + storage.getAbsolutePath() );
	}

	@Override
	public File convert( Reference source )
	{
		if( source.getId().charAt( 0 ) == ':' )
			return new File( source.getId().substring( 1 ) );

		File target = getOrCreate( source );
		String hash = source.getId();

		synchronized( target )
		{
			while( !target.exists() || filesInProgress.contains( hash ) )
			{
				try
				{
					log.debug( "waiting for {}", source.getId() );
					target.wait();
				}
				catch( InterruptedException e )
				{
					log.debug( "got waken up, was waiting for {}", source.getId() );
				}
			}

			try (FileInputStream fis = new FileInputStream( target ) )
			{
				String md5Hex = DigestUtils.md5Hex( fis );
				log.debug( "target is: {} and is {} bytes with hash " + md5Hex, target, target.length() );
			}
			catch( IOException e )
			{
				log.error( "Exception while verifying hash", e );
				e.printStackTrace();
			}
		}

		return target;
	}

	private File getOrCreate( Reference source )
	{
		String hash = source.getId();
		synchronized( files )
		{
			log.debug( "getOrCreate() {}", hash );

			if( !files.containsKey( hash ) )
			{
				files.put( hash, new File( storage, hash ) );
				log.debug( "Adding {} to filesInProgress", hash );
				filesInProgress.add( hash );
				source.getEndpoint().addMessageListener( CHANNEL, listener );
				source.getEndpoint().sendMessage( FileToReferenceConverter.CHANNEL, hash );
			}
			else if( !filesInProgress.contains( hash ) && !isFileHashValid( hash ) )
			{
				log.error( "File has been changed. Request file again..." );
				log.debug( "Removing {} from filesInProgress", hash );
				if( !files.get( hash ).delete() )
					log.error( "Unable to delete file: {}", files.get( hash ) );
				source.getEndpoint().addMessageListener( CHANNEL, listener );
				source.getEndpoint().sendMessage( FileToReferenceConverter.CHANNEL, hash );
			}
			return files.get( hash );
		}
	}

	private boolean isFileHashValid( String hash )
	{

		try (FileInputStream fis = new FileInputStream( files.get( hash ) ))
		{
			String md5Hex = DigestUtils.md5Hex( fis );
			return hash.equals( md5Hex );
		}
		catch( IOException e )
		{
			return false;
		}
	}

	@Override
	public void handleEvent( CollectionEvent event )
	{
		Object element = event.getElement();
		if( element instanceof PropertyHolder )
		{
			PropertyHolder propertyHolder = ( PropertyHolder )element;
			if( event.getEvent() == CollectionEvent.Event.ADDED )
				propertyHolder.addEventListener( PropertyEvent.class, filePropertyListener );
			else
				propertyHolder.removeEventListener( PropertyEvent.class, filePropertyListener );
		}
	}

	private class FileReceiver implements MessageListener
	{
		private final Map<String, OutputStream> writers = Collections
				.synchronizedMap( new HashMap<String, OutputStream>() );

		@Override
		@SuppressWarnings( "unchecked" )
		public void handleMessage( String channel, MessageEndpoint endpoint, Object data )
		{
			Map<String, Object> map = ( Map<String, Object> )data;
			for( Entry<String, Object> entry : map.entrySet() )
			{
				String hash = entry.getKey();
				synchronized( files )
				{
					if( files.containsKey( hash ) )
					{
						File file = files.get( hash );
						if( FileToReferenceConverter.START.equals( entry.getValue() ) )
						{
							try
							{
								if( !file.createNewFile() )
									log.debug( "Failed creating file: " + file.getAbsolutePath() );
								writers.put( hash, new FileOutputStream( file ) );
							}
							catch( FileNotFoundException e )
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//
							catch( IOException e )
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else if( FileToReferenceConverter.STOP.equals( entry.getValue() ) )
						{
							closeQuietly(  writers.remove( hash ) );
							synchronized( file )
							{
								try ( FileInputStream fis = new FileInputStream( file ) )
								{
									String md5Hex = DigestUtils.md5Hex( fis );
									if( hash.equals( md5Hex ) )
									{
										log.debug( "File done. Removing {} from filesInProgress", hash );
										filesInProgress.remove( hash );
										file.notifyAll();
									}
									else
									{
										log.error( "File transfered with MD5 hash: {}, should be {}. Retrying...", md5Hex, hash );

										if( !file.delete() )
											log.error( "Failed to delete file: " + file.getAbsolutePath() );

										endpoint.sendMessage( FileToReferenceConverter.CHANNEL, hash );
									}
								}
								catch( IOException e )
								{
									log.error( "Exception while verifying completed file", e );
								}
							}
						}
						else if( writers.containsKey( hash ) )
						{
							try
							{
								writers.get( hash ).write( Base64.decodeBase64( ( String )entry.getValue() ) );
							}
							catch( IOException e )
							{
								log.error( "Exception while writing to file", e );
							}
						}
					}
				}
			}
		}
	}

	private void closeQuietly( Closeable closeable ){
		try{
			 closeable.close();
		}catch( IOException e){
			//Quiet close
		}
	}

	private class FilePropertyListener implements EventHandler<PropertyEvent>
	{
		@Override
		public void handleEvent( PropertyEvent event )
		{
			if( File.class.isAssignableFrom( event.getProperty().getType() ) )
			{
				if( event.getEvent() == PropertyEvent.Event.CREATED )
					filesInUse.add( ( File )event.getProperty().getValue() );
				else if( event.getEvent() == PropertyEvent.Event.VALUE )
				{
					filesInUse.remove( event.getPreviousValue() );
					filesInUse.add( ( File )event.getProperty().getValue() );
				}
				else if( event.getEvent() == PropertyEvent.Event.DELETED )
					filesInUse.remove( event.getPreviousValue() );
			}
		}
	}

	private class RemoveOldFilesTask implements Runnable
	{
		// max storage size in MB
		public static final int MAX_STORAGE_SIZE = 100;

		private final Comparator<File> compareByLastUsed = new Comparator<File>()
		{
			@Override
			public int compare( File o1, File o2 )
			{
				return ( int )( o1.lastModified() - o2.lastModified() );
			}
		};

		@Override
		public void run()
		{
			List<File> unused = new ArrayList<>();
			for( File file : storage.listFiles() )
				if( !filesInUse.contains( file ) )
					unused.add( file );

			Collections.sort( unused, compareByLastUsed );

			long limit = MAX_STORAGE_SIZE * 1024 * 1024;
			long size = 0;
			for( int i = unused.size() - 1; i >= 0; i-- )
			{
				size += unused.get( i ).length();
				if( size > limit )
				{
					int newSize = unused.size() - i - 1;
					while( unused.size() > newSize )
					{
						final File file = unused.remove( 0 );
						if( !file.delete() )
							log.error( "Unable to remove file: {}", file );
					}
					break;
				}
			}
		}
	}
}
