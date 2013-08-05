/*
 * Copyright 2011 SmartBear Software
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.test.ui.fx.chart;

import com.eviware.loadui.test.TestState;
import com.eviware.loadui.test.categories.IntegrationTest;
import com.eviware.loadui.test.ui.fx.FxIntegrationTestBase;
import com.eviware.loadui.test.ui.fx.states.SimpleWebTestState;
import com.google.common.base.Predicate;
import javafx.scene.Node;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.internal.matchers.TypeSafeMatcher;

import static com.eviware.loadui.test.ui.fx.chart.ChartTestSupport.allChartLines;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author henrik.olsson
 */
@Category(IntegrationTest.class)
public class ChartsTest extends FxIntegrationTestBase
{
	private static final TypeSafeMatcher<Node> WEB_RUNNER = new TypeSafeMatcher<Node>()
	{
		@Override
		public boolean matchesSafely( Node node )
		{
			if( node.getClass().getSimpleName().equals( "StatisticHolderToolboxItem" ) )
			{
				return node.toString().equals( "Web Page Runner 1" );
			}
			return false;
		}

		@Override
		public void describeTo( Description description )
		{
			//To change body of implemented methods use File | Settings | File Templates.
		}
	};

	@Test
	public void shouldHaveTwoLines()
	{
		runTestFor( 5, SECONDS );

		click( "#statsTab" );
		drag( WEB_RUNNER ).by( 150, 150 ).drop().sleep( 1000 );
		click( "#default" );

		assertThat( allChartLines().size(), is( 2 ) );
	}

	@Override
	public TestState getStartingState()
	{
		return SimpleWebTestState.STATE;
	}

}
