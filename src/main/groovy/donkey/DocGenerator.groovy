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
import javax.ws.rs.*

@SupportedAnnotationTypes("javax.ws.rs.Path") @SupportedSourceVersion(SourceVersion.RELEASE_6)
class DocGenerator extends AbstractProcessor
{
    @Override boolean process(final Set<? extends TypeElement> typeElements, final RoundEnvironment roundEnvironment)
    {
        roundEnvironment.getElementsAnnotatedWith(Path).each {
            if (it.kind == ElementKind.CLASS)
            {
                message("Found: " + it.simpleName)
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
        def docs = documentation(element)

        html.html {
            head {
                title 'REST API for ' + element.simpleName.toString()
            }
            body {
                if (intro != null)
                {
                    div {
                        mkp.yieldUnescaped intro
                    }
                }
                table {
                    thead {
                        tr {
                            th('Path')
                            th('Method')
                            th('Consumes')
                            th('Produces')
                        }
                    }
                    docs.each { d ->
                        tr {
                            td(d.path)
                            td(d.method)
                        }
                        tr {
                            td(d.description)
                        }
                    }
                }
            }
        }

        return w.toString()
    }

    def documentation(final Element element)
    {
        def basePath = element.getAnnotation(Path).value()
        def result = new ArrayList<Doc>()

        element.enclosedElements.each { docElement(it, basePath, result) }

        return result
    }

    def docElement(final Element element, final String basePath, final ArrayList<Doc> result)
    {
        if (element.kind == ElementKind.METHOD)
        {
            if (shouldDocument(element))
            {
                result.add(doc(element, basePath))
            }
        }
    }

    def doc(final Element element, final String basePath)
    {
        def d = new Doc(method(element), path(basePath, element))
        d.description = processingEnv.elementUtils.getDocComment(element)

        return d
    }

    def path(final String basePath, final Element element)
    {
        if (element.getAnnotation(Path) != null)
        {
            return basePath + element.getAnnotation(Path).value()
        }

        return basePath
    }

    def shouldDocument(final Element element)
    {
        if (element.getAnnotation(GET))
        {
            return true
        }
        if (element.getAnnotation(POST))
        {
            return true
        }
        if (element.getAnnotation(PUT))
        {
            return true
        }
        if (element.getAnnotation(DELETE))
        {
            return true
        }

        return false
    }

    def method(final Element element)
    {
        if (element.getAnnotation(GET))
        {
            return "GET"
        }
        if (element.getAnnotation(POST))
        {
            return "POST"
        }
        if (element.getAnnotation(PUT))
        {
            return "PUT"
        }
        if (element.getAnnotation(DELETE))
        {
            return "DELETE"
        }

        return "UNKNOWN"
    }

    def store(final Element element, final String filename, final String content)
    {
        processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", filename, element).openWriter().withWriter { w -> w.write(content) }
    }

    def message(final String message)
    {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, message)
    }
}

class Doc
{
    final String method
    final String path
    String description

    Doc(final String method, final String path)
    {
        this.method = method
        this.path = path
    }
}
