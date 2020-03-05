package org.ase.fourwins.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class CollectionUtil {

	@SafeVarargs
	public static <T> List<T> reverse(T... lines) {
		return reverse(List.of(lines));
	}

	public static <T> List<T> reverse(Collection<T> lines) {
		List<T> reversed = new ArrayList<>(lines);
		Collections.reverse(reversed);
		return reversed;
	}

}
