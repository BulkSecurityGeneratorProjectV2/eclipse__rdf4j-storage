/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Håvard Mikkelsen Ottestad
 */
abstract class LoggingCloseableIteration implements CloseableIteration<Tuple, SailException> {

	private final ValidationExecutionLogger validationExecutionLogger;
	private PlanNode planNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public LoggingCloseableIteration(PlanNode planNode, ValidationExecutionLogger validationExecutionLogger) {
		this.planNode = planNode;
		this.validationExecutionLogger = validationExecutionLogger;
	}

	static String leadingSpace(PlanNode planNode) {
		return leadingSpace(planNode.depth());
	}

	static String leadingSpace(int depth) {
		StringBuilder builder = new StringBuilder();
		for (int i = depth; i > 0; i--) {
			builder.append("  ");
		}
		return builder.toString();
	}

	@Override
	public final Tuple next() throws SailException {

		Tuple tuple = loggingNext();

		if (GlobalValidationExecutionLogging.loggingEnabled) {
			validationExecutionLogger.log(planNode.depth(), planNode.getClass().getSimpleName() + ".next()", tuple,
					planNode, planNode.getId());
		}
		return tuple;
	}

	@Override
	public final boolean hasNext() throws SailException {
		return localHasNext();
	}

	abstract Tuple loggingNext() throws SailException;

	abstract boolean localHasNext() throws SailException;

	private String leadingSpace() {
		return leadingSpace(planNode);
	}

}
