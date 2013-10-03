package com.eviware.loadui.impl.reporting;

import com.eviware.loadui.LoadUI;
import com.eviware.loadui.api.reporting.ReportingManager;
import com.eviware.loadui.api.reporting.SummaryExporter;
import com.eviware.loadui.api.summary.Chapter;
import com.eviware.loadui.api.summary.MutableSummary;
import com.eviware.loadui.api.summary.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * @author renato
 */
public class JasperSummaryExporter implements SummaryExporter
{

	public static final Logger log = LoggerFactory.getLogger( SummaryExporter.class );

	private final ReportingManager reportingManager;

	public JasperSummaryExporter( ReportingManager reportingManager )
	{
		this.reportingManager = reportingManager;
	}

	public void saveSummary( MutableSummary summary, String reportFolder, String reportFormat, String label )
	{
		File outputDir;
		if( reportFolder == null || reportFolder.isEmpty() )
			outputDir = new File( System.getProperty( LoadUI.LOADUI_HOME ) );
		else
			outputDir = new File( reportFolder );

		if( reportFormat == null || reportFormat.equalsIgnoreCase( "xml" ) )
		{
			saveSummaryAsXML( summary, createOutputFile( outputDir, "xml", label ) );
		}
		else
		{
			reportFormat = reportFormat.toUpperCase();
			boolean formatSupported = false;
			for( ReportEngine.ReportFormats rf : ReportEngine.ReportFormats.values() )
			{
				if( rf.toString().equals( reportFormat ) )
				{
					formatSupported = true;
					File out = createOutputFile( outputDir, reportFormat, label );
					reportingManager.createReport( summary, out, reportFormat );
					break;
				}
			}
			if( !formatSupported )
			{
				log.warn( "Format '" + reportFormat + "' is not supported. Report will be saved in plain xml." );
				saveSummaryAsXML( summary, createOutputFile( outputDir, "xml", label ) );
			}
		}
	}

	private File createOutputFile( File outputDir, String format, String label )
	{
		String fileName = label + "-summary-" + System.currentTimeMillis() + "." + format.toLowerCase();
		return new File( outputDir, fileName );
	}

	@SuppressWarnings("rawtypes")
	private void saveSummaryAsXML( final MutableSummary summary, final File out )
	{
		SwingWorker worker = new XmlExporter( summary, out );
		worker.execute();
	}

	private static final class XmlExporter extends SwingWorker<Void, Void>
	{
		private final MutableSummary summary;
		private final File out;

		private XmlExporter( MutableSummary summary, File out )
		{
			this.summary = summary;
			this.out = out;
		}

		@Override
		protected Void doInBackground() throws Exception
		{
			try
			{
				XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
				// Create an XML stream writer
				XMLStreamWriter xmlw = xmlof.createXMLStreamWriter( new BufferedWriter( new FileWriter( out ) ) );
				xmlw.writeStartDocument();
				xmlw.writeCharacters( "\n" );
				xmlw.writeStartElement( "summary" );
				xmlw.writeCharacters( "\n" );
				for( String chapKey : summary.getChapters().keySet() )
				{
					Chapter chapter = summary.getChapters().get( chapKey );
					xmlw.writeCharacters( "\t" );
					xmlw.writeStartElement( "chapter" );
					xmlw.writeAttribute( "title", chapter.getTitle() );
					xmlw.writeAttribute( "date", chapter.getDate().toString() );
					xmlw.writeCharacters( "\n" );
					xmlw.writeCharacters( "\t\t" );
					xmlw.writeStartElement( "description" );
					xmlw.writeCharacters( chapter.getDescription() );
					xmlw.writeEndElement();
					xmlw.writeCharacters( "\n" );
					for( String valKey : chapter.getValues().keySet() )
					{
						xmlw.writeCharacters( "\t\t" );
						xmlw.writeStartElement( valKey.replace( " ", "_" ).replace( "(%)", "" ).toLowerCase() );
						xmlw.writeCharacters( chapter.getValues().get( valKey ) );
						xmlw.writeCharacters( "\t\t" );
						xmlw.writeEndElement();
						xmlw.writeCharacters( "\n" );
						/*
						 * xmlw.writeCharacters( "\n" ); xmlw.writeStartElement(
						 * "value-value" ); xmlw.writeCharacters(
						 * chapter.getValues().get( valKey ) );
						 * xmlw.writeEndElement(); // value-value
						 * xmlw.writeCharacters( "\n" ); xmlw.writeEndElement(); //
						 * value
						 */
					}
					for( Section section : chapter.getSections() )
					{
						xmlw.writeCharacters( "\t\t" );
						xmlw.writeStartElement( "section" );
						xmlw.writeAttribute( "title", section.getTitle() );
						xmlw.writeCharacters( "\n" );
						for( String valKey : section.getValues().keySet() )
						{
							xmlw.writeCharacters( "\t\t\t" );
							xmlw.writeStartElement( valKey.replace( " ", "_" ).replace( "(%)", "" ).toLowerCase() );
							xmlw.writeCharacters( section.getValues().get( valKey ) );
							xmlw.writeEndElement(); // value-name
							xmlw.writeCharacters( "\n" );
						}
						for( String tablekey : section.getTables().keySet() )
						{
							xmlw.writeCharacters( "\t\t\t" );
							xmlw.writeStartElement( tablekey.replace( " ", "_" ).toLowerCase() );
							// xmlw.writeAttribute( "name", tablekey );
							TableModel table = section.getTables().get( tablekey );
							xmlw.writeCharacters( "\n" );
							// xmlw.writeStartElement( "columns" );
							// xmlw.writeAttribute( "size", String.valueOf(
							// table.getColumnCount() ) );
							// xmlw.writeCharacters( "\n" );
							// StringBuffer columns = new StringBuffer();
							// String columns = new String [table.getColumnCount()]
							// for( int i = 0; i < table.getColumnCount(); i++ ) {
							// columns.append( table.getColumnName( i ) ).append(
							// "," );
							// columns.deleteCharAt( columns.length() - 1 );
							// xmlw.writeCharacters( columns.toString() );
							// xmlw.writeEndElement();// columns
							// xmlw.writeCharacters( "\n" );
							//
							// xmlw.writeStartElement( "rows" );
							// xmlw.writeAttribute( "size", String.valueOf(
							// table.getRowCount() ) );
							// xmlw.writeCharacters( "\n" );
							for( int j = 0; j < table.getRowCount(); j++ )
							{
								xmlw.writeCharacters( "\t\t\t" );
								xmlw.writeStartElement( "row" );
								xmlw.writeCharacters( "\n" );
								for( int i = 0; i < table.getColumnCount(); i++ )
								{
									xmlw.writeCharacters( "\t\t\t\t" );
									xmlw.writeStartElement( table.getColumnName( i ).replace( " ", "_" ).replace( "/", "_" )
											.toLowerCase() );
									if( table.getValueAt( j, i ) != null )
										xmlw.writeCharacters( table.getValueAt( j, i ).toString() );
									xmlw.writeEndElement();
									xmlw.writeCharacters( "\n" );
								}
								xmlw.writeCharacters( "\t\t\t" );
								xmlw.writeEndElement();// row
								xmlw.writeCharacters( "\n" );
							}
							xmlw.writeCharacters( "\t\t\t" );
							xmlw.writeEndElement(); // table
							xmlw.writeCharacters( "\n" );
						}
						xmlw.writeCharacters( "\t\t" );
						xmlw.writeEndElement();// section
						xmlw.writeCharacters( "\n" );
					}
					xmlw.writeCharacters( "\t" );
					xmlw.writeEndElement(); // chapter
					xmlw.writeCharacters( "\n" );
				}
				xmlw.writeEndElement(); //
				xmlw.writeCharacters( "\n" );
				xmlw.writeEndDocument();
				xmlw.flush();
				xmlw.close();
			}
			catch( Exception e )
			{
				log.error( "Failed to write XML report", e );
			}
			return null;
		}
	}

}
