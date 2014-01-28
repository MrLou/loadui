package com.eviware.loadui.launcher.server;

import com.eviware.loadui.launcher.HeadlessFxLauncherBase;
import com.eviware.loadui.launcher.api.GroovyCommand;
import com.eviware.loadui.launcher.impl.FileGroovyCommand;
import com.eviware.loadui.launcher.impl.ResourceGroovyCommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.*;

/**
 * @author renato
 */
public class LoadUIServerLauncher extends HeadlessFxLauncherBase
{
	protected static final String LOCAL_OPTION = "l";
	protected static final String FILE_OPTION = "f";
	protected static final String AGENT_OPTION = "a";
	protected static final String LIMITS_OPTION = "L";
	@Deprecated
	protected static final String TESTCASE_OPTION = "t";
	protected static final String VU_SCENARIO_OPTION = "v";
	protected static final String PROJECT_OPTION = "p";
	protected static final String WORKSPACE_OPTION = "w";
	protected static final String REPORT_DIR_OPTION = "r";
	protected static final String RETAIN_SAVED_ZOOM_LEVELS = "z";
	protected static final String REPORT_FORMAT_OPTION = "F";
	protected static final String STATISTICS_REPORT_OPTION = "S";
	protected static final String STATISTICS_REPORT_INCLUDE_SUMMARY_OPTION = "s";
	protected static final String STATISTICS_REPORT_COMPARE_OPTION = "c";
	protected static final String ABORT_ONGOING_REQUESTS_OPTION = "A";


	public LoadUIServerLauncher( String[] args )
	{
		super( args );
	}

	@Override
	protected GroovyCommand createCommand( CommandLine cmd )
	{
		Map<String, Object> attributes = new HashMap<>();

		if( cmd.hasOption( PROJECT_OPTION ) )
		{
			attributes.put( "workspaceFile",
					cmd.hasOption( WORKSPACE_OPTION ) ? new File( cmd.getOptionValue( WORKSPACE_OPTION ) ) : null );
			attributes.put( "projectFile",
					cmd.hasOption( PROJECT_OPTION ) ? new File( cmd.getOptionValue( PROJECT_OPTION ) ) : null );
			attributes.put( "testCase", cmd.getOptionValue( TESTCASE_OPTION ) );
			if( cmd.getOptionValue( VU_SCENARIO_OPTION ) != null )
				attributes.put( "testCase", cmd.getOptionValue( VU_SCENARIO_OPTION ) );
			attributes.put( "limits", cmd.hasOption( LIMITS_OPTION ) ? cmd.getOptionValue( LIMITS_OPTION ).split( ":" )
					: null );
			attributes.put( "localMode", cmd.hasOption( LOCAL_OPTION ) );
			Map<String, String[]> agents = null;
			if( cmd.hasOption( AGENT_OPTION ) )
			{
				agents = new HashMap<>();
				for( String option : cmd.getOptionValues( AGENT_OPTION ) )
				{
					int ix = option.indexOf( "=" );
					if( ix != -1 )
						agents.put( option.substring( 0, ix ), option.substring( ix + 1 ).split( "," ) );
					else
						agents.put( option, null );
				}
			}
			attributes.put( "agents", agents );

			attributes.put( "reportFolder", cmd.getOptionValue( REPORT_DIR_OPTION ) );
			attributes.put( "reportFormat",
					cmd.hasOption( REPORT_FORMAT_OPTION ) ? cmd.getOptionValue( REPORT_FORMAT_OPTION ) : "PDF" );

			String[] statisticPageOptionValues = cmd.getOptionValues( STATISTICS_REPORT_OPTION );
			List<String> statisticPages = statisticPageOptionValues == null ? Collections.<String>emptyList() : Arrays
					.asList( statisticPageOptionValues );

			attributes.put( "statisticPages", statisticPages );
			attributes.put( "compare", cmd.getOptionValue( STATISTICS_REPORT_COMPARE_OPTION ) );

			attributes.put( "abort", cmd.getOptionValue( ABORT_ONGOING_REQUESTS_OPTION ) );

			attributes.put( "includeSummary", cmd.hasOption( STATISTICS_REPORT_INCLUDE_SUMMARY_OPTION ) );

			attributes.put( "retainZoom", cmd.hasOption( RETAIN_SAVED_ZOOM_LEVELS ) );

			return new ResourceGroovyCommand( "/RunTest.groovy", attributes );
		}
		else if( cmd.hasOption( FILE_OPTION ) )
		{
			return new FileGroovyCommand( new File( cmd.getOptionValue( FILE_OPTION ) ), attributes );
		}
		else
		{
			return null;
		}
	}

	@Override
	@SuppressWarnings("static-access")
	protected Options createOptions()
	{
		Options options = super.createOptions();
		options.addOption( WORKSPACE_OPTION, "workspace", true, "Sets the Workspace file to load" );
		options.addOption( PROJECT_OPTION, "project", true, "Sets the Project file to run" );
		options.addOption( TESTCASE_OPTION, "testcase", true,
				"Sets which TestCase to run (leave blank to run the entire Project)" );
		options.addOption( VU_SCENARIO_OPTION, "scenario", true,
				"Sets which Scenario to run (leave blank to run the entire Project)" );
		options.addOption( LIMITS_OPTION, "limits", true,
				"Sets the limits (<SECONDS>:<REQUESTS>:<FAILURES>) for the execution (e.g. -L 60:0:200 )" );
		options.addOption( OptionBuilder
				.withLongOpt( "agents" )
				.withDescription(
						"Sets the agents to use for the test ( usage -" + AGENT_OPTION
								+ " <ip>[:<port>][=<scenario>[,<scenario>] ...] )" ).hasArgs().create( AGENT_OPTION ) );
		options.addOption( FILE_OPTION, "file", true, "Executes the specified Groovy script file" );
		options.addOption( LOCAL_OPTION, "local", false, "Executes TestCases in local mode" );
		options.addOption( REPORT_DIR_OPTION, "reports", true, "Generates reports and saves them in specified folder" );
		options
				.addOption( REPORT_FORMAT_OPTION, "format", true,
						"Specify output format for the exported reports (supported formats are: PDF, XLS, HTML, RTF, CSV, TXT and XML)" );
		options
				.addOption( OptionBuilder
						.withLongOpt( "statistics" )
						.withDescription(
								"Sets which Statistics pages to add to the generated report (leave blank save all pages)" )
						.hasOptionalArgs().create( STATISTICS_REPORT_OPTION ) );
		options.addOption( STATISTICS_REPORT_INCLUDE_SUMMARY_OPTION, "summary", false,
				"Set to include summary report in statistics report" );
		options.addOption( STATISTICS_REPORT_COMPARE_OPTION, "compare", true,
				"Specify a saved execution to use as a base for comparison in the generated statistics report" );
		options
				.addOption(
						ABORT_ONGOING_REQUESTS_OPTION,
						"abort",
						true,
						"Overrides \"Abort ongoing requests on finish\" project property. If set to true ongoing requests will be canceled, if false test will finish when all ongoing requests complete. If not set, property value from project will be used to determine what to do with ongoing requests." );

		options.addOption( RETAIN_SAVED_ZOOM_LEVELS, false, "Use the saved zoom levels for charts from the project." );

		return options;
	}

}
