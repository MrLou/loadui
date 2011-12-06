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
package com.eviware.loadui.launcher;

import java.io.PrintStream;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class LauncherWatchdog implements Runnable
{
	private final static Logger log = Logger.getLogger( LauncherWatchdog.class.getName() );

	private final Framework framework;
	private final long timeout;
	private final PrintStream ps;

	public LauncherWatchdog( Framework framework, long timeout, PrintStream ps )
	{
		this.framework = framework;
		this.timeout = timeout;
		this.ps = ps;
	}

	@Override
	public void run()
	{
		log.severe( "Watchdog initialized!" );

		try
		{
			Thread.sleep( timeout );
		}
		catch( InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		for( Bundle bundle : framework.getBundleContext().getBundles() )
		{
			switch( bundle.getState() )
			{
			case Bundle.ACTIVE :
				break;
			case Bundle.RESOLVED :
				break;
			default :
				log.severe( String.format( "Bundle: %s state: %s", bundle.getSymbolicName(), bundle.getState() ) );
				ps.printf( "Bundle: %s state: %s", bundle.getSymbolicName(), bundle.getState() );

				log.severe( String.format( "Headers: %s", bundle.getHeaders() ) );
				ps.printf( "Headers: %s", bundle.getHeaders() );

				try
				{
					bundle.start();
				}
				catch( BundleException e )
				{
					e.printStackTrace( ps );
				}

				try
				{
					Thread.sleep( 5000 );
				}
				catch( InterruptedException e )
				{
					e.printStackTrace( ps );
				}

				ps.println( "Exiting..." );
				ps.flush();
				ps.close();

				System.exit( -1 );
				break;
			}
		}

		log.severe( "Watchdog stopping!" );
	}
}
