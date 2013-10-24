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
package com.eviware.loadui.ui.fx.views.assertions;

import static com.eviware.loadui.ui.fx.control.SettingsDialog.VERTICAL_SPACING;
import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.CONVERTABLE_TO_LONG;
import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.EMPTY_TO_NEGATIVE_ONE;
import static com.eviware.loadui.ui.fx.control.fields.ValidatableLongField.IS_EMPTY;
import static com.google.common.base.Predicates.or;
import static javafx.beans.binding.Bindings.and;
import static javafx.beans.binding.Bindings.when;

import javax.annotation.Nullable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TitledPaneBuilder;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.util.Pair;

import com.eviware.loadui.api.assertion.Constraint;
import com.eviware.loadui.ui.fx.control.fields.Validatable;
import com.eviware.loadui.ui.fx.control.fields.ValidatableLongField;
import com.eviware.loadui.util.assertion.RangeConstraint;
import com.google.common.base.Predicate;

public class ConstraintPane extends VBox implements Validatable
{
	private final ValidatableLongField minField;
	private final ValidatableLongField maxField;
	private final BooleanProperty isValidProperty = new SimpleBooleanProperty( false );
	private final ValidatableLongField timesAllowed;
	private final ValidatableLongField timeWindow;

	public ConstraintPane()
	{
		minField = ValidatableLongField.Builder.create().id("min").build();
		VBox minBox = VBoxBuilder.create().spacing( VERTICAL_SPACING ).children( new Label( "Min" ), minField ).build();

		maxField = ValidatableLongField.Builder.create().id("max").longConstraint( new Predicate<Long>()
		{
			@Override
			public boolean apply( @Nullable Long input )
			{
				return minField.getFieldValue() <= maxField.getFieldValue();
			}
		} ).build();
		VBox maxBox = VBoxBuilder.create().spacing( VERTICAL_SPACING ).children( new Label( "Max" ), maxField ).build();

		Label constrainLabel = LabelBuilder.create().text( "Constraint" ).build();
		constrainLabel.getStyleClass().add( "strong" );

		HBox constraintFields = HBoxBuilder.create().spacing( 26.0 ).styleClass( "constraints" ).children( minBox, maxBox ).build();

		timesAllowed = ValidatableLongField.Builder.create().text( "0" ).build();
		timeWindow = ValidatableLongField.Builder.create().convertFunction( EMPTY_TO_NEGATIVE_ONE )
				.stringConstraint( or( IS_EMPTY, CONVERTABLE_TO_LONG ) ).build();
		timeWindow.disableProperty().bind(
				when( timesAllowed.textProperty().isEqualTo( "0" ) ).then( true ).otherwise( false ) );
		HBox tolerancePane = HBoxBuilder.create()
				.children( timesAllowed, new Label( "times, within" ), timeWindow, new Label( "seconds" ) ).build();
		TitledPane advancedPane = TitledPaneBuilder.create().text( "Advanced" ).expanded( false ).content( tolerancePane )
				.build();

		setSpacing( 16.0 );
		getChildren().setAll( constrainLabel, constraintFields, advancedPane );

		isValidProperty.bind( and( minField.isValidProperty(), maxField.isValidProperty() ) );
	}

	@Override
	public ReadOnlyBooleanProperty isValidProperty()
	{
		return isValidProperty;
	}

	@Override
	public boolean isValid()
	{
		return isValidProperty.get();
	}

	public Constraint<Number> getConstraint()
	{
		return new RangeConstraint( minField.getFieldValue(), maxField.getFieldValue() );
	}

	public Pair<Integer, Integer> getTolerance()
	{
		return new Pair<Integer, Integer>( timesAllowed.getFieldValue().intValue(), timeWindow.getFieldValue().intValue() );
	}
}
