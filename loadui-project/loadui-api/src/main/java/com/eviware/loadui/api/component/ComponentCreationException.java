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
package com.eviware.loadui.api.component;

public class ComponentCreationException extends Exception
{
	private static final long serialVersionUID = 1299528546052371222L;

	private final String componentType;

	public ComponentCreationException( String type )
	{
		this( type, null, null );
	}

	public ComponentCreationException( String type, String message )
	{
		this( type, message, null );
	}

	public ComponentCreationException( String type, Throwable throwable )
	{
		this( type, null, throwable );
	}

	public ComponentCreationException( String type, String message, Throwable throwable )
	{
		super( message, throwable );

		componentType = type;
	}

	public String getComponentType()
	{
		return componentType;
	}

	@Override
	public String getMessage()
	{
		return "[Component: " + componentType + "]" + super.getMessage();
	}
}
