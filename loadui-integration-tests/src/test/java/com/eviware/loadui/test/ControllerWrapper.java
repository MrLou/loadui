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
package com.eviware.loadui.test;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * An embedded headless loadUI Controller which can be used for testing. All
 * packages in the loadui-api bundle are exported into the runtime so that they
 * can be used in tests. The loadui-fx-interface bundle is removed due to its
 * dependency on JavaFX.
 * 
 * @author dain.nilsson
 */
public class ControllerWrapper
{
	private final File baseDir = new File( "target/controllerTest" );
	private final OSGiLauncher launcher;
	private final BundleContext context;

	public ControllerWrapper() throws Exception
	{
		if( baseDir.exists() && !Utilities.deleteRecursive( baseDir ) )
			throw new RuntimeException( "Test directory already exists and cannot be deleted!" );

		if( !baseDir.mkdir() )
			throw new RuntimeException( "Could not create test directory!" );

		launcher = new OSGiLauncher( new String[] { "-nolock", "-nofx" } );
		Properties config = launcher.getConfig();
		config.setProperty( "felix.cache.rootdir", baseDir.getAbsolutePath() );
		File bundleDir = new File( baseDir, "bundle" );
		Utilities.copyDirectory( new File( "../loadui-controller-deps/target/bundle" ), bundleDir );

		// osgi = new OSGiWrapper();
		// Properties config = osgi.getConfig();
		// config.setProperty( "felix.cache.rootdir", baseDir.getAbsolutePath() );
		// File bundleDir = new File( baseDir, "bundle" );
		// Utilities.copyDirectory( new File(
		// "../loadui-controller-deps/target/bundle" ), bundleDir );

		// Remove bundles depending on JavaFX and the API bundle.
		for( File bundle : bundleDir.listFiles() )
		{
			if( bundle.getName().startsWith( "loadui-fx-" ) )
				bundle.delete();
			else if( bundle.getName().startsWith( "loadui-api" ) )
			{
				ZipFile api = new ZipFile( bundle );
				Set<String> packages = new TreeSet<String>();
				for( Enumeration<? extends ZipEntry> e = api.entries(); e.hasMoreElements(); )
				{
					ZipEntry entry = e.nextElement();
					if( entry.getName().endsWith( ".class" ) )
					{
						packages.add( entry.getName().substring( 0, entry.getName().lastIndexOf( "/" ) )
								.replaceAll( "/", "." ) );
					}
				}
				StringBuilder apiPackages = new StringBuilder();
				for( String pkg : packages )
					apiPackages.append( "," ).append( pkg );

				config.put( "org.osgi.framework.system.packages.extra", apiPackages.toString().substring( 1 ) );
				api.close();

				bundle.delete();
			}
		}

		config.setProperty( "felix.auto.deploy.dir", bundleDir.getAbsolutePath() );

		launcher.init();
		launcher.start();
		context = launcher.getBundleContext();
	}

	public void stop() throws BundleException
	{
		try
		{
			launcher.stop();
		}
		catch( BundleException e )
		{
			throw e;
		}
		finally
		{
			Utilities.deleteRecursive( baseDir );
		}
	}

	public BundleContext getBundleContext()
	{
		return context;
	}
}