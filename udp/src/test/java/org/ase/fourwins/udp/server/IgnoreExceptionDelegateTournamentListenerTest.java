package org.ase.fourwins.udp.server;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.stream.Stream;

import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.invocation.Invocation;

class IgnoreExceptionDelegateTournamentListenerTest {

	@TestFactory
	Stream<DynamicTest> allExceptionsToCallsToInterfaceMethodAreCaught() {
		return stream(TournamentListener.class.getMethods()).map(this::callMethodOnSut);
	}

	private DynamicTest callMethodOnSut(Method method) {
		TournamentListener mock = mock(TournamentListener.class, i -> {
			throw new UnsupportedOperationException("method: " + i.getMethod());
		});
		TournamentListener sut = new IgnoreExceptionDelegateTournamentListener(mock);

		return dynamicTest(method.getName(), () -> {
			assertThat(tapSystemErr(() -> method.invoke(sut, emptyArgs(method))).isEmpty(), is(false));
			assertThat(Set.of(method), is(methodsCalled(mock)));
		});
	}

	private Set<Method> methodsCalled(TournamentListener mock) {
		return mockingDetails(mock).getInvocations().stream().map(Invocation::getMethod).collect(toSet());
	}

	private static Object[] emptyArgs(Method method) {
		return stream(method.getParameters()).map(IgnoreExceptionDelegateTournamentListenerTest::emptyArg)
				.toArray(Object[]::new);
	}

	private static Object emptyArg(Parameter parameter) {
		return parameter.getType() == boolean.class ? false : parameter.getType().isPrimitive() ? 0 : null;
	}

}
