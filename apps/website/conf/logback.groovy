/*
 * This configures logback, an indirect dependency of ours through
 * akka. Without this file we suffer from terrible logspam because
 * some idiot set the value to ALL in the library.
 */

import static ch.qos.logback.classic.Level.INFO

root(INFO)