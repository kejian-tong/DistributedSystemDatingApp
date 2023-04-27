package assignment4.config.util;

public class Pair {
  private boolean isUrlPathValid;
  private String param; // direction;

  public Pair(boolean isUrlPathValid, String param) {
    this.isUrlPathValid = isUrlPathValid;
    this.param = param;
  }


  public boolean isUrlPathValid() {
    return isUrlPathValid;
  }

  public String getParam() {
    return param;
  }


}
