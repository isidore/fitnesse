package fitnesse.responders.testHistory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import util.FileUtil;
import edu.emory.mathcs.backport.java.util.Arrays;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SettableRequest;
import fitnesse.http.Response.ReasonCodes;
import fitnesse.responders.testHistory.HistoryComparer.ComparerResults;
import fitnesse.testutil.FitNesseUtil;

public class HistoryComparerResponder2Test {

  @Test
  public void shouldBeAbleToCompareTwoHistories() throws Exception {

    HistoryComparerResponder responder = new HistoryComparerResponder();
    Response response = responder.makeResponse(FitNesseUtil
        .makeTestContext(null), setupRequest(responder));
    assertEquals(ReasonCodes.OK, response.getStatus());
    FileUtil.deleteFileSystemDirectory(FitNesseUtil.TEST_DIR);
  }

  private Request setupRequest(HistoryComparerResponder responder)
      throws Exception {
    String histories[] = createHistoryFiles("firstFakeFile", "secondFakeFile");
    HistoryComparer mockedComparer = mockOutComparer(histories[0], histories[1]);
    responder.setTestComparer(mockedComparer);

    SettableRequest request = new SettableRequest();
    request.addInput("TestResult_firstFakeFile", "");
    request.addInput("TestResult_secondFakeFile", "");
    request.setResource("TestFolder");

    return request;
  }

  private String[] createHistoryFiles(String name1, String name2) {
    String basePath = FileUtil
        .useCorrectPathSeperator("./TestDir/files/testResults/TestFolder/");
    String firstHistory = basePath + name1;
    String secondHistory = basePath + name2;
    ensureTestResultsExist(firstHistory, secondHistory);
    return new String[] { firstHistory, secondHistory };
  }

  public static void ensureTestResultsExist(String... files) {
    for (String file : files) {
      FileUtil.createFile(file, "");
    }
  }

  private HistoryComparer mockOutComparer(String firstFile, String secondFile)
      throws Exception {
    HistoryComparer mockedComparer = mock(HistoryComparer.class);
    when(mockedComparer.getResultContent()).thenReturn(
        asList(ComparerResults.MATCH));
    when(mockedComparer.compare(firstFile, secondFile)).thenReturn(true);

    String content = "<table><tr><td>This is the content</td></tr></table>";
    mockedComparer.firstTableResults = asList(content);
    mockedComparer.secondTableResults = asList(content);
    return mockedComparer;
  }

  private List<String> asList(String... match) {
    return Arrays.asList(match);
  }
}
