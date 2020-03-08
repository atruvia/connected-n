package org.ase.fourwins.udp.server;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.newProxyInstance;
import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class IgnoreExceptions {

	public static <T> T catchExceptions(Class<T> iface, T delegate, Consumer<Exception> exceptionConsumer) {
		return iface.cast(newProxyInstance(currentThread().getContextClassLoader(), new Class[] { iface },
				(bean, method, args) -> invoke(delegate, method, args, exceptionConsumer)));
	}

	private static <T> Object invoke(T delegate, Method method, Object[] args, Consumer<Exception> exceptionConsumer) {
		try {
			return delegate.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(delegate, args);
		} catch (Exception e) {
			exceptionConsumer.accept(e);
		}
		return null;
	}

}