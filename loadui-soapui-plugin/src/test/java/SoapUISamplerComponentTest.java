import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;

import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.api.component.categories.GeneratorCategory;
import com.eviware.loadui.api.model.ComponentItem;
import com.eviware.loadui.api.model.ProjectItem;
import com.eviware.loadui.api.statistics.Statistic;
import com.eviware.loadui.api.statistics.StatisticVariable;
import com.eviware.loadui.api.statistics.StatisticVariable.Mutable;
import com.eviware.loadui.api.terminal.InputTerminal;
import com.eviware.loadui.api.terminal.OutputTerminal;
import com.eviware.loadui.api.terminal.TerminalMessage;
import com.eviware.loadui.components.soapui.SoapUISamplerComponent;
import com.eviware.loadui.util.component.ComponentTestUtils;
import com.eviware.loadui.util.test.TestUtils;
import com.google.common.collect.ImmutableMap;

public class SoapUISamplerComponentTest
{
	private SoapUISamplerComponent runner;
	private ComponentItem component;
	private InputTerminal triggerTerminal;
	private OutputTerminal resultsTerminal;

	@Before
	@SuppressWarnings( "unchecked" )
	public void setup()
	{
		ComponentTestUtils.getDefaultBeanInjectorMocker();

		component = ComponentTestUtils.createComponentItem();
		ComponentItem componentSpy = spy( component );
		ComponentContext contextSpy = spy( component.getContext() );
		doReturn( contextSpy ).when( componentSpy ).getContext();
		doReturn( componentSpy ).when( contextSpy ).getComponent();

		final Mutable mockVariable = mock( StatisticVariable.Mutable.class );
		when( mockVariable.getStatisticHolder() ).thenReturn( componentSpy );
		@SuppressWarnings( "rawtypes" )
		final Statistic statisticMock = mock( Statistic.class );
		when( statisticMock.getStatisticVariable() ).thenReturn( mockVariable );
		when( mockVariable.getStatistic( anyString(), anyString() ) ).thenReturn( statisticMock );
		doReturn( mockVariable ).when( contextSpy ).addStatisticVariable( anyString(), anyString(),
				Matchers.<String> anyVararg() );
		doNothing().when( contextSpy ).removeStatisticVariable( anyString() );
		doReturn( mockVariable ).when( contextSpy ).addListenableStatisticVariable( anyString(), anyString(),
				Matchers.<String> anyVararg() );

		ProjectItem projectMock = contextSpy.getCanvas().getProject();
		when( projectMock.getProjectFile() ).thenReturn( new File( "temp.tmp" ) );

		runner = new SoapUISamplerComponent( contextSpy );
		ComponentTestUtils.setComponentBehavior( component, runner );
		contextSpy.setNonBlocking( true );
		component = componentSpy;

		triggerTerminal = runner.getTriggerTerminal();
		resultsTerminal = runner.getResultTerminal();
	}

	@Test
	public void shouldOutput_testCaseProperties() throws InterruptedException, URISyntaxException, ExecutionException,
			TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 1" );

		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );
		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );
		assertThat( message.get( GeneratorCategory.TRIGGER_TIMESTAMP_MESSAGE_PARAM ), is( ( Object )0 ) );
		assertThat( message.size(), is( 8 ) );
		assertThat( message.get( "myProperty" ), is( ( Object )"notChanged" ) );
	}

	/*
	 * Tests for SOAPUI-3947.
	 */
	@Test
	public void shouldHandle_tenConcurrentRequests() throws URISyntaxException, InterruptedException,
			ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 2" );

		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();
		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );
		assertThat( message, is( nullValue() ) );

		sendSimpleTrigger();

		message = results.poll( 5, TimeUnit.SECONDS );
		assertThat( message, is( notNullValue() ) );
	}

	/*
	 * Tests for SOAPUI-3884.
	 */
	@Test
	public void session_shouldNot_beMaintained() throws URISyntaxException, InterruptedException, ExecutionException,
			TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 5" );

		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );
		sendSimpleTrigger();
		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "Status" ), is( ( Object )Boolean.TRUE ) );
	}

	@Test
	public void assertionFailures_shouldFail_theSample() throws URISyntaxException, InterruptedException,
			ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 3" );

		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );
		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "Status" ), is( ( Object )Boolean.FALSE ) );
	}

	@Test
	public void disablingAssertions_shouldNotFail_theSample() throws URISyntaxException, InterruptedException,
			ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 3" );
		runner.setDisableSoapUIAssertions( true );
		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );
		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "Status" ), is( ( Object )Boolean.TRUE ) );
	}

	@Test
	public void fieldsInIncomingMessages_shouldOverride_testCaseProperties() throws InterruptedException,
			URISyntaxException, ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 4" );
		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );

		ComponentTestUtils.sendMessage( triggerTerminal, ImmutableMap.<String, Object> of( "hasBeenOverridden", "true" ) );

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "hasBeenOverridden" ), is( ( Object )"true" ) );
	}

	/*
	 * Tests for SOAPUI-3830.
	 */
	@Test
	public void ampersands_should_beEncoded_inRestRequests() throws InterruptedException, URISyntaxException,
			ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 6" );
		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );

		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "Status" ), is( ( Object )Boolean.TRUE ) );
	}

	@Test
	public void disablingTestSteps_should_work() throws InterruptedException, URISyntaxException, ExecutionException,
			TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 4" );
		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );

		sendSimpleTrigger();

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "changedLastBy" ), is( ( Object )"Step1" ) );

		runner.setTestStepIsDisabled( 0, true );
		TestUtils.awaitEvents( component );

		sendSimpleTrigger();

		message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "changedLastBy" ), is( ( Object )"never" ) );

	}

	@Test
	public void propertiesInSettings_shouldOverride_testCaseProperties() throws InterruptedException,
			URISyntaxException, ExecutionException, TimeoutException
	{
		setTestCase( "soapUI-loadUI-plugin-project.xml", "TestSuite 1", "TestCase 4" );
		BlockingQueue<TerminalMessage> results = ComponentTestUtils.getMessagesFrom( resultsTerminal );

		ComponentTestUtils.sendMessage( triggerTerminal, ImmutableMap.<String, Object> of( "hasBeenOverridden", "true" ) );

		TerminalMessage message = results.poll( 5, TimeUnit.SECONDS );

		assertThat( message.get( "hasBeenOverridden" ), is( ( Object )"true" ) );
	}

	private void sendSimpleTrigger()
	{
		ComponentTestUtils.sendMessage( triggerTerminal,
				ImmutableMap.<String, Object> of( GeneratorCategory.TRIGGER_TIMESTAMP_MESSAGE_PARAM, 0 ) );
	}

	private void setProject( String fileName ) throws URISyntaxException
	{
		File project = new File( getClass().getResource( "/" + fileName ).toURI() );
		assertThat( project.exists(), is( true ) );
		runner.setProject( project );
	}

	private void setTestCase( String projectFile, String testSuite, String testCase ) throws URISyntaxException,
			InterruptedException, ExecutionException, TimeoutException
	{
		setProject( projectFile );
		TestUtils.awaitEvents( component );
		runner.setTestSuite( testSuite );
		TestUtils.awaitEvents( component );
		runner.setTestCase( testCase );
		TestUtils.awaitEvents( component );
	}
}
