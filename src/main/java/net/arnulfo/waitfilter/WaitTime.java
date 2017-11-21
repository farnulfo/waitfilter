package net.arnulfo.waitfilter;

/**
 *
 * @author Franck Arnulfo
 */
public class WaitTime implements WaitTimeMBean {

  private long waitTime;

  public WaitTime(long waitTime) {
    this.waitTime = waitTime;
  }

  @Override
  public long getWaitTime() {
    return waitTime;
  }

  @Override
  public synchronized void setWaitTime(long waitTime) {
    this.waitTime = waitTime;
  }

}
