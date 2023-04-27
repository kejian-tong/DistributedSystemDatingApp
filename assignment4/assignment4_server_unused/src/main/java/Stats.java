public class Stats {
  private Integer numLikes;
  private Integer numDislikes;

  public Integer getNumLikes() {
    return numLikes;
  }

  public Integer getNumDislikes() {
    return numDislikes;
  }

  public Stats numLikes(Integer numLikes) {
    this.numLikes = numLikes;
    return this;

  }

  public Stats numDislikes(Integer numDislikes) {
    this.numDislikes = numDislikes;
    return this;
  }
}
