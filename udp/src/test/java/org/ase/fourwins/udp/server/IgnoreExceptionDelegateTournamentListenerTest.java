package org.ase.fourwins.udp.server;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class IgnoreExceptionDelegateTournamentListenerTest {

	private final IgnoreExceptionDelegateTournamentListener sut = new IgnoreExceptionDelegateTournamentListener(
			throwEx());

	private static TournamentListener throwEx() {
		return mock(TournamentListener.class, i -> {
			throw new UnsupportedOperationException("method: " + i.getMethod());
		});
	}

	@TestFactory
	Stream<DynamicTest> allExceptionsToCallsToInterfaceMethodAreCaught() {
		return stream(TournamentListener.class.getMethods()).map(this::callMethodOnSut);
	}

	private DynamicTest callMethodOnSut(Method method) {
		return dynamicTest(method.getName(), () -> tapSystemErr(() -> method.invoke(sut, emptyArgs(method))));
	}

	private static Object[] emptyArgs(Method method) {
		return stream(method.getParameters()).map(IgnoreExceptionDelegateTournamentListenerTest::emptyArg)
				.toArray(Object[]::new);
	}

	private static Object emptyArg(Parameter parameter) {
		return parameter.getType() == boolean.class ? false : parameter.getType().isPrimitive() ? 0 : null;
	}

}
