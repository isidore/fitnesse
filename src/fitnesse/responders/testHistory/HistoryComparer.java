package fitnesse.responders.testHistory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.util.ParserException;

import fitnesse.responders.run.TestExecutionReport;
import fitnesse.responders.testHistory.CompareResults.Comparison;
import fitnesse.slimTables.HtmlTableScanner;

public class HistoryComparer {
  public static class ComparerResults {

    public static final String NO_MATCH = "fail";
    public static final String MATCH = "pass";
  }

  private TableListComparer comparer;

  public String secondFileContent = "";
  public String firstFileContent = "";
  public File resultFile;
  public ArrayList<String> resultContent = new ArrayList<String>();
  public HtmlTableScanner firstScanner;
  public HtmlTableScanner secondScanner;
  public List<String> firstTableResults;
  public List<String> secondTableResults;
  public ArrayList<MatchedPair> matchedTables;
  private static final String blankTable = "<table><tr><td></td></tr></table>";

  public String getFileContent(String filePath) {
    TestExecutionReport report;
    try {
      report = new TestExecutionReport().read(new File(filePath));
      if (report.getResults().size() != 1)
        return null;
      return report.getResults().get(0).getContent();
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  public boolean allTablesMatch() {
    if (matchedTables == null || matchedTables.size() == 0
        || firstTableResults == null || firstTableResults.size() == 0)
      return false;
    if (matchedTables.size() == firstTableResults.size()) {
      for (MatchedPair match : matchedTables) {
        if (match.matchScore < (MatchedPair.MAX_MATCH_SCORE - .01))
          return false;
      }
      return true;
    }
    return false;
  }

  public CompareResults compare(String firstFilePath, String secondFilePath)
      throws Exception {
    if (firstFilePath.equals(secondFilePath))
      return new CompareResults(Comparison.Uncomparable);
    initializeFileContents(firstFilePath, secondFilePath);
    return grabAndCompareTablesFromHtml();
  }

  public CompareResults grabAndCompareTablesFromHtml() throws ParserException {
    initializeComparerHelpers();
    if (firstScanner.getTableCount() == 0 || secondScanner.getTableCount() == 0)
      return new CompareResults(Comparison.Uncomparable);
    comparer = new TableListComparer(firstScanner, secondScanner);
    comparer.compareAllTables();
    matchedTables = comparer.tableMatches;
    getTableTextFromScanners();
    lineUpTheTables();
    addBlanksToUnmatchingRows();
    makePassFailResultsFromMatches();
    CompareResults compareResults = new CompareResults(Comparison.Comparable,
        firstTableResults, secondTableResults, resultContent, matchedTables);
    compareResults.setCompleteMatch(allTablesMatch());
    return compareResults;
  }

  private void initializeComparerHelpers() throws ParserException {
    matchedTables = new ArrayList<MatchedPair>();
    firstScanner = new HtmlTableScanner(firstFileContent);
    secondScanner = new HtmlTableScanner(secondFileContent);
  }

  public void lineUpTheTables() {
    for (int currentMatch = 0; currentMatch < matchedTables.size(); currentMatch++)
      lineUpMatch(currentMatch);
    lineUpLastRow();

  }

  private void lineUpMatch(int currentMatch) {
    insertBlanksUntilMatchLinesUp(new FirstResultAdjustmentStrategy(),
        currentMatch);
    insertBlanksUntilMatchLinesUp(new SecondResultAdjustmentStrategy(),
        currentMatch);
  }

  private void insertBlanksUntilMatchLinesUp(
      ResultAdjustmentStrategy adjustmentStrategy, int currentMatch) {
    while (adjustmentStrategy.matchIsNotLinedUp(currentMatch)) {
      adjustmentStrategy.insertBlankTableBefore(currentMatch);
      incrementRemaingMatchesToCompensateForInsertion(adjustmentStrategy,
          currentMatch);
    }
  }

  private void incrementRemaingMatchesToCompensateForInsertion(
      ResultAdjustmentStrategy adjustmentStrategy, int currentMatch) {
    for (int matchToAdjust = currentMatch; matchToAdjust < matchedTables.size(); matchToAdjust++) {
      matchedTables.set(matchToAdjust, adjustmentStrategy
          .getAdjustedMatch(matchToAdjust));
    }
  }

  private interface ResultAdjustmentStrategy {
    boolean matchIsNotLinedUp(int matchIndex);

    void insertBlankTableBefore(int matchIndex);

    MatchedPair getAdjustedMatch(int matchIndex);
  }

  private class FirstResultAdjustmentStrategy implements
      ResultAdjustmentStrategy {
    public boolean matchIsNotLinedUp(int matchIndex) {
      MatchedPair matchedPair = matchedTables.get(matchIndex);
      return matchedPair.first < matchedPair.second;
    }

    public void insertBlankTableBefore(int matchIndex) {
      firstTableResults.add(matchedTables.get(matchIndex).first, blankTable);
    }

    public MatchedPair getAdjustedMatch(int matchIndex) {
      MatchedPair matchedPair = matchedTables.get(matchIndex);
      return new MatchedPair(matchedPair.first + 1, matchedPair.second,
          matchedPair.matchScore);

    }
  }

  private class SecondResultAdjustmentStrategy implements
      ResultAdjustmentStrategy {
    public boolean matchIsNotLinedUp(int matchIndex) {
      MatchedPair matchedPair = matchedTables.get(matchIndex);
      return matchedPair.first > matchedPair.second;
    }

    public void insertBlankTableBefore(int matchIndex) {
      secondTableResults.add(matchedTables.get(matchIndex).second, blankTable);
    }

    public MatchedPair getAdjustedMatch(int matchIndex) {
      MatchedPair matchedPair = matchedTables.get(matchIndex);
      return new MatchedPair(matchedPair.first, matchedPair.second + 1,
          matchedPair.matchScore);
    }
  }

  private void lineUpLastRow() {
    while (firstTableResults.size() > secondTableResults.size())
      secondTableResults.add(blankTable);
    while (secondTableResults.size() > firstTableResults.size())
      firstTableResults.add(blankTable);
  }

  public void addBlanksToUnmatchingRows() {
    for (int tableIndex = 0; tableIndex < firstTableResults.size(); tableIndex++) {
      if (tablesDontMatchAndArentBlank(tableIndex)) {
        insetBlanksToSplitTheRow(tableIndex);
        incrementMatchedPairsIfBelowTheInsertedBlank(tableIndex);
      }
    }
  }

  private boolean tablesDontMatchAndArentBlank(int tableIndex) {
    return !thereIsAMatchForTableWithIndex(tableIndex)
        && firstAndSecondTableAreNotBlank(tableIndex);
  }

  private boolean thereIsAMatchForTableWithIndex(int tableIndex) {
    return MatchedPair.findMatchByFirstTableIndex(matchedTables, tableIndex)
        .getScore() > 0.1;
  }

  private boolean firstAndSecondTableAreNotBlank(int tableIndex) {
    return !(firstTableResults.get(tableIndex).equals(blankTable) || secondTableResults
        .get(tableIndex).equals(blankTable));
  }

  private void incrementMatchedPairsIfBelowTheInsertedBlank(int tableIndex) {
    for (int j = 0; j < matchedTables.size(); j++) {
      MatchedPair match = matchedTables.get(j);
      if (match.first > tableIndex)
        matchedTables.set(j, new MatchedPair(match.first + 1, match.second + 1,
            match.matchScore));
    }
  }

  private void insetBlanksToSplitTheRow(int tableIndex) {
    secondTableResults.add(tableIndex, blankTable);
    firstTableResults.add(tableIndex + 1, blankTable);
  }

  private void getTableTextFromScanners() {
    firstTableResults = new ArrayList<String>();
    secondTableResults = new ArrayList<String>();
    for (int i = 0; i < firstScanner.getTableCount(); i++)
      firstTableResults.add(firstScanner.getTable(i).toHtml());

    for (int i = 0; i < secondScanner.getTableCount(); i++)
      secondTableResults.add(secondScanner.getTable(i).toHtml());
  }

  public void makePassFailResultsFromMatches() {
    for (int i = 0; i < firstTableResults.size(); i++) {
      String result = ComparerResults.NO_MATCH;
      for (MatchedPair match : matchedTables) {
        if (match.first == i && match.matchScore >= 1.19) {
          result = ComparerResults.MATCH;
        }
      }
      resultContent.add(result);

    }
  }

  private void initializeFileContents(String firstFilePath,
      String secondFilePath) throws ParserException {
    String content = getFileContent(firstFilePath);
    firstFileContent = content == null ? "" : content;
    content = getFileContent(secondFilePath);
    secondFileContent = content == null ? "" : content;
  }

  public List<String> getResultContent() {
    return resultContent;
  }

}
