package com.eviware.loadui.impl.component.categories;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.api.terminal.Connection;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.impl.model.ComponentItemImpl;
import com.eviware.loadui.util.component.ComponentTestUtils;
import com.google.common.collect.ImmutableMap;

public class FlowBaseTest
{
	private FlowBase flowBase;
	private ComponentItemImpl component;

	@Before
	public void setup()
	{
		ComponentTestUtils.getDefaultBeanInjectorMocker();
		component = ComponentTestUtils.createComponentItem();
		flowBase = new FlowBase( component.getContext() )
		{
		};
		component.setBehavior( flowBase );
		component.getContext().setNonBlocking( true );
	}

	@Test
	public void shouldDelegateLikeFunction()
	{
		OutputTerminal dummyOut = mock( OutputTerminal.class );
		InputTerminal dummyIn = mock( InputTerminal.class );
		when( dummyIn.likes( dummyOut ) ).thenReturn( true );

		InputTerminal input = flowBase.getIncomingTerminal();
		OutputTerminal output1 = flowBase.createOutgoing();

		assertThat( input.likes( dummyOut ), is( false ) );

		Connection connection1 = output1.connectTo( dummyIn );
		assertThat( input.likes( dummyOut ), is( true ) );

		OutputTerminal output2 = flowBase.createOutgoing();
		output2.connectTo( dummyIn );
		assertThat( input.likes( dummyOut ), is( true ) );

		connection1.disconnect();
		assertThat( input.likes( dummyOut ), is( true ) );

		flowBase.deleteOutgoing();
		assertThat( input.likes( dummyOut ), is( false ) );
	}

	@Test
	public void shouldPropagateSignature() throws InterruptedException, ExecutionException, TimeoutException
	{
		Map<String, Class<? extends Object>> signature = ImmutableMap.of( "A", Object.class, "B", String.class );

		ComponentContext context = ComponentTestUtils.createComponentItem().getContext();
		OutputTerminal otherOutput = context.createOutput( "output" );
		context.setSignature( otherOutput, signature );

		InputTerminal input = flowBase.getIncomingTerminal();
		OutputTerminal output1 = flowBase.createOutgoing();

		Connection connection = otherOutput.connectTo( input );

		assertThat( output1.getMessageSignature(), equalTo( signature ) );

		OutputTerminal output2 = flowBase.createOutgoing();

		assertThat( output2.getMessageSignature(), equalTo( signature ) );

		connection.disconnect();
		assertThat( output1.getMessageSignature().isEmpty(), is( true ) );
		assertThat( output2.getMessageSignature().isEmpty(), is( true ) );
	}
}
