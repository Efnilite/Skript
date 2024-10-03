package ch.njol.skript.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an expression that can be simplified to a {@link Literal} at parse-time.
 *
 * @param <T> The type of the literal.
 */
@FunctionalInterface
public interface Simplifiable<T> {

	/**
	 * Simplifies the expression to a {@link Literal} at parse-time.
	 * Will only be called if {@link Expression#isSimplifiable()} returns true, so should always return a value.
	 *
	 * @return the simplified expression if it can be simplified
	 */
	@NotNull Literal<? extends T> simplified();

}
