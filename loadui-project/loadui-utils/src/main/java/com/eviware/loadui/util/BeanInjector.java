/*
 * Copyright 2011 SmartBear Software
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
package com.eviware.loadui.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public enum BeanInjector
{
	INSTANCE;

	private final LoadingCache<Class<?>, Object> beanCache = CacheBuilder.newBuilder().weakValues()
			.build( new CacheLoader<Class<?>, Object>()
			{
				@Override
				public Object load( Class<?> key ) throws Exception
				{
					return INSTANCE.doGetBean( key );
				}
			} );

	@Nonnull
	public static <T> T getBean( @Nonnull final Class<T> cls )
	{
		try
		{
			return cls.cast( INSTANCE.beanCache.get( cls ) );
		}
		catch( ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void setBundleContext( BundleContext arg0 )
	{
		INSTANCE.context = arg0;
		arg0.addServiceListener( new ServiceListener()
		{
			@Override
			public void serviceChanged( ServiceEvent event )
			{
				String[] objectClasses = ( String[] )event.getServiceReference().getProperty( "objectClass" );
				try
				{
					for( String objectClass : objectClasses )
					{
						Class<?> key = Class.forName( objectClass );
						INSTANCE.beanCache.invalidate( key );
					}
				}
				catch( ClassNotFoundException e )
				{
					// Ignore
				}
			}
		} );
		INSTANCE.clearCache();
		INSTANCE.beanCache.put( BundleContext.class, arg0 );
		INSTANCE.waiterLatch.countDown();
	}

	private final CountDownLatch waiterLatch = new CountDownLatch( 1 );

	private volatile BundleContext context;

	private <T> T doGetBean( @Nonnull Class<T> cls )
	{
		try
		{
			if( !waiterLatch.await( 5, TimeUnit.SECONDS ) )
			{
				throw new RuntimeException( "BundleContext is missing, has BeanInjector been configured?" );
			}
		}
		catch( InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}

		ServiceReference<T> ref = context.getServiceReference( cls );
		if( ref != null )
		{
			T service = context.getService( ref );
			if( service != null )
				return service;
		}

		throw new IllegalArgumentException( "No Bean found for class: " + cls );
	}

	protected static class ContextSetter implements BundleContextAware
	{
		@Override
		public void setBundleContext( BundleContext arg0 )
		{
			BeanInjector.setBundleContext( arg0 );
		}
	}

	public void clearCache()
	{
		beanCache.invalidateAll();
	}
}
