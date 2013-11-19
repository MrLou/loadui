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
package com.eviware.loadui.components.soapui.utils;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.integration.SoapUIProjectLoader;
import com.eviware.soapui.SoapUIExtensionClassLoader;
import com.eviware.soapui.SoapUIExtensionClassLoader.SoapUIClassLoaderState;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlProjectPro;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.*;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.support.GroovyUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

public class SoapUiProjectUtils
{
	private static final Logger log = LoggerFactory.getLogger( SoapUiProjectUtils.class );

	private static final ImmutableMap<String, String> namePattern_to_driverString = new ImmutableMap.Builder<String, String>()
			.put( "mysql.*\\.jar", "com.mysql.jdbc.Driver" ).put( "postgresql.*\\.jar", "org.postgresql.Driver" )
			.put( "derbyclient\\.jar", "org.apache.derby.jdbc.ClientDriver" )
			.put( "derby\\.jar", "org.apache.derby.jdbc.EmbeddedDriver" )
			.put( "sqljdbc.*\\.jar", "com.microsoft.sqlserver.jdbc.SQLServerDriver" )
			.put( "ojdbc.*\\.jar", "oracle.jdbc.driver.OracleDriver" )
			.put( "jconn.*\\.jar", "com.sybase.jdbc2.jdbc.SybDriver" )
			.put( "pbclient\\.jar", "com.pointbase.jdbc.jdbcUniversalDriver" )
			.put( "jaybird.*\\.jar", "org.firebirdsql.jdbc.FBDriver" )
			.put( "interclient.*\\.jar", "interbase.interclient.Driver" ).build();

	/**
	 * Disables all soapUI assertions for the specified TestCase.
	 *
	 * @param testCase to disable assertions for
	 */
	public static void disableSoapUIAssertions( @Nonnull TestCase testCase )
	{
		for( Assertable assertableStep : Iterables.filter( testCase.getTestStepList(), Assertable.class ) )
			for( TestAssertion assertion : assertableStep.getAssertionList() )
				( ( WsdlMessageAssertion )assertion ).setDisabled( true );
	}

	/**
	 * If project is composite, this will create non composite copy and return
	 * copy referencing it. If project is non composite same file will be
	 * returned.
	 *
	 * @param originalProjectFile SoapUI project that needs to be checked if it is composite or
	 * not.
	 * @return Reference to non-composite soapUi project
	 */
	public static File makeNonCompositeCopy( File originalProjectFile )
	{
		File result = originalProjectFile;
		if( originalProjectFile != null )
		{
			SoapUIClassLoaderState state = SoapUIExtensionClassLoader.ensure();
			WsdlProject project;
			try
			{
				log.info( "Loading project located at: {}", originalProjectFile.getAbsolutePath() );
				project = SoapUIProjectLoader.getInstance().getProject( originalProjectFile.getAbsolutePath() );

				if( project instanceof WsdlProjectPro )
				{
					Boolean composite = ( ( WsdlProjectPro )project ).isComposite();
					if( composite )
					{
						log.info( "Making composite project non-composite so LoadUI can run it: {}", project.getPath() );
						( ( WsdlProjectPro )project ).setComposite( false );
						project.save();
						// non composite project must be unloaded here
						SoapUIProjectLoader.getInstance().releaseProject( project );
						result = new File( project.getPath() );
					}
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
			finally
			{
				state.restore();
			}
		}
		return result;
	}

	public static void makeAllDataSourcesShared( @Nonnull WsdlTestCase testCase )
	{
		for( WsdlDataSourceTestStep s : testCase.getTestStepsOfType( WsdlDataSourceTestStep.class ) )
			s.setShared( true );
	}

	public static void disableAllDataSourceLoops( @Nonnull WsdlTestCase testCase )
	{
		for( WsdlDataSourceLoopTestStep s : testCase.getTestStepsOfType( WsdlDataSourceLoopTestStep.class ) )
			s.setDisabled( true );
	}

	public static void clearResponse( @Nonnull WsdlTestCase testCase )
	{
		for( TestStep s : testCase.getTestStepList() )
		{
			if( s instanceof AMFRequestTestStep )
				( ( AMFRequestTestStep )s ).setResponse( null, null );
			else if( s instanceof HttpTestRequestStep )
				( ( HttpTestRequestStep )s ).getTestRequest().setResponse( null, null );
			else if( s instanceof JdbcRequestTestStep )
				( ( JdbcRequestTestStep )s ).setResponse( null, null );
			else if( s instanceof RestTestRequestStep )
				( ( RestTestRequestStep )s ).getTestRequest().setResponse( null, null );
			else if( s instanceof WsdlTestRequestStep )
				( ( WsdlTestRequestStep )s ).getTestRequest().setResponse( null, null );
		}
	}

	public static void registerJdbcDrivers()
	{
		log.debug( "Registering JDBC Drivers." );
		File extDirectory = LoadUI.relativeFile( "ext" );

		if( extDirectory.isDirectory() )
		{
			for( final String fileName : Arrays.asList( extDirectory.list() ) )
			{
				log.debug( "Analyzing filename: " + fileName );
				for( String namePattern : namePattern_to_driverString.keySet() )
				{
					if( fileName.matches( namePattern ) )
					{
						log.debug( "Registering JDBC Driver: " + namePattern_to_driverString.get( namePattern ) );
						GroovyUtils.registerJdbcDriver( namePattern_to_driverString.get( namePattern ) );
					}
				}
			}
		}
	}
}
