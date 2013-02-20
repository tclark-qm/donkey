package donkey

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import javax.ws.rs.Path

@SupportedAnnotationTypes("javax.ws.rs.Path") @SupportedSourceVersion(SourceVersion.RELEASE_6)
class DocGenerator extends AbstractProcessor
{
    @Override boolean process(final Set<? extends TypeElement> typeElements, final RoundEnvironment roundEnvironment)
    {
        roundEnvironment.getElementsAnnotatedWith(Path).each {
            if (it.kind == ElementKind.CLASS)
            {
                message("Found: " + it.simpleName + " (" + processingEnv.elementUtils.getDocComment(it) + ")")
                store(it, "rest-doc-" + it.simpleName.toString() + ".html", markup(it))
            }
        }
        return false
    }

    def markup(final Element element)
    {
        def w = new StringWriter()
        def html = new groovy.xml.MarkupBuilder(w)

        def intro = processingEnv.elementUtils.getDocComment(element)

        html.html {
            head {
                title 'REST API for ' + element.simpleName.toString()
            }
            body    {
                if(intro != null)   {
                    div {
                        mkp.yieldUnescaped intro
                    }
                }
            }
        }

        return w.toString()
    }

    def store(final Element element, final String filename, final String content)
    {
        def file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", filename, element)
        file.openWriter().withWriter { w -> w.write(content) }
    }

    def message(final String message)
    {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, message)
    }
}
