package com.eviware.loadui.components.web;

import com.eviware.loadui.api.base.Clock;
import com.eviware.loadui.api.component.ComponentContext;
import com.eviware.loadui.components.web.api.RequestRunnerProvider;
import com.eviware.loadui.util.test.FakeClock;
import com.eviware.loadui.util.test.FakeHttpClient;

import java.net.URI;

public class FakeRequestRunnerProvider implements RequestRunnerProvider
{
	public RequestRunner provideRequestRunner( ComponentContext context, URI pageUri, Iterable<URI> pageUris )
	{
		Clock clock = new FakeClock();
		return new RequestRunner( clock,
				new FakeHttpClient(),
				pageUris,
				new WebRunnerStatsSender( context, clock ) );
	}
}
