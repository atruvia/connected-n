package org.ase.fourwins.udp.server;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.ase.fourwins.udp.server.IgnoreExceptions.catchExceptions;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.invocation.Invocation;

class IgnoreExceptionsTest {

	@TestFactory
	Stream<DynamicTest> allExceptionsToCallsToInterfaceMethodAreCaught() {
		return stream(TournamentListener.class.getMethods()).map(this::callMethodOnSut);
	}

	private DynamicTest callMethodOnSut(Method method) {
		TournamentListener mock = mock(TournamentListener.class, i -> {
			throw new UnsupportedOperationException("method: " + i.getMethod());
		});

		List<Exception> exceptions = new ArrayList<>();
		TournamentListener sut = catchExceptions(TournamentListener.class, mock, exceptions::add);

		return dynamicTest(method.getName(), () -> {
			method.invoke(sut, nullArgs(method));
			assertThat(Set.of(method), is(methodsCalled(mock)));
			assertThat(exceptions.size(), is(1));
		});
	}

	private Set<Method> methodsCalled(TournamentListener mock) {
		return mockingDetails(mock).getInvocations().stream().map(Invocation::getMethod).collect(toSet());
	}

	private static Object[] nullArgs(Method method) {
		return stream(method.getParameters()).map(IgnoreExceptionsTest::nullArg).toArray(Object[]::new);
	}

	private static Object nullArg(Parameter parameter) {
		return parameter.getType() == boolean.class ? false : parameter.getType().isPrimitive() ? 0 : null;
	}

}
