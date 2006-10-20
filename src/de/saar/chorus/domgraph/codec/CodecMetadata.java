package de.saar.chorus.domgraph.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CodecMetadata {
	String name();
	String extension();
	boolean experimental() default false;
}
