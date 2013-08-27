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

import static javafx.beans.binding.Bindings.and;
import static javafx.beans.binding.Bindings.not;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.loadui.api.assertion.Constraint;
import com.eviware.loadui.api.serialization.ListenableValue;
import com.eviware.loadui.api.statistics.StatisticHolder;
import com.eviware.loadui.api.traits.Labeled;
import com.eviware.loadui.ui.fx.control.ConfirmationDialog;

public class CreateAssertionDialog extends ConfirmationDialog
{
	protected static final Logger log = LoggerFactory.getLogger( CreateAssertionDialog.class );
	private final AssertableTree tree;
	private final ConstraintPane constraintPane;

	public CreateAssertionDialog( Node owner, StatisticHolder holder )
	{
		super( owner, "Create Assertion", "Create" );

		tree = AssertableTree.forHolder( holder );
		constraintPane = new ConstraintPane();

		confirmDisableProperty().bind( not( and( tree.isValidProperty(), constraintPane.isValidProperty() ) ) );

		HBox hBox = HBoxBuilder.create().spacing( 30.0 ).children( tree, constraintPane ).build();
		getItems().add( hBox );
	}

	public StatisticWrapper<Number> getSelectedAssertable()
	{
		return  tree.getSelectedAssertion();
	}

	public Constraint<Number> getConstraint()
	{
		return constraintPane.getConstraint();
	}

	public Pair<Integer, Integer> getTolerance()
	{
		return constraintPane.getTolerance();
	}
}
