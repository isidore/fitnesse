package fitnesse.responders.testHistory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

import org.approvaltests.Approvals;
import org.approvaltests.UseReporter;
import org.approvaltests.reporters.FailedDiffTextApprovalReport;

import util.FileUtil;
import util.ListUtility;
import fitnesse.http.Request;
import fitnesse.http.SettableRequest;
import fitnesse.http.SimpleResponse;
import fitnesse.http.Response.ReasonCodes;
import fitnesse.responders.testHistory.CompareResults.Comparison;
import fitnesse.responders.testHistory.HistoryComparer.ComparerResults;
import fitnesse.responders.testHistory.HistoryComparerResponder.FileExister;
import fitnesse.testutil.FitNesseUtil;

@UseReporter(FailedDiffTextApprovalReport.class)
public class HistoryComparerResponderTest extends TestCase {

  public void testShouldBeAbleToCompareTwoHistories() throws Exception {
    String content = "<table><tr><td>This is the content</td></tr></table>";
    CompareResults results = new CompareResults(Comparison.Comparable,
        ListUtility.list(content), ListUtility.list(content), ListUtility
            .list(ComparerResults.MATCH), ListUtility.asList(new MatchedPair(0,
            0, .1)));
    results.setCompleteMatch(false);

    SimpleResponse response = runForResults(results, true);
    assertEquals(ReasonCodes.OK, response.getStatus());
    Approvals.approveHtml(response.getContent());
  }

  public void testShouldReturnErrorPageIfCompareFails() throws Exception {
    CompareResults results = new CompareResults(Comparison.Uncomparable);
    SimpleResponse response = runForResults(results, true);
    assertEquals(400, response.getStatus());
    Approvals.approveHtml(response.getContent());
  }

  public void testShouldReturnErrorPageIfFilesDontExist() throws Exception {
    SimpleResponse response = runForResults(null, false);
    assertEquals(400, response.getStatus());
    Approvals.approveHtml(response.getContent());
  }

  public void testShouldReturnErrorPageIfOnly1InputFile() throws Exception {
    SettableRequest request = new SettableRequest();
    request.addInput(Request.TEST_RESULT_PREFIX + "20090215111430.txt", "");
    request.setResource("TestFolder");

    HistoryComparerResponder responder = new HistoryComparerResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(
        FitNesseUtil.makeTestContext(null), request);
    
    assertEquals(400, response.getStatus());
    Approvals.approveHtml(response.getContent());
  }

  private SimpleResponse runForResults(CompareResults results,
      boolean filesExist) throws Exception {
    HistoryComparerResponder responder = new HistoryComparerResponder();
    responder.fileExister = new FilesExistMock(filesExist);
    SimpleResponse response = (SimpleResponse) responder.makeResponse(
        FitNesseUtil.makeTestContext(null), setupRequest(responder, results));
    return response;
  }

  private Request setupRequest(HistoryComparerResponder responder,
      CompareResults results) throws Exception {
    String folder = "TestFolder";
    String basePath = FileUtil
        .useCorrectPathSeperator("./TestDir/files/testResults/" + folder + "/");
    String name1 = "20090215111430.txt";
    String name2 = "20090215111435.txt";
    HistoryComparer mockedComparer = mockOutComparer(results, basePath + name1,
        basePath + name2);
    responder.setTestComparer(mockedComparer);

    SettableRequest request = createCompareRequestForFiles(folder, name1, name2);

    return request;
  }

  private SettableRequest createCompareRequestForFiles(String folder,
      String name1, String name2) {
    SettableRequest request = new SettableRequest();
    request.addInput(Request.TEST_RESULT_PREFIX + name1, "");
    request.addInput(Request.TEST_RESULT_PREFIX + name2, "");
    request.setResource(folder);
    return request;
  }

  private HistoryComparer mockOutComparer(CompareResults results,
      String firstFile, String secondFile) throws Exception {

    HistoryComparer mockedComparer = mock(HistoryComparer.class);
    when(mockedComparer.compare(firstFile, secondFile)).thenReturn(results);
    return mockedComparer;
  }

  /* Inner Classes */

  public static class FilesExistMock extends FileExister {
    private final boolean exists;

    public FilesExistMock(boolean exists) {
      this.exists = exists;
    }

    @Override
    public boolean filesExist(String... files) {
      return exists;
    }
  }

}
