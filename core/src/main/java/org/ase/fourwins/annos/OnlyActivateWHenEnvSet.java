package org.ase.fourwins.annos;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface OnlyActivateWHenEnvSet {
	String[] value();
}