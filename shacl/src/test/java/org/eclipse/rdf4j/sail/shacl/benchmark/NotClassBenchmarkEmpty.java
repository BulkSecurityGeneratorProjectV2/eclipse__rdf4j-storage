/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import ch.qos.logback.classic.Logger;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NotClassBenchmarkEmpty {

	private List<List<Statement>> allStatements;
	private List<List<Statement>> allStatementsWithoutAnimals;

	private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private final IRI ANIMAL = vf.createIRI("http://example.com/ns#Animal");

	@Setup(Level.Iteration)
	public void setUp() {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI person = vf.createIRI("http://example.com/" + i + "_" + j);
			IRI friend = vf.createIRI("http://example.com/friend" + i + "_" + j);

			statements.add(vf.createStatement(person, RDF.TYPE, FOAF.PERSON));
			statements.add(vf.createStatement(person, FOAF.KNOWS, friend));
			statements.add(vf.createStatement(friend, RDF.TYPE, FOAF.PERSON));

			IRI notAPerson = vf.createIRI("http://example.com/notAPerson" + i + "_" + j);
			IRI animal = vf.createIRI("http://example.com/animal" + i + "_" + j);

			statements.add(vf.createStatement(notAPerson, FOAF.KNOWS, animal));
			statements.add(vf.createStatement(animal, RDF.TYPE, ANIMAL));
		}));

		allStatementsWithoutAnimals = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI person = vf.createIRI("http://example.com/" + i + "_" + j);
			IRI friend = vf.createIRI("http://example.com/friend" + i + "_" + j);

			statements.add(vf.createStatement(person, RDF.TYPE, FOAF.PERSON));
			statements.add(vf.createStatement(person, FOAF.KNOWS, friend));
			statements.add(vf.createStatement(friend, RDF.TYPE, FOAF.PERSON));
		}));

		System.gc();

	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclNotClassBenchmark.ttl"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}

	@Benchmark
	public void shaclWithoutAnimals() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclNotClassBenchmark.ttl"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatementsWithoutAnimals) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}

	@Benchmark
	public void noShacl() {

		SailRepository repository = new SailRepository(new MemoryStore());

		repository.init();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}

	@Benchmark
	public void sparqlInsteadOfShacl() {

		SailRepository repository = new SailRepository(new MemoryStore());

		repository.init();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				try (Stream<BindingSet> stream = Iterations
						.stream(connection.prepareTupleQuery("select * where {?a a <" + FOAF.PERSON + ">. ?a <"
								+ FOAF.KNOWS + "> ?c. ?c a <" + ANIMAL + "> }").evaluate())) {
					stream.forEach(System.out::println);
				}
				connection.commit();
			}
		}

	}

	@Benchmark
	public void sparqlInsteadOfShaclWithoutAnimals() {

		SailRepository repository = new SailRepository(new MemoryStore());

		repository.init();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatementsWithoutAnimals) {
				connection.begin();
				connection.add(statements);
				try (Stream<BindingSet> stream = Iterations
						.stream(connection.prepareTupleQuery("select * where {?a a <" + FOAF.PERSON + ">. ?a <"
								+ FOAF.KNOWS + "> ?c. ?c a <" + ANIMAL + "> }").evaluate())) {
					stream.forEach(System.out::println);
				}
				connection.commit();
			}
		}

	}

}
