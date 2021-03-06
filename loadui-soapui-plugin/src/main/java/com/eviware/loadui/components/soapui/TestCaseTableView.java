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
package com.eviware.loadui.components.soapui;

import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.components.soapui.utils.PropertyOverrider;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Callable;

final public class TestCaseTableView extends VBox
{

	protected static final Logger log = LoggerFactory.getLogger( TestCaseTableView.class );

	/**
	 * Creates a tableView that displays editable Properties from the given
	 * ComponentContext and TestCase.
	 *
	 * @param component
	 * @param context
	 * @return
	 */

	public static Callable<Node> createTableView( final SoapUISamplerComponent component, final ComponentContext context )
	{
		return new Callable<Node>()
		{
			@Override
			public Node call() throws Exception
			{
				PropertiesTableView table = new PropertiesTableView( context );
				TestCase testCase = component.getTestCase();

				if( testCase != null )
				{
					table.getItems().setAll(
							PropertyOverrider.applyOveriddenProperties( testCase.getPropertyList(), context ) );
				}
				else
				{
					table.getItems().clear();
				}

				TestCaseTableView node = new TestCaseTableView();
				node.getChildren().addAll( new Label( "TestCase Properties" ), table );
				return node;
			}
		};
	}

	@Immutable
	private static class PropertiesTableView extends TableView<TestProperty>
	{

		private PropertiesTableView( final ComponentContext context )
		{
			this.setEditable( true );

			TableColumn<TestProperty, String> keyColumn = new TableColumn<>( "Property" );
			TableColumn<TestProperty, String> valueColumn = new TableColumn<>( "Value" );

			keyColumn.prefWidthProperty().set( 100 );
			valueColumn.prefWidthProperty().set( 200 );

			keyColumn
					.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<TestProperty, String>, ObservableValue<String>>()
					{

						@Override
						public ObservableValue<String> call( CellDataFeatures<TestProperty, String> data )
						{
							return new ReadOnlyStringWrapper( data.getValue().getName() );
						}
					} );

			valueColumn
					.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<TestProperty, String>, ObservableValue<String>>()
					{

						@Override
						public ObservableValue<String> call( CellDataFeatures<TestProperty, String> data )
						{
							ReadOnlyStringWrapper stringWrapper = new ReadOnlyStringWrapper( data.getValue().getValue() );

							return stringWrapper;
						}
					} );

			valueColumn.setCellFactory( new Callback<TableColumn<TestProperty, String>, TableCell<TestProperty, String>>()
			{

				@Override
				public EditableTextCell call( TableColumn<TestProperty, String> arg0 )
				{
					return new EditableTextCell();
				}
			} );

			valueColumn.setOnEditCommit( new EventHandler<TableColumn.CellEditEvent<TestProperty, String>>()
			{

				@Override
				public void handle( CellEditEvent<TestProperty, String> event )
				{
					log.info( "Setting property {} to {}", event.getRowValue().getName(), event.getNewValue() );
					setOrCreateContextProperty( context, event.getRowValue().getName(), event.getNewValue() );

				}

			} );

			valueColumn.setEditable( true );

			this.getColumns().setAll( keyColumn, valueColumn );
			log.debug( "Created properties table {}", this.toString() );
		}

		private void setOrCreateContextProperty( ComponentContext context, String name, String value )
		{
			String propertyName = PropertyOverrider.OVERRIDING_VALUE_PREFIX + name;

			if( context.getProperty( propertyName ) == null )
			{
				context.createProperty( propertyName, String.class, value );
			}
			else
			{
				context.getProperty( propertyName ).setValue( value );
			}

		}

	}

}
