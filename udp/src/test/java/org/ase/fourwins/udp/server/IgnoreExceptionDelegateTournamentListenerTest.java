package org.ase.fourwins.udp.server;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;

import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class IgnoreExceptionDelegateTournamentListenerTest {

	@TestFactory
	Stream<DynamicTest> dynamicTestsWithCollection() {
		IgnoreExceptionDelegateTournamentListener sut = new IgnoreExceptionDelegateTournamentListener(throwEx());
		return Arrays.stream(TournamentListener.class.getMethods()).map(m -> {
			return dynamicTest(m.getName(), () -> tapSystemErr(() -> m.invoke(sut, nulls(m))));
		});

	}

	private static TournamentListener throwEx() {
		return mock(TournamentListener.class, i -> {
			throw new UnsupportedOperationException("method: " + i.getMethod());
		});
	}

	private static Object[] nulls(Method method) {
		return stream(method.getParameters()).map(p -> arg(p)).toArray(Object[]::new);
	}

	private static Object arg(Parameter parameter) {
		return parameter.getType() == boolean.class ? false : parameter.getType().isPrimitive() ? 0 : null;
	}

}
