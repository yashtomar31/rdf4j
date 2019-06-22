/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public abstract class MultithreadedTest {
	SimpleValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testDataAndShapes() {

		Properties.setLockTrackingEnabled(true);

		List<List<Transaction>> list = new ArrayList<>();

		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();
				String join = String.join("\n", "",
					"ex:data_" + i + "_" + j,
					"  ex:age " + i + j + 100,
					"  ."

				);
				transaction.add(join, null);


				transactions.add(transaction);
			}
			list.add(transactions);
		}


		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();

				String join = String.join("\n", "",
					"ex:shape_" + i + "_" + j,
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person" + j + " ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype sh:integer ;",
					"                sh:minCount 1 ;",
					"                sh:maxCount 1 ;",
					"        ] .");

				transaction.add(join, RDF4J.SHACL_SHAPE_GRAPH);
				transactions.add(transaction);
			}
			list.add(transactions);
		}

		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();
				String join;
				if (i % 2 == 0) {
					join = String.join("\n", "",
						"ex:data_" + i + "_" + j + " a ex:Person" + j + "; ",
						"  ex:age" + i + " " + i + j,
						"  ."

					);
				} else {
					join = String.join("\n", "",
						"ex:data_" + i + "_" + j + " a ex:Person" + j + "; ",
						"  ."

					);
				}

				transaction.add(join, null);
				transactions.add(transaction);
			}
			list.add(transactions);
		}

		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();
				if (i % 2 == 0) {
					String join = String.join("\n", "",
						"ex:data_" + i + "_" + j ,
						"  ex:age" + i + " " + i + j+100,
						"  ."

					);
					transaction.add(join, null);
				}


				transactions.add(transaction);
			}
			list.add(transactions);
		}


		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();

				String join = String.join("\n", "",
					"ex:shape_" + i + "_" + j,
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person" + j + " ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype sh:integer ;",
					"                sh:minCount 1 ;",
					"                sh:maxCount 1 ;",
					"        ] .");

				transaction.remove(join, RDF4J.SHACL_SHAPE_GRAPH);
				transactions.add(transaction);
			}
			list.add(transactions);
		}

		for (int j = 0; j < 10; j++) {
			ArrayList<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Transaction transaction = new Transaction();
				String join;
				if (i % 2 == 0) {
					join = String.join("\n", "",
						"ex:data_" + i + "_" + j + " a ex:Person" + j + "; ",
						"  ex:age" + i + " " + i + j,
						"  ."

					);
				} else {
					join = String.join("\n", "",
						"ex:data_" + i + "_" + j + " a ex:Person" + j + "; ",
						"  ."

					);
				}

				transaction.remove(join, null);
				transactions.add(transaction);
			}
			list.add(transactions);
		}

		parallelTest(list, IsolationLevels.SNAPSHOT);

	}

	private void parallelTest(List<List<Transaction>> list, IsolationLevels isolationLevel) {
		ShaclSail sail = new ShaclSail(getBaseSail());
		sail.setParallelValidation(true);
		sail.setLogValidationPlans(false);
		sail.setGlobalLogValidationExecution(false);
		sail.setLogValidationViolations(false);
		SailRepository repository = new SailRepository(sail);
		repository.init();

		Random r = new Random();

		try {
			for(int i = 0; i<3; i++) {
				list.stream()
					.flatMap(Collection::stream)
					.sorted(Comparator.comparingInt(System::identityHashCode))
					.parallel()
					.forEach(transaction -> {
						try (SailRepositoryConnection connection = repository.getConnection()) {

							connection.begin(isolationLevel);
							if(r.nextBoolean()) {
								connection.add(transaction.addedStatements);
								connection.remove(transaction.removedStatements);
							}else{
								connection.add(transaction.removedStatements);
								connection.remove(transaction.addedStatements);
							}
							try {
								connection.commit();
							} catch (RepositoryException e) {
								if (!(e.getCause() instanceof ShaclSailValidationException)) {
									throw e;
								}
								connection.rollback();
							}

						}

					});
			}

		} finally {
			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin();
				((ShaclSailConnection) connection.getSailConnection()).revalidate();
				connection.commit();
			}
			repository.shutDown();
		}
	}

	abstract NotifyingSail getBaseSail();

	class Transaction {
		List<Statement> addedStatements = new ArrayList<>();
		List<Statement> removedStatements = new ArrayList<>();

		private void add(String turtle, IRI graph) {
			turtle = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.") + turtle;

			StringReader shaclRules = new StringReader(turtle);

			try {
				Model parse = Rio.parse(shaclRules, "", RDFFormat.TURTLE);
				parse.stream()
					.map(statement -> {
						if (graph != null) {
							return vf.createStatement(statement.getSubject(), statement.getPredicate(),
								statement.getObject(), graph);
						}

						return statement;
					})
					.forEach(statement -> addedStatements.add(statement));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		private void remove(String turtle, IRI graph) {
			turtle = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.") + turtle;

			StringReader shaclRules = new StringReader(turtle);

			try {
				Model parse = Rio.parse(shaclRules, "", RDFFormat.TURTLE);
				parse.stream()
					.map(statement -> {
						if (graph != null) {
							return vf.createStatement(statement.getSubject(), statement.getPredicate(),
								statement.getObject(), graph);
						}

						return statement;
					})
					.forEach(statement -> removedStatements.add(statement));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
	}

}

