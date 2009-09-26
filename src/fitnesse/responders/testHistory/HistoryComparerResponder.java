package fitnesse.responders.testHistory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.velocity.context.Context;
import org.apache.velocity.util.ContextAware;

import util.VelocityUtils;
import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.responders.ErrorResponder;
import fitnesse.responders.templateUtilities.PageTitle;
import fitnesse.wiki.PathParser;

public class HistoryComparerResponder implements Responder, ContextAware {
  public static class FileExister {
    public boolean filesExist(String... files) {
      for (String file : files) {
        if (!new File(file).exists()) {
          return false;
        }
      }
      return true;
    }
  }

  private static final String COMPARE_HISTORY_TEMPLATE = "compareHistory.vm";
  private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
      TestHistory.TEST_RESULT_FILE_DATE_PATTERN);

  private HistoryComparer comparer;
  private PageTitle pageTitle;
  private CompareResults compareResult;
  FileExister fileExister = new FileExister();

  public HistoryComparerResponder(HistoryComparer historyComparer) {
    comparer = historyComparer;
  }

  public HistoryComparerResponder() {
    comparer = new HistoryComparer();
  }

  public Response makeResponse(FitNesseContext context, Request request)
      throws Exception {
    pageTitle = new PageTitle("Test History", PathParser.parse(request
        .getResource()));
    List<String> fileNames = getFileNamesFromRequest(request);
    if (fileNames.size() != 2)
      return new ErrorResponder(
          ("Compare Failed because the wrong number of Input Files were given. "
              + "Select two please.")).makeResponse(context, request);
    String firstFilePath = composeFileName(request, fileNames.get(0), context);
    String secondFilePath = composeFileName(request, fileNames.get(1), context);

    if (!fileExister.filesExist(firstFilePath, secondFilePath))
      return new ErrorResponder(
          "Compare Failed because the files were not found.").makeResponse(
          context, request);

    return makeResponseFromComparison(context, request, fileNames,
        firstFilePath, secondFilePath);
  }

  private Response makeResponseFromComparison(FitNesseContext context,
      Request request, List<String> fileNames, String firstFilePath,
      String secondFilePath) throws Exception {
    compareResult = comparer.compare(firstFilePath, secondFilePath);
    compareResult.setFirstFileDate(parseDateFromFile(fileNames.get(0)));
    compareResult.setSecondFileDate(parseDateFromFile(fileNames.get(1)));
    return renderResponse(context, request, compareResult);
  }

  public Response renderResponse(FitNesseContext context, Request request,
      CompareResults compareResult) throws Exception {
    this.compareResult = compareResult;
    if (compareResult.isComparisonPossible())
      return makeValidResponse();
    else {
      String message = String.format("These files could not be compared."
          + "  They might be suites, or something else might be wrong.");
      return new ErrorResponder(message).makeResponse(context, request);
    }
  }

  private static Date parseDateFromFile(String fileName) {
    try {
      return DATE_FORMAT.parse(fileName);
    } catch (ParseException e) {
      return null;
    }
  }

  private String composeFileName(Request request, String fileName,
      FitNesseContext context) {
    return context.getTestHistoryDirectory().getPath() + File.separator
        + request.getResource() + File.separator + fileName;
  }

  private List<String> getFileNamesFromRequest(Request request) {
    Set<String> keys = request.getMap().keySet();
    ArrayList<String> files = new ArrayList<String>();
    for (String key : keys) {
      if (key.contains(Request.TEST_RESULT_PREFIX)) {
        files.add(key.substring(key.indexOf("_") + 1));
      }
    }
    return files;
  }

  private Response makeValidResponse() throws Exception {
    String text = VelocityUtils.parseTemplate(COMPARE_HISTORY_TEMPLATE, this);
    return makeResponseFromTemplate(text);

  }

  private Response makeResponseFromTemplate(String text) throws Exception {
    SimpleResponse response = new SimpleResponse();
    response.setContent(text);
    return response;
  }

  public void setTestComparer(HistoryComparer comparer) {
    this.comparer = comparer;
  }

  @Override
  public void setContext(Context velocityContext) {

    velocityContext.put("results", compareResult);
    velocityContext.put("pageTitle", pageTitle);

  }
}
