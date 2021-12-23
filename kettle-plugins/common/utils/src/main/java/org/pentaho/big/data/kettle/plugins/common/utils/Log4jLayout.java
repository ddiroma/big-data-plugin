package org.pentaho.big.data.kettle.plugins.common.utils;

import org.apache.logging.log4j.core.LogEvent;


public interface Log4jLayout {
  public String format( LogEvent event );

  public boolean ignoresThrowable();

  public void activateOptions();

  public boolean isTimeAdded();

  public void setTimeAdded( boolean addTime );
}
