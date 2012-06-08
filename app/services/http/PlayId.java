package services.http;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Guice binding annotation that identifies the String ID
 * of the current play application. See services.http.HttpModule
 * Usage:
 *
 * {{{
 *     class SomeService(@PlayId playId: String) {
 *       def printCurrentPlayId {
 *         println("The current play ID (options: test, staging, demo, live) is: " + playId)
 *       }
 *     }
 * }}}
 */
@BindingAnnotation
@Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
public @interface PlayId {}
