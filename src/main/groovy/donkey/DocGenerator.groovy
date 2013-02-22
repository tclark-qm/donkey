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
                div(class: 'hero-unit') {
                    h1(element.simpleName.toString())

                    if (intro != null)
                    {
                        mkp.yieldUnescaped intro
                    }
                }
                div(class: 'row') {
                    div(class: 'span11 offset1') {
                        docs.each { d ->
                            div(class: 'well') {
                                h3(d.label)
                                p {
                                    mkp.yieldUnescaped d.description
                                }
                                h4('Endpoint')
                                dl(class: 'dl-horizontal') {
                                    dt('Path')
                                    dd(d.path)
                                    dt('Method')
                                    dd(d.method)
                                    dt('Consumes')
                                    dd(d.consumes)
                                    dt('Produces')
                                    dd(d.produces)
                                }
                                if (d.pathParams.size() > 0)
                                {
                                    h4('Path parameters')
                                    dl(class: 'dl-horizontal') {
                                        d.pathParams.each { p ->
                                            dt(p.name)
                                            dd(p.typeName)
                                        }
                                    }
                                }
                                if (d.headerParams.size() > 0)
                                {
                                    h4('Header parameters')
                                    dl(class: 'dl-horizontal') {
                                        d.headerParams.each { p ->
                                            dt(p.name)
                                            dd(p.typeName)
                                        }
                                    }
                                }
                                h4('Request body')
                                p(d.requestBody)

                                h4('Response')
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
        def result = new ArrayList<MethodDocumentation>()

        element.enclosedElements.each {
            if (it.kind == ElementKind.METHOD)
            {
                if (shouldDocument(it))
                {
                    result.add(new MethodDocumentation(it, resource, processingEnv))
                }
            }
        }

        return result
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

class Param
{
    final String name
    final String typeName

    Param(final String name, final String typeName)
    {
        this.name = name
        this.typeName = typeName
    }
}
