package donkey
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.ws.rs.*

class MethodDocumentation
{
    final String method
    final String path
    final String label
    final String description
    final String consumes
    final String produces
    final List<Param> pathParams
    final List<Param> headerParams
    final String requestBody

    MethodDocumentation(final Element element, final Resource resource, final ProcessingEnvironment processingEnv)
    {
        this.method = method(element)
        this.path = path(resource.basePath, element)
        this.label = element.simpleName.toString()

        if (processingEnv.elementUtils.getDocComment(element))
        {
            this.description = processingEnv.elementUtils.getDocComment(element)
        }
        else
        {
            this.description = 'No description provided'
        }

        if (element.getAnnotation(Consumes))
        {
            this.consumes = '' + element.getAnnotation(Consumes).value()
        }
        else
        {
            this.consumes = '' + resource.consumes
        }

        if (element.getAnnotation(Produces))
        {
            this.produces = '' + element.getAnnotation(Produces).value()
        }
        else
        {
            this.produces = '' + resource.produces
        }

        this.pathParams = pathParams(element)
        this.headerParams = headerParams(element)
        this.requestBody = requestBody(element, processingEnv)
    }

    private static String method(final Element element)
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

    private static String path(final String basePath, final Element element)
    {
        if (element.getAnnotation(Path) != null)
        {
            return basePath + element.getAnnotation(Path).value()
        }

        return basePath
    }

    private static ArrayList<Param> pathParams(final Element element)
    {
        return params(element, PathParam)
    }

    private static ArrayList<Param> headerParams(final Element element)
    {
        return params(element, HeaderParam)
    }

    private static ArrayList<Param> params(final Element element, final Class annotationClass)
    {
        def params = new ArrayList<Param>()
        def m = element as ExecutableElement
        m.parameters.each {
            if (it.getAnnotation(annotationClass) != null)
            {
                //noinspection GroovyAssignabilityCheck
                params.add(new Param(it.getAnnotation(annotationClass).value(), it.asType().toString()))
            }
        }

        return params
    }

    private static String requestBody(final Element element, final ProcessingEnvironment processingEnv)
    {
        def m = element as ExecutableElement

        def body = m.parameters.find {
            it.annotationMirrors.isEmpty()
        }

        if (body)  {
            return body.simpleName.toString()
        }

        return null
    }
}
