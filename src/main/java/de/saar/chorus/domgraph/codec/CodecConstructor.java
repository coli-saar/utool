/*
 * @(#)CodecConstructor.java created 21.10.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation type for marking the constructor of a codec
 * class that should be used to construct new objects of this
 * class from the codec manager. If the codec class defines
 * only one public constructor, annotating it with CodecConstructor
 * is optional. However, if your codec class defines multiple
 * public constructors (e.g. because it was automatically
 * generated by JavaCC), then use this annotation to specify
 * the relevant one. 
 * 
 * @author Alexander Koller
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface CodecConstructor {

}
