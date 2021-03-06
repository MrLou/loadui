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
package com.eviware.loadui.ui.fx.control;

import java.util.Comparator;

import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;

import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;

@DefaultProperty( "items" )
public class ToolBox<E extends Node> extends Control
{
	private static final String DEFAULT_STYLE_CLASS = "tool-box";
	private static final String TOOL_BOX_PROPERTY = "tool-box";
	private static final String CATEGORY_PROPERTY = "tool-box-category";

	/**
	 * Sets the category for a child when contained by a ToolBox. When no
	 * category has been set, or it has been explicitly set to null, the child's
	 * toString() value will be used as a category.
	 * 
	 * @param node
	 * @param category
	 */
	public static void setCategory( Node node, String category )
	{
				
		Object oldCategory = node.getProperties().get( CATEGORY_PROPERTY );
		
		@SuppressWarnings( "unchecked" )
		ToolBox<Node> toolBox = ( ToolBox<Node> )node.getProperties().get( TOOL_BOX_PROPERTY );
				
		if( toolBox != null && oldCategory != null )
			toolBox.getItems().remove( node );
				
		node.getProperties().put( CATEGORY_PROPERTY, category );
		if( toolBox != null && !toolBox.getItems().contains( node ) )
			toolBox.getItems().add( node );
		
	}

	/**
	 * Returns the child's Category.
	 * 
	 * @param node
	 * @return
	 */
	public static String getCategory( Node node )
	{
		Object category = node.getProperties().get( CATEGORY_PROPERTY );
		return String.valueOf( category == null ? node : category );
	}

	private final Label label;
	private final ObservableList<E> items = FXCollections.observableArrayList();
	private final ObjectProperty<Comparator<String>> categoryComparator = new SimpleObjectProperty<>( this,
			"categoryComparator" );
	private final ObservableMap<String, Comparator<? super E>> itemComparators = FXCollections.observableHashMap();

	public ToolBox()
	{
		this.label = new Label();
		
		initialize();
	}

	public ToolBox( String label )
	{
		this.label = new Label( label );
		initialize();
	}

	public ToolBox( String label, Node graphic )
	{
		this.label = new Label( label, graphic );
		initialize();
	}

	public void initialize()
	{
		getStyleClass().setAll( DEFAULT_STYLE_CLASS );

		label.getStyleClass().add( "title" );

		itemComparators.put( null, Ordering.usingToString() );

		items.addListener( new ListChangeListener<E>()
		{
			@Override
			public void onChanged( ListChangeListener.Change<? extends E> change )
			{
				while( change.next() )
				{
					for( E node : change.getRemoved() )
					{
						node.getProperties().remove( TOOL_BOX_PROPERTY );
					}

					for( E node : change.getAddedSubList() )
					{
						
						node.getProperties().put( TOOL_BOX_PROPERTY, ToolBox.this );
					}
				}
			}
		} );
	}

	public Label getLabel()
	{
		return label;
	}

	public StringProperty textProperty()
	{
		return getLabel().textProperty();
	}

	public String getText()
	{
		return getLabel().getText();
	}

	public void setText( String text )
	{
		getLabel().setText( text );
	}

	public Node getGraphic()
	{
		return getLabel().getGraphic();
	}

	public void setGraphic( Node graphic )
	{
		getLabel().setGraphic( graphic );
	}

	public ObservableList<E> getItems()
	{
		return items;
	}

	public ObjectProperty<Comparator<String>> categoryComparatorProperty()
	{
		return categoryComparator;
	}

	public void setCategoryComparator( Comparator<String> categoryComparator )
	{
		this.categoryComparator.set( categoryComparator );
	}

	public ObservableMap<String, Comparator<? super E>> getComparators()
	{
		return itemComparators;
	}

	public Comparator<String> getCategoryComparator()
	{
		return categoryComparator.get();
	}

	public void setComparator( Comparator<? super E> comparator )
	{
		setComparator( null, comparator );
	}

	public void setComparator( String category, Comparator<? super E> comparator )
	{
		itemComparators.put( category, comparator );
	}

	public Comparator<? super E> getComparator()
	{
		return itemComparators.get( null );
	}

	public Comparator<? super E> getComparator( String category )
	{
		if( itemComparators.containsKey( category ) )
		{
			return itemComparators.get( category );
		}
		return getComparator();
	}

	private final DoubleProperty heightPerItem = new SimpleDoubleProperty( this, "heightPerItem", 120.0 );

	public DoubleProperty heightPerItemProperty()
	{
		return heightPerItem;
	}

	public double getHeightPerItem()
	{
		return heightPerItem.get();
	}

	public void setHeightPerItem( double value )
	{
		Preconditions.checkArgument( value > 0, "heightPerItem must be >0, was %d", value );
		heightPerItem.set( value );
	}
}
