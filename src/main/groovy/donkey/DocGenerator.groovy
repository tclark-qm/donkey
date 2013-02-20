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
                store(it, "rest-doc-" + it.simpleName.toString() + ".html", markup(it))
            }
        }
        return false
    }

    def markup(final Element element)
    {
        def w = new StringWriter()
        w.write('<!DOCTYPE html>\n')

        def html = new groovy.xml.MarkupBuilder(w)

        def intro = processingEnv.elementUtils.getDocComment(element)
        def docs = documentation(element)

        html.html(lang: 'en') {
            head {
                title 'REST API for ' + element.simpleName.toString()
                link(href: 'http://netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/css/bootstrap-combined.min.css', rel: 'stylesheet')
            }
            body {
                h1(element.simpleName.toString())
                if (intro != null)
                {
                    div {
                        mkp.yieldUnescaped intro
                    }
                }
                div(class: 'row') {
                    div(class: 'span12') {
                        docs.each { d ->
                            h3(d.label)
                            table(class: 'table') {
                                thead {
                                    tr {
                                        th('Path')
                                        th('Method')
                                        th('Consumes')
                                        th('Produces')
                                    }
                                }
                                tr {
                                    td(d.path)
                                    td(d.method)
                                    td(d.consumes)
                                    td(d.produces)
                                }
                            }
                        }
                    }
                }
                script('', src: 'http://code.jquery.com/jquery.js')
                script('', src: 'http://netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/js/bootstrap.min.js')
            }
        }

        return w.toString()
    }

    def documentation(final Element element)
    {
        def produces
        def consumes

        if (element.getAnnotation(Consumes) != null)
        {
            consumes = element.getAnnotation(Consumes).value()
        }
        else
        {
            consumes = 'Unknown'
        }

        if (element.getAnnotation(Produces) != null)
        {
            produces = element.getAnnotation(Produces).value()
        }
        else
        {
            produces = 'Unknown'
        }

        def resource = new Resource(element.getAnnotation(Path).value(), consumes as String, produces as String)
        def result = new ArrayList<Doc>()

        element.enclosedElements.each { docElement(it, resource, result) }

        return result
    }

    def docElement(final Element element, final Resource resource, final ArrayList<Doc> result)
    {
        if (element.kind == ElementKind.METHOD)
        {
            if (shouldDocument(element))
            {
                result.add(doc(element, resource))
            }
        }
    }

    def doc(final Element element, final Resource resource)
    {
        def document = new Doc(method(element), path(resource.basePath, element))
        document.label = element.simpleName.toString()
        if (processingEnv.elementUtils.getDocComment(element))
        {
            document.description = processingEnv.elementUtils.getDocComment(element)
        }
        else {
            document.description = 'No description provided'
        }
        document.consumes = '' + resource.consumes
        document.produces = '' + resource.produces

        return document
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

class Resource
{
    final String basePath
    final String consumes
    final String produces

    Resource(final String basePath, final String consumes, final String produces)
    {
        this.basePath = basePath
        this.consumes = consumes
        this.produces = produces
    }
}

class Doc
{
    final String method
    final String path
    String label
    String description
    String consumes
    String produces

    Doc(final String method, final String path)
    {
        this.method = method
        this.path = path
    }
}
