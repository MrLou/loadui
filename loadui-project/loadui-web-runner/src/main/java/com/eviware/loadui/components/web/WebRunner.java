package com.eviware.loadui.components.web;

import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.api.serialization.ListenableValue;
import com.eviware.loadui.api.terminal.TerminalMessage;
import com.eviware.loadui.api.testevents.MessageLevel;
import com.eviware.loadui.api.testevents.TestEventManager;
import com.eviware.loadui.components.web.api.RequestRunnerProvider;
import com.eviware.loadui.impl.component.categories.RunnerBase;
import com.eviware.loadui.util.html.HtmlAssetScraper;
import com.eviware.loadui.util.property.UrlProperty;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebRunner extends RunnerBase implements ListenableValue.ValueListener<String>
{

	private final UrlProperty webPageUrlProperty;
	private final HtmlAssetScraper scraper;
	private final RequestRunnerProvider requestRunnerProvider;
	private RequestRunner requestRunner;
	private TestEventManager testEventManager;
	private Runnable toRunOnRelease;
	private AtomicBoolean isLoadTestRunning = new AtomicBoolean( false );

	// TODO remove this
	public static final String WEB_PAGE_URL_PROP = UrlProperty.URL;

	public WebRunner( ComponentContext context, HtmlAssetScraper scraper, RequestRunnerProvider requestRunnerProvider )
	{
		super( context );
		this.scraper = scraper;
		this.requestRunnerProvider = requestRunnerProvider;
		this.webPageUrlProperty = new UrlProperty( context );
		context.setLayout( new WebRunnerLayout( webPageUrlProperty, context ) );
		webPageUrlProperty.addUrlChangeListener( this );
	}

	public void setLoadTestRunning( boolean running )
	{
		isLoadTestRunning.set( running );
		if( running )
		{
			if( requestRunner != null )
			{
				requestRunner.resetCounters();
			}
			updateWebPageUrl( webPageUrlProperty.getUrl() );
		}
	}

	@Override
	public void update( String url )
	{
		if( isLoadTestRunning.get() )
		{
			updateWebPageUrl( url );
		}
	}

	private void updateWebPageUrl( String url )
	{
		log.debug( "Updating url to {}", url );
		try
		{
			URI pageUri = validateUrl( url );
			requestRunner = requestRunnerProvider.provideRequestRunner( getContext(), pageUri, scraper.scrapeUrl( url ) );
		}
		catch( IllegalArgumentException e )
		{
			log.debug( "WebRunner cannot accept the invalid URL: {}", url );
		}
		catch( IOException e )
		{
			log.debug( "An error occurred while scraping the provided URL: {}", url );
		}
	}

	private void notifyUser( String message )
	{
		if( testEventManager != null )
		{
			testEventManager.logMessage( MessageLevel.WARNING, message );
		}
	}

	private URI validateUrl( String url ) throws IllegalArgumentException
	{
		if( url == null )
		{
			throw new IllegalArgumentException( "URL cannot be null" );
		}
		return URI.create( url );
	}

	@Override
	protected TerminalMessage sample( TerminalMessage triggerMessage, Object sampleId ) throws SampleCancelledException
	{
		if( requestRunner == null )
			throw new RuntimeException( "Cannot run, no URL set or URL is invalid" );
		requestRunner.run();
		return triggerMessage;
	}

	@Override
	protected int onCancel()
	{
		//TODO implement
		return 0;
	}

	@Override
	public void onRelease()
	{
		super.onRelease();
		if( toRunOnRelease != null )
		{
			toRunOnRelease.run();
		}
	}

	public void setTestEventManager( TestEventManager testEventManager )
	{
		this.testEventManager = testEventManager;
	}


	public void setOnRelease( Runnable onRelease )
	{
		this.toRunOnRelease = onRelease;
	}

}
