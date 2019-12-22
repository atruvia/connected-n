package com.github.stefanbirkner.systemlambda;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.Class.forName;
import static java.lang.System.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;

/**
 * {@code SystemLambda} is a collection of functions for testing code
 * that uses {@code java.lang.System}. It provides support for
 * <ul>
 *     <li>Ensuring that nothing is written to
 *     {@link #assertNothingWrittenToSystemErr(Statement) System.err} and/or
 *     {@link #assertNothingWrittenToSystemOut(Statement) System.out}</li>
 *     <li>Suppress output to
 *     {@link #muteSystemErr(Statement) System.err} and/or
 *     {@link #muteSystemOut(Statement) System.out}</li>
 *     <li>Tap output that is written to
 *     {@link #tapSystemErr(Statement) System.err} and/or
 *     {@link #tapSystemOut(Statement) System.out}. Also tapped output with
 *     normalized line separator for
 *     {@link #tapSystemErrNormalized(Statement) System.err} and/or
 *     {@link #tapSystemOutNormalized(Statement) System.out}.</li>
 *     <li>Using a specific {@code SecurityManager}:
 *     {@link #withSecurityManager(SecurityManager, Statement)}</li>
 *     <li>Specify text that is returned by System.in:
 *     {@link #withTextFromSystemIn(String...)} </li>
 *     <li>Set environment variables:
 *     {@link #withEnvironmentVariable(String, String)}</li>
 *     <li>Working with system properties:
 *     {@link #restoreSystemProperties(Statement)}</li>
 * </ul>
 */
public class SystemLambda {

	private static final boolean AUTO_FLUSH = true;
	private static final String DEFAULT_ENCODING = Charset.defaultCharset().name();

	/**
	 * Executes the statement and fails (throws an {@code AssertionError}) if
	 * the statement tries to write to {@code System.err}.
	 * <p>The following test fails
	 * <pre>
	 * &#064;Test
	 * public void fails_because_something_is_written_to_System_err() {
	 *   assertNothingWrittenToSystemErr(
	 *     () -&gt; {
	 *       System.err.println("some text");
	 *     }
	 *   );
	 * }
	 * </pre>
	 * The test fails with the failure "Tried to write 's' to System.err
	 * although this is not allowed."
	 *
	 * @param statement an arbitrary piece of code.
	 * @throws Exception any exception thrown by the statement or an
	 *                   {@code AssertionError} if the statement tries to write
	 *                   to {@code System.err}.
	 * @see #assertNothingWrittenToSystemOut(Statement)
	 * @since 1.0.0
	 */
	public static void assertNothingWrittenToSystemErr(
		Statement statement
	) throws Exception {
		executeWithSystemErrReplacement(
			new DisallowWriteStream(),
			statement
		);
	}

	/**
	 * Executes the statement and fails (throws an {@code AssertionError}) if
	 * the statement tries to write to {@code System.out}.
	 * <p>The following test fails
	 * <pre>
	 * &#064;Test
	 * public void fails_because_something_is_written_to_System_out() {
	 *   assertNothingWrittenToSystemOut(
	 *     () -&gt; {
	 *       System.out.println("some text");
	 *     }
	 *   );
	 * }
	 * </pre>
	 * The test fails with the failure "Tried to write 's' to System.out
	 * although this is not allowed."
	 *
	 * @param statement an arbitrary piece of code.
	 * @throws Exception any exception thrown by the statement or an
	 *                   {@code AssertionError} if the statement tries to write
	 *                   to {@code System.out}.
	 * @see #assertNothingWrittenToSystemErr(Statement)
	 * @since 1.0.0
	 */
	public static void assertNothingWrittenToSystemOut(
		Statement statement
	) throws Exception {
		executeWithSystemOutReplacement(
			new DisallowWriteStream(),
			statement
		);
	}

	/**
	 * Usually the output of a test to {@code System.err} does not have to be
	 * visible. It may even slowdown the test. {@code muteSystemErr} can be
	 * used to suppress this output.
	 * <pre>
	 * &#064;Test
	 * public void nothing_is_written_to_System_err() {
	 *   muteSystemErr(
	 *     () -&gt; {
	 *       System.err.println("some text");
	 *     }
	 *   );
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @throws Exception any exception thrown by the statement.
	 * @see #muteSystemOut(Statement)
	 * @since 1.0.0
	 */
	public static void muteSystemErr(
		Statement statement
	) throws Exception {
		executeWithSystemErrReplacement(
			new NoopStream(),
			statement
		);
	}

	/**
	 * Usually the output of a test to {@code System.out} does not have to be
	 * visible. It may even slowdown the test. {@code muteSystemOut} can be
	 * used to suppress this output.
	 * <pre>
	 * &#064;Test
	 * public void nothing_is_written_to_System_out() {
	 *   muteSystemOut(
	 *     () -&gt; {
	 *       System.out.println("some text");
	 *     }
	 *   );
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @throws Exception any exception thrown by the statement.
	 * @see #muteSystemErr(Statement)
	 * @since 1.0.0
	 */
	public static void muteSystemOut(
		Statement statement
	) throws Exception {
		executeWithSystemOutReplacement(
			new NoopStream(),
			statement
		);
	}

	/**
	 * {@code tapSystemErr} returns a String with the text that is written to
	 * {@code System.err} by the provided piece of code.
	 * <pre>
	 * &#064;Test
	 * public void check_the_text_that_is_written_to_System_err() {
	 *   String textWrittenToSystemErr = tapSystemErr(
	 *     () -&gt; {
	 *       System.err.print("some text");
	 *     }
	 *   );
	 *   assertEquals("some text", textWrittenToSystemErr);
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @return text that is written to {@code System.err} by the statement.
	 * @throws Exception any exception thrown by the statement.
	 * @see #tapSystemOut(Statement)
	 * @since 1.0.0
	 */
	public static String tapSystemErr(
		Statement statement
	) throws Exception {
		TapStream tapStream = new TapStream();
		executeWithSystemErrReplacement(
			tapStream,
			statement
		);
		return tapStream.textThatWasWritten();
	}

	/**
	 * {@code tapSystemOut} returns a String with the text that is written to
	 * {@code System.out} by the provided piece of code.
	 * <pre>
	 * &#064;Test
	 * public void check_the_text_that_is_written_to_System_out() {
	 *   String textWrittenToSystemOut = tapSystemOut(
	 *     () -&gt; {
	 *       System.out.print("some text");
	 *     }
	 *   );
	 *   assertEquals("some text", textWrittenToSystemOut);
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @return text that is written to {@code System.out} by the statement.
	 * @throws Exception any exception thrown by the statement.
	 * @see #tapSystemErr(Statement)
	 * @since 1.0.0
	 */
	public static String tapSystemOut(
		Statement statement
	) throws Exception {
		TapStream tapStream = new TapStream();
		executeWithSystemOutReplacement(
			tapStream,
			statement
		);
		return tapStream.textThatWasWritten();
	}

	/**
	 * {@code tapSystemErrNormalized} returns a String with the text that is
	 * written to {@code System.err} by the provided piece of code. New line
	 * characters are replaced with a single {@code \n}.
	 * <pre>
	 * &#064;Test
	 * public void check_the_text_that_is_written_to_System_err() {
	 *   String textWrittenToSystemErr = tapSystemErrNormalized(
	 *     () -&gt; {
	 *       System.err.println("some text");
	 *     }
	 *   );
	 *   assertEquals("some text\n", textWrittenToSystemErr);
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @return text that is written to {@code System.err} by the statement.
	 * @throws Exception any exception thrown by the statement.
	 * @see #tapSystemOut(Statement)
	 * @since 1.0.0
	 */
	public static String tapSystemErrNormalized(
		Statement statement
	) throws Exception {
		return tapSystemErr(statement)
			.replace(lineSeparator(), "\n");
	}

	/**
	 * {@code tapSystemOutNormalized} returns a String with the text that is
	 * written to {@code System.out} by the provided piece of code. New line
	 * characters are replaced with a single {@code \n}.
	 * <pre>
	 * &#064;Test
	 * public void check_the_text_that_is_written_to_System_out() {
	 *   String textWrittenToSystemOut = tapSystemOutNormalized(
	 *     () -&gt; {
	 *       System.out.println("some text");
	 *     }
	 *   );
	 *   assertEquals("some text\n", textWrittenToSystemOut);
	 * }
	 * </pre>
	 *
	 * @param statement an arbitrary piece of code.
	 * @return text that is written to {@code System.out} by the statement.
	 * @throws Exception any exception thrown by the statement.
	 * @see #tapSystemErr(Statement)
	 * @since 1.0.0
	 */
	public static String tapSystemOutNormalized(
		Statement statement
	) throws Exception {
		return tapSystemOut(statement)
			.replace(lineSeparator(), "\n");
	}

	/**
	 * Executes the statement and restores the system properties after the
	 * statement has been executed. This allows you to set or clear system
	 * properties within the statement without affecting other tests.
	 * <pre>
	 * &#064;Test
	 * public void execute_code_that_manipulates_system_properties(
	 * ) throws Exception {
	 *   System.clearProperty("some property");
	 *   System.setProperty("another property", "value before test");
	 *
	 *   restoreSystemProperties(
	 *     () -&gt; {
	 *       System.setProperty("some property", "some value");
	 *       assertEquals(
	 *         "some value",
	 *         System.getProperty("some property")
	 *       );
	 *
	 *       System.clearProperty("another property");
	 *       assertNull(
	 *         System.getProperty("another property")
	 *       );
	 *     }
	 *   );
	 *
	 *   //values are restored after test
	 *   assertNull(
	 *     System.getProperty("some property")
	 *   );
	 *   assertEquals(
	 *     "value before test",
	 *     System.getProperty("another property")
	 *   );
	 * }
	 * </pre>
	 * @param statement an arbitrary piece of code.
	 * @throws Exception any exception thrown by the statement.
	 * @since 1.0.0
	 */
	public static void restoreSystemProperties(
		Statement statement
	) throws Exception {
		Properties originalProperties = getProperties();
		setProperties(copyOf(originalProperties));
		try {
			statement.execute();
		} finally {
			setProperties(originalProperties);
		}
	}

	private static Properties copyOf(Properties source) {
		Properties copy = new Properties();
		copy.putAll(source);
		return copy;
	}

	/**
	 * Executes a statement with the specified environment variables. All
	 * changes to environment variables are reverted after the statement has
	 * been executed.
	 * <pre>
	 * &#064;Test
	 * public void execute_code_with_environment_variables(
	 * ) throws Exception {
	 *   withEnvironmentVariable("first", "first value")
	 *     .and("second", "second value")
	 *     .and("third", null)
	 *     .execute(
	 *       () -&gt; {
	 *         assertEquals(
	 *           "first value",
	 *           System.getenv("first")
	 *         );
	 *         assertEquals(
	 *           "second value",
	 *           System.getenv("second")
	 *         );
	 *         assertNull(
	 *           System.getenv("third")
	 *         );
	 *       }
	 *     );
	 * }
	 * </pre>
	 * <p>You cannot specify the value of an an environment variable twice. An
	 * {@code IllegalArgumentException} when you try.
	 * <p><b>Warning:</b> This method uses reflection for modifying internals of the
	 * environment variables map. It fails if your {@code SecurityManager} forbids
	 * such modifications.
	 * @param name the name of the environment variable.
	 * @param value the value of the environment variable.
	 * @return an {@link WithEnvironmentVariables} instance that can be used to
	 * set more variables and run a statement with the specified environment
	 * variables.
	 * @since 1.0.0
	 * @see WithEnvironmentVariables#and(String, String)
	 * @see WithEnvironmentVariables#execute(Statement)
	 */
	public static WithEnvironmentVariables withEnvironmentVariable(
		String name,
		String value
	) {
		return new WithEnvironmentVariables(
			singletonMap(name, value));
	}

    /**
     * Executes the statement with the provided security manager.
     * <pre>
     * &#064;Test
     * public void execute_code_with_specific_SecurityManager() {
     *   SecurityManager securityManager = new ASecurityManager();
     *   withSecurityManager(
     *     securityManager,
     *     () -&gt; {
     *       assertSame(securityManager, System.getSecurityManager());
     *     }
     *   );
     * }
     * </pre>
     * The specified security manager is only present during the test.
     * @param securityManager the security manager that is used while the
     *                        statement is executed.
     * @param statement an arbitrary piece of code.
     * @throws Exception any exception thrown by the statement.
     * @since 1.0.0
     */
    public static void withSecurityManager(
        SecurityManager securityManager,
        Statement statement
    ) throws Exception {
        SecurityManager originalSecurityManager = getSecurityManager();
        setSecurityManager(securityManager);
        try {
            statement.execute();
        } finally {
            setSecurityManager(originalSecurityManager);
        }
    }

	/**
	 * Executes the statement and lets {@code System.in} provide the specified
	 * text during the execution. In addition several Exceptions can be
	 * specified that are thrown when {@code System.in#read} is called.
	 *
	 * <pre>
	 *   public void MyTest {
	 *
	 *     &#064;Test
	 *     public void readTextFromStandardInputStream() {
	 *       withTextFromSystemIn("foo", "bar")
	 *         .execute(() -&gt; {
	 *           Scanner scanner = new Scanner(System.in);
	 *           scanner.nextLine();
	 *           assertEquals("bar", scanner.nextLine());
	 *         });
	 *     }
	 *   }
	 * </pre>
	 *
	 * <h3>Throwing Exceptions</h3>
	 * <p>You can also simulate a {@code System.in} that throws an
	 * {@code IOException} or {@code RuntimeException}. Use
	 *
	 * <pre>
	 *   public void MyTest {
	 *
	 *     &#064;Test
	 *     public void readTextFromStandardInputStreamFailsWithIOException() {
	 *       withTextFromSystemIn()
	 *         .andExceptionThrownOnInputEnd(new IOException())
	 *         .execute(() -&gt; {
	 *           assertThrownBy(
	 *             IOException.class,
	 *             () -&gt; new Scanner(System.in).readLine())
	 *           );
	 *         )};
	 *     }
	 *
	 *     &#064;Test
	 *     public void readTextFromStandardInputStreamFailsWithRuntimeException() {
	 *       withTextFromSystemIn()
	 *         .andExceptionThrownOnInputEnd(new RuntimeException())
	 *         .execute(() -&gt; {
	 *           assertThrownBy(
	 *             RuntimeException.class,
	 *             () -&gt; new Scanner(System.in).readLine())
	 *           );
	 *         )};
	 *     }
	 *   }
	 * </pre>
	 * <p>If you provide text as parameters of {@code withTextFromSystemIn(...)}
	 * in addition then the exception is thrown after the text has been read
	 * from {@code System.in}.
	 * @param lines the lines that are available from {@code System.in}.
	 * @return an {@link SystemInStub} instance that is used to execute a
	 * statement with its {@link SystemInStub#execute(Statement) execute}
	 * method. In addition it can be used to specify an exception that is thrown
	 * after the text is read.
	 * @since 1.0.0
	 * @see SystemInStub#execute(Statement)
	 * @see SystemInStub#andExceptionThrownOnInputEnd(IOException)
	 * @see SystemInStub#andExceptionThrownOnInputEnd(RuntimeException)
	 */
    public static SystemInStub withTextFromSystemIn(
    	String... lines
	) {
    	String text = stream(lines)
			.map(line -> line + getProperty("line.separator"))
			.collect(joining());
    	return new SystemInStub(text);
	}

	private static void executeWithSystemErrReplacement(
		OutputStream replacementForErr,
		Statement statement
	) throws Exception {
		PrintStream originalStream = err;
		try {
			setErr(wrap(replacementForErr));
			statement.execute();
		} finally {
			setErr(originalStream);
		}
	}

	private static void executeWithSystemOutReplacement(
		OutputStream replacementForOut,
		Statement statement
	) throws Exception {
		PrintStream originalStream = out;
		try {
			setOut(wrap(replacementForOut));
			statement.execute();
		} finally {
			setOut(originalStream);
		}
	}

	private static PrintStream wrap(
		OutputStream outputStream
	) throws UnsupportedEncodingException {
		return new PrintStream(
			outputStream,
			AUTO_FLUSH,
			DEFAULT_ENCODING
		);
	}

	private static class DisallowWriteStream extends OutputStream {
		@Override
		public void write(int b) {
			throw new AssertionError(
				"Tried to write '"
					+ (char) b
					+ "' although this is not allowed."
			);
		}
	}

	private static class NoopStream extends OutputStream {
		@Override
		public void write(
			int b
		) {
		}
	}

	private static class TapStream extends OutputStream {
		final ByteArrayOutputStream text = new ByteArrayOutputStream();

		@Override
		public void write(
			int b
		) {
			text.write(b);
		}

		String textThatWasWritten() {
			return text.toString();
		}
	}

	/**
	 * A collection of values for environment variables. New values can be
	 * added by {@link #and(String, String)}. The {@code EnvironmentVariables}
	 * object is then used to execute an arbitrary piece of code with these
	 * environment variables being present.
	 */
	public static final class WithEnvironmentVariables {
		private final Map<String, String> variables;

		private WithEnvironmentVariables(
			Map<String, String> variables
		) {
			this.variables = variables;
		}

		/**
		 * Creates a new {@code WithEnvironmentVariables} object that
		 * additionally stores the value for an additional environment variable.
		 * <p>You cannot specify the value of an environment variable twice. An
		 * {@code IllegalArgumentException} when you try.
		 * @param name the name of the environment variable.
		 * @param value the value of the environment variable.
		 * @return a new {@code WithEnvironmentVariables} object.
		 * @throws IllegalArgumentException when a value for the environment
		 * variable {@code name} is already specified.
		 * @see #withEnvironmentVariable(String, String)
		 * @see #execute(Statement)
		 */
    	public WithEnvironmentVariables and(
    		String name,
			String value
		) {
    		validateNotSet(name, value);
			HashMap<String, String> moreVariables = new HashMap<>(variables);
			moreVariables.put(name, value);
			return new WithEnvironmentVariables(moreVariables);
		}

		private void validateNotSet(
			String name,
			String value
		) {
			if (variables.containsKey(name)) {
				String currentValue = variables.get(name);
				throw new IllegalArgumentException(
					"The environment variable '" + name + "' cannot be set to "
						+ format(value) + " because it was already set to "
						+ format(currentValue) + "."
				);
			}
		}

		private String format(
			String text
		) {
    		if (text == null)
    			return "null";
    		else
    			return "'" + text + "'";
		}

		/**
		 * Executes a statement with environment variable values according to
		 * what was set before. All changes to environment variables are
		 * reverted after the statement has been executed.
		 * <pre>
		 * &#064;Test
		 * public void execute_code_with_environment_variables(
		 * ) throws Exception {
		 *   withEnvironmentVariable("first", "first value")
		 *     .and("second", "second value")
		 *     .and("third", null)
		 *     .execute(
		 *       () -&gt; {
		 *         assertEquals(
		 *           "first value",
		 *           System.getenv("first")
		 *         );
		 *         assertEquals(
		 *           "second value",
		 *           System.getenv("second")
		 *         );
		 *         assertNull(
		 *           System.getenv("third")
		 *         );
		 *       }
		 *     );
		 * }
		 * </pre>
		 * <p><b>Warning:</b> This method uses reflection for modifying internals of the
		 * environment variables map. It fails if your {@code SecurityManager} forbids
		 * such modifications.
		 * @throws Exception any exception thrown by the statement.
		 * @since 1.0.0
		 * @see #withEnvironmentVariable(String, String)
		 * @see WithEnvironmentVariables#and(String, String)
		 */
    	public void execute(
    		Statement statement
		) throws Exception {
    		Map<String, String> originalVariables = new HashMap<>(getenv());
    		try {
				setEnvironmentVariables();
				statement.execute();
			} finally {
				restoreOriginalVariables(originalVariables);
			}
		}

		private void setEnvironmentVariables() {
			overrideVariables(
				getEditableMapOfVariables()
			);
			overrideVariables(
				getTheCaseInsensitiveEnvironment()
			);
		}

		private void overrideVariables(
			Map<String, String> existingVariables
		) {
			if (existingVariables != null) //theCaseInsensitiveEnvironment may be null
				variables.forEach(
					(name, value) -> set(existingVariables, name, value)
				);
		}

		private void set(
			Map<String, String> variables,
			String name,
			String value
		) {
			if (value == null)
				variables.remove(name);
			else
				variables.put(name, value);
		}

		void restoreOriginalVariables(
			Map<String, String> originalVariables
		) {
			restoreVariables(
				getEditableMapOfVariables(),
				originalVariables
			);
			restoreVariables(
				getTheCaseInsensitiveEnvironment(),
				originalVariables
			);
		}

		void restoreVariables(
			Map<String, String> variables,
			Map<String, String> originalVariables
		) {
    		if (variables != null) {//theCaseInsensitiveEnvironment may be null
				variables.clear();
				variables.putAll(originalVariables);
			}
		}

		private static Map<String, String> getEditableMapOfVariables() {
			Class<?> classOfMap = getenv().getClass();
			try {
				return getFieldValue(classOfMap, getenv(), "m");
			} catch (IllegalAccessException e) {
				throw new RuntimeException("System Rules cannot access the field"
					+ " 'm' of the map System.getenv().", e);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException("System Rules expects System.getenv() to"
					+ " have a field 'm' but it has not.", e);
			}
		}

		/*
		 * The names of environment variables are case-insensitive in Windows.
		 * Therefore it stores the variables in a TreeMap named
		 * theCaseInsensitiveEnvironment.
		 */
		private static Map<String, String> getTheCaseInsensitiveEnvironment() {
			try {
				Class<?> processEnvironment = forName("java.lang.ProcessEnvironment");
				return getFieldValue(
					processEnvironment, null, "theCaseInsensitiveEnvironment");
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("System Rules expects the existence of"
					+ " the class java.lang.ProcessEnvironment but it does not"
					+ " exist.", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("System Rules cannot access the static"
					+ " field 'theCaseInsensitiveEnvironment' of the class"
					+ " java.lang.ProcessEnvironment.", e);
			} catch (NoSuchFieldException e) {
				//this field is only available for Windows
				return null;
			}
		}

		private static Map<String, String> getFieldValue(
			Class<?> klass,
			Object object,
			String name
		) throws NoSuchFieldException, IllegalAccessException {
			Field field = klass.getDeclaredField(name);
			field.setAccessible(true);
			return (Map<String, String>) field.get(object);
		}
	}

	public static class SystemInStub {
		private IOException ioException;
		private RuntimeException runtimeException;
		private final String text;

		private SystemInStub(String text) {
			this.text = text;
		}

		public SystemInStub andExceptionThrownOnInputEnd(
			IOException exception
		) {
			if (runtimeException != null)
				throw new IllegalStateException("You cannot call"
					+ " andExceptionThrownOnInputEnd(IOException) because"
					+ " andExceptionThrownOnInputEnd(RuntimeException) has"
					+ " already been called.");
			this.ioException = exception;
			return this;
		}

		public SystemInStub andExceptionThrownOnInputEnd(
			RuntimeException exception
		) {
			if (ioException != null)
				throw new IllegalStateException("You cannot call"
					+ " andExceptionThrownOnInputEnd(RuntimeException) because"
					+ " andExceptionThrownOnInputEnd(IOException) has already"
					+ " been called.");
			this.runtimeException = exception;
			return this;
		}

		public void execute(
			Statement statement
		) throws Exception {
			InputStream stubStream = new ReplacementInputStream(
				text, ioException, runtimeException
			);
			InputStream originalIn = System.in;
			try {
				setIn(stubStream);
				statement.execute();
			} finally {
				setIn(originalIn);
			}
		}


		private static class ReplacementInputStream extends InputStream {
			private final StringReader reader;
			private final IOException ioException;
			private final RuntimeException runtimeException;

			ReplacementInputStream(
				String text,
				IOException ioException,
				RuntimeException runtimeException
			) {
				this.reader = new StringReader(text);
				this.ioException = ioException;
				this.runtimeException = runtimeException;
			}

			@Override
			public int read() throws IOException {
				int character = reader.read();
				if (character == -1)
					handleEmptyReader();
				return character;
			}

			private void handleEmptyReader() throws IOException {
				if (ioException != null)
					throw ioException;
				else if (runtimeException != null)
					throw runtimeException;
			}

			@Override
			public int read(byte[] buffer, int offset, int len) throws IOException {
				if (buffer == null)
					throw new NullPointerException();
				else if (offset < 0 || len < 0 || len > buffer.length - offset)
					throw new IndexOutOfBoundsException();
				else if (len == 0)
					return 0;
				else
					return readNextLine(buffer, offset, len);
			}

			private int readNextLine(byte[] buffer, int offset, int len)
				throws IOException {
				int c = read();
				if (c == -1)
					return -1;
				buffer[offset] = (byte) c;

				int i = 1;
				for (; (i < len) && !isCompleteLineWritten(buffer, i - 1); ++i) {
					byte read = (byte) read();
					if (read == -1)
						break;
					else
						buffer[offset + i] = read;
				}
				return i;
			}

			private boolean isCompleteLineWritten(byte[] buffer,
												  int indexLastByteWritten) {
				byte[] separator = getProperty("line.separator")
					.getBytes(defaultCharset());
				int indexFirstByteOfSeparator = indexLastByteWritten
					- separator.length + 1;
				return indexFirstByteOfSeparator >= 0
					&& contains(buffer, separator, indexFirstByteOfSeparator);
			}

			private boolean contains(byte[] array, byte[] pattern, int indexStart) {
				for (int i = 0; i < pattern.length; ++i)
					if (array[indexStart + i] != pattern[i])
						return false;
				return true;
			}
		}
	}
}
