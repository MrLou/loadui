package com.eviware.loadui.impl.statistics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.eviware.loadui.api.statistics.StatisticVariable;
import com.eviware.loadui.api.statistics.StatisticsManager;
import com.eviware.loadui.api.statistics.StatisticsWriter;
import com.eviware.loadui.api.statistics.StatisticsWriterFactory;
import com.eviware.loadui.api.statistics.store.Entry;

/**
 * A StatisticsWriter used to calculate a raw value, where each update signifies
 * a change in the value, and the time between updates is thus important in the
 * calculation.
 * 
 * @author dain.nilsson
 */
public class VariableStatisticsWriter extends AbstractStatisticsWriter
{
	public static final String TYPE = "VARIABLE";

	public static enum Stats
	{
		VALUE
	}

	private double sum = 0;
	private double lastValue = 0;
	private long lastUpdate = System.currentTimeMillis();

	public VariableStatisticsWriter( StatisticsManager manager, StatisticVariable variable,
			Map<String, Class<? extends Number>> values, Map<String, Object> config )
	{
		super( manager, variable, values, config );
	}

	@Override
	public void update( long timestamp, Number value )
	{
		synchronized( this )
		{
			while( lastTimeFlushed + delay < timestamp )
				flush();
			long delta = timestamp - lastUpdate;
			lastUpdate = timestamp;
			sum += lastValue * delta;
			lastValue = value.doubleValue();
		}
	}

	@Override
	public Entry output()
	{
		double value = sum / delay;
		lastTimeFlushed += delay;
		sum = 0;
		if( lastUpdate < lastTimeFlushed )
			lastUpdate = lastTimeFlushed;

		return at( lastTimeFlushed ).put( Stats.VALUE.name(), value ).build();
	}

	@Override
	public Entry aggregate( Set<Entry> entries )
	{
		if( entries.size() <= 1 )
			return entries.size() == 0 ? null : entries.iterator().next();

		long maxTime = -1;
		double total = 0;
		int count = 0;
		for( Entry entry : entries )
		{
			count++ ;
			maxTime = Math.max( maxTime, entry.getTimestamp() );
			total += entry.getValue( Stats.VALUE.name() ).doubleValue();
		}

		return at( maxTime ).put( Stats.VALUE.name(), total / count ).build( false );
	}

	@Override
	protected void reset()
	{
		super.reset();

		lastTimeFlushed = System.currentTimeMillis();
		lastUpdate = lastTimeFlushed;
		lastValue = 0;
		sum = 0;
	}

	@Override
	public String getType()
	{
		return TYPE;
	}

	public static class Factory implements StatisticsWriterFactory
	{
		@Override
		public String getType()
		{
			return TYPE;
		}

		@Override
		public StatisticsWriter createStatisticsWriter( StatisticsManager statisticsManager, StatisticVariable variable,
				Map<String, Object> config )
		{
			return new VariableStatisticsWriter( statisticsManager, variable,
					Collections.<String, Class<? extends Number>> singletonMap( Stats.VALUE.name(), Double.class ), config );
		}
	}
}
