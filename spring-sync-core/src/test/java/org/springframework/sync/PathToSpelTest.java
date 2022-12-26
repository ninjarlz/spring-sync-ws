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

import org.junit.Test;
import org.springframework.expression.Expression;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PathToSpelTest {

	@Test
	public void listIndex() {
		Expression expr = PathToSpEL.pathToExpression("/1/description");
		List<Todo> todos = new ArrayList<>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		assertEquals("B", (String) expr.getValue(todos));		
	}
	
	@Test
	public void listTilde() {
		Expression expr = PathToSpEL.pathToExpression("/~/description");
		List<Todo> todos = new ArrayList<>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		assertEquals("C", (String) expr.getValue(todos));
	}
	
}
