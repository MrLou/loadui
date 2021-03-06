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
package com.eviware.loadui.impl.statistics.store;

import java.io.File;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;

import com.eviware.loadui.api.testevents.TestEventRegistry;
import com.eviware.loadui.impl.statistics.db.DatabaseMetadata;

public class H2ExecutionManager extends ExecutionManagerImpl<JdbcConnectionPool>
{
	public static final String SQL_CREATE_TABLE_EXPRESSION = "CREATE TABLE";
	public static final String SQL_ADD_PRIMARY_KEY_INDEX_EXPRESSION = "ALTER TABLE ? ADD CONSTRAINT ?_pk_index PRIMARY KEY(?)";
	public static final String SQL_AUTOINCREMENT_PRIMARY_KEY_EXPRESSION = " BIGINT AUTO_INCREMENT PRIMARY KEY";

	public static final String TYPE_INTEGER = "INT";
	public static final String TYPE_BIGINT = "BIGINT";
	public static final String TYPE_DOUBLE = "DOUBLE";
	public static final String TYPE_STRING = "VARCHAR(255)";
	public static final String TYPE_BOOLEAN = "BOOLEAN";
	public static final String TYPE_BINARY = "BLOB";

	public H2ExecutionManager( TestEventRegistry testEventRegistry )
	{
		super( testEventRegistry );
	}

	@Override
	public JdbcConnectionPool createDataSource( String db )
	{
		JdbcConnectionPool pool = JdbcConnectionPool
				.create(
						"jdbc:h2:"
								+ baseDirectoryURI
								+ db
								+ File.separator
								+ db
								+ ";DB_CLOSE_ON_EXIT=FALSE;DATABASE_EVENT_LISTENER='com.eviware.loadui.impl.statistics.store.H2EventListener';TRACE_LEVEL_FILE=2",
						"sa", "sa" );
		pool.setMaxConnections( 25 );

		return pool;
	}

	@Override
	public void releaseDataSource( JdbcConnectionPool dataSource )
	{
		try
		{
			dataSource.dispose();
		}
		catch( SQLException e )
		{
			// do nothing
		}
	}

	@Override
	protected void initializeDatabaseMetadata( DatabaseMetadata metadata )
	{
		metadata.setAddPrimaryKeyIndexExpression( SQL_ADD_PRIMARY_KEY_INDEX_EXPRESSION );
		metadata.setCreateTableExpression( SQL_CREATE_TABLE_EXPRESSION );
		metadata.setAutoIncrementPKExpression( SQL_AUTOINCREMENT_PRIMARY_KEY_EXPRESSION );

		metadata.addTypeConversionPair( Integer.class, TYPE_INTEGER );
		metadata.addTypeConversionPair( Long.class, TYPE_BIGINT );
		metadata.addTypeConversionPair( Double.class, TYPE_DOUBLE );
		metadata.addTypeConversionPair( String.class, TYPE_STRING );
		metadata.addTypeConversionPair( Boolean.class, TYPE_BOOLEAN );
		metadata.addTypeConversionPair( Byte[].class, TYPE_BINARY );
	}

}
