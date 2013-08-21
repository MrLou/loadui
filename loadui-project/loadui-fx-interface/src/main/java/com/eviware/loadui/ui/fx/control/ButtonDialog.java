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

import static javafx.scene.text.Font.font;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.FontWeight;

import javax.annotation.Nonnull;

public class ButtonDialog extends Dialog
{
	private final Pane itemPane = VBoxBuilder.create().spacing( 6 ).build();
	private final HBox buttonRow = HBoxBuilder.create().padding( new Insets( 12, 0, 0, 0 ) ).spacing( 15 )
			.alignment( Pos.BOTTOM_RIGHT ).build();

	public ButtonDialog( @Nonnull final Node owner, @Nonnull String header )
	{
		super( owner, header );

		Label headerLabel = LabelBuilder.create().font( font( null, FontWeight.BOLD, 14 ) ).text( header ).build();

		super.getItems().setAll( headerLabel, itemPane, buttonRow );
	}

	@Override
	public ObservableList<Node> getItems()
	{
		return itemPane.getChildren();
	}

	public ObservableList<Node> getButtons()
	{
		return buttonRow.getChildren();
	}
}
