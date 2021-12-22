package org.pentaho.big.data.kettle.plugins.job;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.StringWriter;
import java.io.Writer;

public class LogUtils {
  public static void addAppender( Appender appender, Logger logger, Level level ) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
    Configuration config = ctx.getConfiguration();
    appender.start();
    config.addAppender( appender );
    LoggerConfig loggerConfig = config.getLoggerConfig( logger.getName() );
    loggerConfig.addAppender( appender, level, null );
    ctx.updateLoggers();
  }

  public static void removeAppender( Appender appender, Logger logger ) {
    appender.stop();
    LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig( logger.getName() );
    loggerConfig.removeAppender( logger.getName() );
    ctx.updateLoggers();
  }

  public static Appender makeAppender( String name, StringWriter stringWriter, String layout ) {
    return WriterAppender.newBuilder().setName( name )
      .setLayout( PatternLayout.newBuilder().withPattern( layout ).build() ).setTarget( stringWriter ).build();
  }

  public static Appender makeAppender( String name, Writer writer, Layout layout ) {
    return WriterAppender.newBuilder().setName( name ).setLayout( layout ).setTarget( writer ).build();
  }
}
