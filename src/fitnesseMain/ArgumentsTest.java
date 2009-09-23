// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesseMain;

import junit.framework.TestCase;

import org.approvaltests.Approvals;

import fitnesse.Arguments;

public class ArgumentsTest extends TestCase {

  public void testSimpleCommandline() throws Exception {
    approveArguments(null);
  }

  private void approveArguments(String a) throws Exception {
    String[] split = a == null ? new String[0] : a.split(" ");
    Arguments args = FitNesseMain.parseCommandLine(split);
    Approvals.approve(args.toString());
  }

  public void testArgumentsAlternates() throws Exception {
    approveArguments("-p 123 -d MyWd -r MyRoot -l LogDir -e 322 -o -a userpass.txt -i");
  }

  public void testAllArguments() throws Exception {
    approveArguments("-p 81 -d directory -r root -l myLogDirectory -o -e 22");
  }

  public void testNotOmitUpdates() throws Exception {
    approveArguments("-p 81 -d directory -r root -l myLogDirectory");
  }

  public void testBadArgument() throws Exception {
    assertNull(FitNesseMain.parseCommandLine((new String[] { "-x" })));
  }
}
