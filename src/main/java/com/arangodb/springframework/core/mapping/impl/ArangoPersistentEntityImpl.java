/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.mapping.impl;

import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import com.arangodb.entity.CollectionType;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.Document;
import com.arangodb.springframework.core.mapping.Edge;

/**
 * @author Mark - mark at arangodb.com
 * @param <T>
 *
 */
public class ArangoPersistentEntityImpl<T> extends BasicPersistentEntity<T, ArangoPersistentProperty>
		implements ArangoPersistentEntity<T> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private String collection;
	private CollectionType collectionType;
	private final StandardEvaluationContext context;

	public ArangoPersistentEntityImpl(final TypeInformation<T> information) {
		super(information);
		collection = StringUtils.uncapitalize(information.getType().getSimpleName());
		collectionType = CollectionType.DOCUMENT;
		context = new StandardEvaluationContext();

		final Optional<Document> document = findAnnotation(Document.class);
		document.ifPresent(d -> {
			collection = StringUtils.hasText(d.collection()) ? d.collection() : collection;
			collectionType = CollectionType.DOCUMENT;
		});
		final Optional<Edge> edge = findAnnotation(Edge.class);
		edge.ifPresent(e -> {
			collection = StringUtils.hasText(e.collection()) ? e.collection() : collection;
			collectionType = CollectionType.EDGES;
		});
		final Expression expression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
		if (expression != null) {
			collection = expression.getValue(context, String.class);
		}
	}

	@Override
	public String getCollection() {
		return collection;
	}

	@Override
	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		context.setRootObject(applicationContext);
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.addPropertyAccessor(new BeanFactoryAccessor());
	}

}