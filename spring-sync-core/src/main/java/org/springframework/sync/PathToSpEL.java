/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.sync;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Array;

/**
 * Utilities for converting patch paths to/from SpEL expressions.
 * 
 * For example, "/foo/bars/1/baz" becomes "foo.bars[1].baz".
 * 
 * @author Craig Walls
 */
public class PathToSpEL {

	private static final String SIZE_SUFFIX = "[size() - 1]";
	private static final String THIS_ENTRY = "#this";

	private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

	/**
	 * Converts a patch path to an {@link Expression}.
	 * @param path the patch path to convert.
	 * @return an {@link Expression}
	 */
	public static Expression pathToExpression(String path) {
		return SPEL_EXPRESSION_PARSER.parseExpression(pathToSpEL(path));
	}
	
	/**
	 * Convenience method to convert a SpEL String to an {@link Expression}.
	 * @param spel the SpEL expression as a String
	 * @return an {@link Expression}
	 */
	public static Expression spelToExpression(String spel) {
		return SPEL_EXPRESSION_PARSER.parseExpression(spel);
	}	
	
	/**
	 * Produces an expression targeting the parent of the object that the given path targets. 
	 * @param path the path to find a parent expression for.
	 * @return an {@link Expression} targeting the parent of the object specifed by path.
	 */
	public static Expression pathToParentExpression(String path) {
		return spelToExpression(pathNodesToSpEL(copyOf(path.split("\\/"), path.split("\\/").length - 1)));
	}

	// private helpers
	
	private static String pathToSpEL(String path) {
		return pathNodesToSpEL(path.split("\\/"));
	}
	
	private static String pathNodesToSpEL(String[] pathNodes) {
		StringBuilder spelBuilder = new StringBuilder();

		for (String pathNode : pathNodes) {
			if (pathNode.length() == 0) {
				continue;
			}

			if ("~".equals(pathNode)) {
				spelBuilder.append(SIZE_SUFFIX);
				continue;
			}

			try {
				int index = Integer.parseInt(pathNode);
				spelBuilder.append('[').append(index).append(']');
			} catch (NumberFormatException e) {
				if (!spelBuilder.isEmpty()) {
					spelBuilder.append('.');
				}
				spelBuilder.append(pathNode);
			}
		}
		
		String spel = spelBuilder.toString();
		if (spel.isEmpty()) {
			return THIS_ENTRY;
		}
		return spel;		
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] copyOf(T[] original, int newLength) {
		return (T[]) copyOf(original, newLength, original.getClass());
	}

	// reproduces Arrays.copyOf because that API is missing on Android 2.2
	private static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
		@SuppressWarnings("unchecked")
		T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
				: (T[]) Array.newInstance(newType.getComponentType(), newLength);
		System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
		return copy;
	}

}
