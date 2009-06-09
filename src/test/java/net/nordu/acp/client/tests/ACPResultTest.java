/**
 * 
 */
package net.nordu.acp.client.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import net.nordu.acp.client.ACPResult;

import org.junit.Before;
import org.junit.Test;

/**
 * @author leifj
 *
 */
public class ACPResultTest {

	private ACPResult r1,r2,r3;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		r1 = ACPResult.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("acp-response-1.xml"));
		r2 = ACPResult.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("acp-response-2.xml"));
		r3 = ACPResult.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("acp-response-3.xml"));
	}

	@Test
	public void testReadFile() {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("empty.xml");
			assertNotNull(in);
			LineNumberReader r = new LineNumberReader(new InputStreamReader(in)); 
			String line;
			while ( (line = r.readLine()) != null) {
				assertNotNull(line);
				System.err.println(line);
			}
		} catch (Exception t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	
	/**
	 * Test method for {@link net.nordu.acp.client.ACPResult#parse(java.io.InputStream)}.
	 */
	@Test
	public void testParse() {
		try {
			ACPResult r = ACPResult.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("acp-response-1.xml"));
			assertNotNull(r);
			assertNotNull(r.getDocument());
			System.err.println(r);
		} catch (Exception t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	/**
	 * Test method for {@link net.nordu.acp.client.ACPResult#isError()}.
	 */
	@Test
	public void testIsError() {
		assert(!r1.isError());
		assert(r2.isError());
	}

	/**
	 * Test method for {@link net.nordu.acp.client.ACPResult#getError()}.
	 */
	@Test
	public void testGetError() {
		try {
			assertNotNull(r2.getError());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

}
