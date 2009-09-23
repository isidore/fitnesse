package fitnesse.responders.search;

import static fitnesse.wiki.PageData.*;
import static fitnesse.wiki.PageType.*;
import static fitnesse.responders.search.ExecuteSearchPropertiesResponder.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.*;

import util.RegexTestCase;
import fitnesse.FitNesseContext;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.http.SettableRequest;
import fitnesse.http.MockResponseSender;
import fitnesse.http.Response;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperties;

public class ExecuteSearchPropertiesResponderTest extends RegexTestCase {
  private WikiPage root;
  private PageCrawler crawler;
  private ExecuteSearchPropertiesResponder responder;
  private FitNesseContext context;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    FitNesseUtil.makeTestContext(root);
    crawler = root.getPageCrawler();
    responder = new ExecuteSearchPropertiesResponder();
    context = FitNesseUtil.makeTestContext(root);
  }

  @Test
  public void testResponseWithNoParametersWillReturnEmptyPage()
  throws Exception {
    SettableRequest request = setupRequest();
    String content = invokeResponder(request);
    assertSubString("Search Page Properties Results", content);
    assertSubString("No search properties", content);
  }

  @Test
  public void testResponseWithNoMatchesWillReturnEmptyPageList()
  throws Exception {
    SettableRequest request = setupRequest();
    request.addInput(PAGE_TYPE_ATTRIBUTE, "Suite,Normal");

    String content = invokeResponder(request);

    assertSubString("No pages", content);
  }

  @Test
  public void testResponseWithMatchesWillReturnPageList() throws Exception {
    SettableRequest request = setupRequest();
    request.addInput(PAGE_TYPE_ATTRIBUTE, TEST.toString());

    String content = invokeResponder(request);
    String[] titles = { "Page", TEST.toString(), "PageOne"};

    assertOutputHasRowWithLink(content, titles);

    request.addInput("Suites", "filter1");

    content = invokeResponder(request);

    assertHasRegexp("Found 1 result for your search", content);
    String[] titles1 = { "Page", TEST.toString(), "Tags", "PageOne" };
    assertOutputHasRowWithLink(content, titles1);
    assertOutputHasRowWithLabels("filter1,filter2");
  }

  private String invokeResponder(SettableRequest request) throws Exception {
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    return sender.sentData();
  }

  private SettableRequest setupRequest() throws Exception {
    WikiPage page = crawler.addPage(root, PathParser.parse("PageOne"));
    PageData data = page.getData();
    data.setContent("some content");
    WikiPageProperties properties = data.getProperties();
    properties.set(TEST.toString(), "true");
    properties.set("Suites", "filter1,filter2");
    page.commit(data);

    SettableRequest request = new SettableRequest();
    request.setResource("PageOne");
    request.addInput("Action", "Any");
    request.addInput("Security", "Any");
    request.addInput("Special", "Any");
    return request;
  }

  private void assertOutputHasRowWithLink(String content, String... titles) {
    for (String title : titles) {
      assertOutputHasRow(content, title, "a href.*");
    }
  }

  private void assertOutputHasRowWithLabels(String content, String... labels) {
    for (String label : labels) {
      assertOutputHasRow(content, label, "label");
    }
  }

  private void assertOutputHasRow(String content, String title, String tagName) {
    assertHasRegexp("<table.*<tr.*<td.*<" + tagName + ">" + title + "</"
        + tagName.split(" ")[0] + ">", content);
  }

  @Test
  public void testGetPageTypesFromInput() {
    assertPageTypesMatch(TEST);
    assertPageTypesMatch(TEST, NORMAL);
    assertPageTypesMatch(TEST, SUITE, NORMAL);
    //    assertPageTypesMatch("");
  }

  private void assertPageTypesMatch(PageType... pageTypes) {
    SettableRequest request = new SettableRequest();
    List<PageType> types = Arrays.asList(pageTypes);
    final String commaSeparatedPageTypes = buildPageTypeListForRequest(pageTypes);
    request.addInput(PAGE_TYPE_ATTRIBUTE, commaSeparatedPageTypes);
    assertEquals(types, responder.getPageTypesFromInput(request));
  }

  private String buildPageTypeListForRequest(PageType... pageTypes) {
    StringBuffer buffer = new StringBuffer();
    for (PageType type: pageTypes) {
      buffer.append(type.toString());
      buffer.append(',');
    }
    buffer.deleteCharAt(buffer.length()-1);

    final String commaSeparatedPageTypes = buffer.toString();
    return commaSeparatedPageTypes;
  }

  @Test
  public void testGetAttributesFromInput() {
    SettableRequest request = new SettableRequest();
    request.addInput(ACTION, "Edit");

    Map<String, Boolean> foundAttributes = responder.getAttributesFromInput(request);
    assertFalse(foundAttributes.containsKey("Version"));
    assertTrue(foundAttributes.containsKey("Edit"));
    assertTrue(foundAttributes.get("Edit"));

    request.addInput(ACTION, "Edit,Properties");
    foundAttributes = responder.getAttributesFromInput(request);
    assertTrue(foundAttributes.get("Properties"));
  }

  @Test
  public void testPageTypesAreOrEd() throws Exception {
    SettableRequest request = setupRequest();
    request.addInput(PAGE_TYPE_ATTRIBUTE, "Test,Suite");

    String content = invokeResponder(request);
    String[] titles = { "Page", TEST.toString(), "PageOne" };

    assertOutputHasRowWithLink(content, titles);

    request.addInput("Suites", "filter1");

    content = invokeResponder(request);

    assertHasRegexp("Found 1 result for your search", content);
    String[] titles1 = { "Page", TEST.toString(), "Tags", "PageOne" };
    assertOutputHasRowWithLink(content, titles1);
    assertOutputHasRowWithLabels(content, "filter1,filter2");
  }

  @Test
  public void testPageMatchesWithObsoletePages() throws Exception {
    SettableRequest request = setupRequestForObsoletePage();
    request.addInput(PAGE_TYPE_ATTRIBUTE, "Test,Suite");

    String content = invokeResponder(request);
    String[] titles = { "Page", TEST.toString(), "ObsoletePage" };

    assertOutputHasRowWithLink(content, titles);

    request.addInput(SPECIAL, "SetUp,TearDown");

    content = invokeResponder(request);

    assertSubString("No pages", content);
  }

  private SettableRequest setupRequestForObsoletePage() throws Exception {
    WikiPage page = crawler.addPage(root, PathParser.parse("ObsoletePage"));
    PageData data = page.getData();
    data.setContent("some content");
    WikiPageProperties properties1 = data.getProperties();
    properties1.set(TEST.toString(), "true");
    properties1.set("Suites", "filter1,filter2");
    WikiPageProperties properties = properties1;
    properties.set(PropertyPRUNE, "true");
    page.commit(data);

    SettableRequest request = setupRequest();
    request.setResource("ObsoletePage");
    return request;
  }

  public void testFindJustObsoletePages() throws Exception {
    SettableRequest request = setupRequestForObsoletePage();
    request.addInput(PAGE_TYPE_ATTRIBUTE, "Test,Suite,Normal");
    request.addInput(SPECIAL, "obsolete");

    String content = invokeResponder(request);
    String[] titles = { "ObsoletePage" };

    assertOutputHasRowWithLink(content, titles);

  }
}
