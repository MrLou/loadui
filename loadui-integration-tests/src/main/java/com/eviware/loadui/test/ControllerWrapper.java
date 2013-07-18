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
package com.eviware.loadui.test;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.eviware.loadui.LoadUI;

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
	private final File homeDir = new File( baseDir, ".loadui" );
	private final OSGiLauncher launcher;
	private final BundleContext context;

	public ControllerWrapper() throws Exception
	{
		if( baseDir.exists() && !IntegrationTestUtils.deleteRecursive( baseDir ) )
			throw new RuntimeException( "Test directory already exists and cannot be deleted!" );

		if( !baseDir.mkdir() )
			throw new RuntimeException( "Could not create test directory!" );
		if( !homeDir.mkdir() )
			throw new RuntimeException( "Could not create home directory!" );
		System.setProperty( LoadUI.LOADUI_HOME, homeDir.getAbsolutePath() );

		launcher = new OSGiLauncher( new String[] { "-nolock" } );
		Properties config = launcher.getConfig();
		config.setProperty( "felix.cache.rootdir", baseDir.getAbsolutePath() );

		File bundleDir = new File( baseDir, "bundle" );
		IntegrationTestUtils.copyDirectory( new File( "../loadui-project/loadui-controller-deps/target/bundle" ), bundleDir );
		IntegrationTestUtils.copyDirectory( new File( "target/bundle" ), bundleDir );

		// Remove bundles depending on JavaFX and the API bundle.
		for( File bundle : bundleDir.listFiles() )
		{
			if( bundle.getName().contains( "loadui-fx" ) || bundle.getName().contains( "groovy-component" ) )
			{
				if( !bundle.delete() )
					throw new IOException( "Unable to delete file: " + bundle );
			}
			else if( bundle.getName().startsWith( "loadui-api" ) )
			{
				//FIXME we do this because tests are not currently run inside a genuine OSGi environment
				// We need to use something like pax-exam to create a OSGi test environment
				try (ZipFile api = new ZipFile( bundle ))
				{
					Set<String> packages = new TreeSet<>();
					for( Enumeration<? extends ZipEntry> e = api.entries(); e.hasMoreElements(); )
					{
						ZipEntry entry = e.nextElement();
						if( entry.getName().endsWith( ".class" ) )
						{
							packages.add( entry.getName().substring( 0, entry.getName().lastIndexOf( "/" ) )
									.replaceAll( "/", "." ) );
						}
					}

					//Add the required packages that should be in the OSGi config file.
					StringBuilder apiPackages = new StringBuilder(
							"com.sun.crypto.provider,com.sun.net.ssl,com.sun.net.ssl.internal.ssl,org.w3c.dom.traversal,javax.transaction.xa;version=1.1.0,sun.io,org.antlr.runtime,org.antlr.runtime.tree" );

					int dashIndex = LoadUI.version().indexOf( "-" );
					String version = dashIndex < 0 ? LoadUI.version() : LoadUI.version().substring( 0, dashIndex );
					for( String pkg : packages )
						apiPackages.append( ", " ).append( pkg ).append( "; version=\"" ).append( version ).append( '"' );

					config.put( "org.osgi.framework.system.packages.extra", apiPackages.toString() );
				}

				if( !bundle.delete() )
					throw new IOException( "Unable to delete file: " + bundle );
			}
		}

		config.setProperty( "felix.auto.deploy.dir", bundleDir.getAbsolutePath() );
		System.setProperty( LoadUI.LOADUI_WORKING, baseDir.getAbsolutePath() );

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
		finally
		{
			IntegrationTestUtils.deleteRecursive( baseDir );
		}
	}

	public BundleContext getBundleContext()
	{
		return context;
	}
}
