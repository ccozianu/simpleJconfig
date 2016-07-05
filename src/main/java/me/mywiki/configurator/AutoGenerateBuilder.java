package me.mywiki.configurator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * use this interface to annotate your read-only api
 * if you want mywikiConfig APT processor to generate the builder.
 * Make sure your environment supports APT processing :
 * https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoGenerateBuilder {}
