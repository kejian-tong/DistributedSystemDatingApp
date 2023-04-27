package assignment4.config.datamodel;

public class MatchStats {
  private Integer numLlikes;
  private Integer numDislikes;

  public MatchStats numLikes(Integer numLikes) {
    this.numLlikes = numLikes;
    return this;

  }

  public MatchStats numDislikes(Integer numDislikes) {
    this.numDislikes = numDislikes;
    return this;
  }
}
