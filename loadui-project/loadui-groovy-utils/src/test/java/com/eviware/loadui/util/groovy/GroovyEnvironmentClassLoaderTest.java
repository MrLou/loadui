package com.eviware.loadui.util.groovy;

import com.eviware.loadui.test.categories.IntegrationTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category( IntegrationTest.class )
@Ignore( value = "Although useful, this test is proving hard to implement right. We need to fix this ASAP. LOADUI-826")
public class GroovyEnvironmentClassLoaderTest
{

	private static final String CLASS_NOT_IN_CLASS_PATH = "org.fit.cssbox.css.CSSUnits";
	private static final String[] DEPENDENCY = { "cssbox", "cssbox", "3.4" };

	private static GroovyEnvironmentClassLoader cl;

	@BeforeClass
	public static void setup()
	{
		cl = new GroovyEnvironmentClassLoader( GroovyEnvironmentClassLoaderTest.class.getClassLoader(),
				new File( "target", ".groovy" ) );
	}

	@AfterClass
	public static void cleanup() throws IOException
	{
		try
		{
			cl.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		} finally
		{
			cl = null;
		}
	}

	@Test( expected = ClassNotFoundException.class )
	public void ensureClassUsedForTestsIsNotInClassPath() throws ClassNotFoundException
	{
		getClass().getClassLoader().loadClass( CLASS_NOT_IN_CLASS_PATH );
	}

	@Test
	public void ensureNormalDownloadOfDependencyWorks() throws Exception
	{
		cl.loadDependency( DEPENDENCY[0], DEPENDENCY[1], DEPENDENCY[2] );
		Class<?> cls = cl.loadClass( CLASS_NOT_IN_CLASS_PATH );
		assertEquals( CLASS_NOT_IN_CLASS_PATH, cls.getName() );
	}

	@Test
	public void testLoadJarFile() throws Exception
	{

		// we must assume normal dependency loading worked to test this
		ensureNormalDownloadOfDependencyWorks();

		boolean dependencyAdded = cl.loadJarFile( DEPENDENCY[0], DEPENDENCY[1], DEPENDENCY[2] );

		assertTrue( dependencyAdded );

	}

}
