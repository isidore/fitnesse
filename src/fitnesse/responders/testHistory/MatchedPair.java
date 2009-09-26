/**
 * 
 */
package fitnesse.responders.testHistory;

import java.util.List;

public class MatchedPair {
  int first;
  int second;
  public double matchScore;
  public static final double MAX_MATCH_SCORE = 1.2;
  // min for match is .8 content score + .2 topology bonus.
  public static final double MIN_MATCH_SCORE = .8;

  public MatchedPair(Integer first, Integer second, double matchScore) {
    this.first = first;
    this.second = second;
    this.matchScore = matchScore;
  }

  public double getScore() {
    return matchScore;
  }

  public double getScoreAsPercentage() {
    return matchScore / MAX_MATCH_SCORE;
  }

  @Override
  public String toString() {
    return "[first: " + first + ", second: " + second + ", matchScore: "
        + matchScore + "]";
  }

  @Override
  public int hashCode() {
    return this.first + this.second;
  }

  @Override
  public boolean equals(Object obj) {
    MatchedPair match = (MatchedPair) (obj);
    return (this.first == match.first && this.second == match.second);
  }

  public static MatchedPair findMatchByFirstTableIndex(
      List<MatchedPair> matchedTables, int firstIndex) {
    for (MatchedPair match : matchedTables) {
      if (match.first == firstIndex)
        return match;
    }
    return new MatchedPair(-1, -1, 0);
  }

}