package util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.util.ContextAware;

import com.spun.util.parser.ParserCommons;

import fitnesse.VelocityFactory;

public class VelocityUtils {

  public static String parseTemplate(String velocityTemplate, ContextAware aware)
      throws Exception, IOException {
    Template template = VelocityFactory.getVelocityEngine().getTemplate(
        velocityTemplate);
    StringWriter writer = new StringWriter();
    VelocityContext velocityContext = new VelocityContext();
    aware.setContext(velocityContext);
    velocityContext.put("common", new ParserCommons());

    template.merge(velocityContext, writer);
    return writer.toString();
  }

}
