package donkey;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.util.Set;

@SupportedAnnotationTypes("javax.ws.rs.Path") @SupportedSourceVersion(SourceVersion.RELEASE_6)
public class RestDocumentationGenerator extends AbstractProcessor
{
    @Override public boolean process(final Set<? extends TypeElement> typeElements, final RoundEnvironment roundEnvironment)
    {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Path.class))
        {
            if (element.getKind() == ElementKind.CLASS)
            {
                Path path = element.getAnnotation(Path.class);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found class containing resources " + element.getSimpleName() + " " + path.value());

                for (Element e : element.getEnclosedElements())
                {
                    if (e.getKind() == ElementKind.METHOD)
                    {
                        final Path annotation = e.getAnnotation(Path.class);
                        if (annotation != null)
                        {
                            message("\t" + e.getSimpleName() + " " + annotation.value() + " " + httpMethod(e));
                            message("\t\tComment [" + processingEnv.getElementUtils().getDocComment(e) + "]");
                        }
                    }
                }
            }
        }

        return false;
    }

    private String httpMethod(final Element e)
    {
        if (e.getAnnotation(POST.class) != null)
        {
            return "POST";
        }
        else if (e.getAnnotation(GET.class) != null)
        {
            return "GET";
        }
        else if (e.getAnnotation(DELETE.class) != null)
        {
            return "DELETE";
        }
        else if (e.getAnnotation(PUT.class) != null)
        {
            return "PUT";
        }

        return "UNKNOWN";
    }

    private void message(final String message)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }
}
