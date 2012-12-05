package com.eviware.loadui.ui.fx.views.assertions;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.Scene;

import com.eviware.loadui.api.statistics.StatisticsManager;
import com.eviware.loadui.ui.fx.api.Inspector;
import com.eviware.loadui.ui.fx.api.perspective.PerspectiveEvent;
import com.eviware.loadui.ui.fx.util.InspectorHelpers;

public class AssertionInspector implements Inspector
{
	private static final String FILTER = PerspectiveEvent.getPath( PerspectiveEvent.PERSPECTIVE_PROJECT ) + ".*";

	private final AssertionInspectorView panel;

	public AssertionInspector( StatisticsManager statisticsManager )
	{
		panel = new AssertionInspectorView();
	}

	@Override
	public void initialize( ReadOnlyProperty<Scene> sceneProperty )
	{
		panel.projectProperty().bind( InspectorHelpers.projectProperty( sceneProperty ) );
	}

	@Override
	public String getName()
	{
		return "Assertions";
	}

	@Override
	public Node getPanel()
	{
		return panel;
	}

	@Override
	public void onShow()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onHide()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String getHelpUrl()
	{
		return null;
	}

	@Override
	public String getPerspectiveRegex()
	{
		return FILTER;
	}
}