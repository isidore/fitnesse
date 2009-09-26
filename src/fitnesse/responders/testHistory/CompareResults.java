package fitnesse.responders.testHistory;

import java.util.Date;
import java.util.List;

public class CompareResults {

  private final Comparison comparison;
  private final List<String> firstTableResults;
  private final List<String> secondTableResults;
  private final List<String> resultContent;
  private final List<MatchedPair> matchedTables;
  private Date firstTableCreationDate;
  private Date secondTableCreationDate;
  private boolean completeMatch;

  public enum Comparison {
    Uncomparable, Comparable

  }

  public CompareResults(Comparison comparison) {
    this(comparison, null, null, null, null);

  }

  public Date getFirstTableCreationDate() {
    return firstTableCreationDate;
  }

  public Date getSecondTableCreationDate() {
    return secondTableCreationDate;
  }

  public CompareResults(Comparison comparison, List<String> firstTableResults,
      List<String> secondTableResults, List<String> resultContent,
      List<MatchedPair> matchedTables) {
    this.comparison = comparison;
    this.firstTableResults = firstTableResults;
    this.secondTableResults = secondTableResults;
    this.resultContent = resultContent;
    this.matchedTables = matchedTables;
  }

  public boolean isComparisonPossible() {
    return comparison == Comparison.Comparable;
  }

  public List<String> getFirstTableResults() {
    return firstTableResults;
  }

  public List<String> getSecondTableResults() {
    return secondTableResults;
  }

  public List<String> getResultContent() {
    return resultContent;
  }

  public MatchedPair findMatchByFirstTableIndex(int firstIndex) {
    return MatchedPair.findMatchByFirstTableIndex(matchedTables, firstIndex);
  }

  public void setFirstFileDate(Date firstTableCreationDate) {
    this.firstTableCreationDate = firstTableCreationDate;
  }

  public void setSecondFileDate(Date secondTableCreationDate) {
    this.secondTableCreationDate = secondTableCreationDate;
  }

  public boolean isCompleteMatch() {
    return completeMatch;
  }

  public void setCompleteMatch(boolean completeMatch) {
    this.completeMatch = completeMatch;
  }

}
