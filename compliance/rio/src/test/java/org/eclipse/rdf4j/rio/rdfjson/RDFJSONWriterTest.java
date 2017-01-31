/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.rdfjson.RDFJSONParserFactory;
import org.eclipse.rdf4j.rio.rdfjson.RDFJSONWriterFactory;

/**
 * JUnit test for the RDF/JSON parser.
 * 
 * @author Peter Ansell
 */
public class RDFJSONWriterTest extends RDFWriterTest {

	public RDFJSONWriterTest() {
		super(new RDFJSONWriterFactory(), new RDFJSONParserFactory());
	}
	
	@Override
	protected Model parse(InputStream reader, String baseURI)
		throws RDFParseException, RDFHandlerException, IOException
	{
		return QueryResults.asModel(
				QueryResults.parseGraphBackground(reader, baseURI, rdfParserFactory.getRDFFormat()));
	}
	
}
