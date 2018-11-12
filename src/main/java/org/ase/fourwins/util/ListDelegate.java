package org.ase.fourwins.util;

import java.util.List;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class ListDelegate<T> implements List<T> {
	@Delegate
	private final List<T> delegate;
}