package me.mywiki.configurator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractElementVisitor8;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;


@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
    "me.mywiki.configurator.GenerateBuilder"
 })
public class AptConfigurator extends AbstractProcessor{
    public AptConfigurator() {
        System.err.println("AptConfigurator WAS INSTANTIATED");
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(AutoGenerateBuilder.class)) {
                if (annotatedElement.getKind() != ElementKind.INTERFACE) {
                    reportError( annotatedElement,
                                 "Only classes can be annotated with @%s",
                                 AutoGenerateBuilder.class.getSimpleName());
                }
                else {
                    String packageName= packageNameOf(annotatedElement);
                    String readOnlyInterfaceName= annotatedElement.getSimpleName().toString();
                    String builderInterfaceName= packageName + "." +annotatedElement + "$Builder";
                    JavaFileObject javaFile= super.processingEnv.getFiler()
                                                                 .createSourceFile( packageName + "." + builderInterfaceName);
                    try(Writer writer= javaFile.openWriter() ) {
                        processAnnotatedinterface(annotatedElement, writer, packageName, readOnlyInterfaceName, builderInterfaceName);
                    }
                }
            }
        }
        catch(Exception ex) {
            if (ex instanceof RuntimeException ) { throw (RuntimeException) ex; }
            else                                 { throw new RuntimeException(ex); }
        }
        return true;
    }
     
    private String packageNameOf(Element annotatedElement) {
        // TODO Auto-generated method stub
        return null;
    }

    private void processAnnotatedinterface( Element annotatedElement, 
                                            Writer os, 
                                            String packageName, 
                                            String readOnlyInterfaceName, 
                                            String builderInterfaceName) throws IOException 
    {
        if (isTopLevel(annotatedElement)) 
        {
                final  Map<String, TypeMirror> methodsToReturnType = new HashMap<>();
                for (Element inner: annotatedElement.getEnclosedElements()) {
                    if (inner.getKind() == ElementKind.METHOD) {
                        inner.accept(new AbstractElementVisitor8<Void,Void>() {
                            @Override
                            public Void visitPackage(PackageElement e, Void p) { return null; }
                            
                            @Override
                            public Void visitType(TypeElement e, Void p) { return null; }
                            
                            @Override
                            public Void visitVariable(VariableElement e, Void p) { return null; }
                            
                            @Override
                            public Void visitTypeParameter(TypeParameterElement e, Void p) { return null; }
                        
                            public Void visitExecutable(javax.lang.model.element.ExecutableElement e, Void p) {
                                Name name= e.getSimpleName();
                                if (  e.getParameters().size() !=  0
                                       || name.contentEquals("equals")
                                       || name.contentEquals("hashCode")
                                       || name.contentEquals("cloneBuilder") )
                                {   
                                    return null;
                                }
                                methodsToReturnType.put(name.toString(), e.getReturnType());
                                return null;
                            };
                        }, null);
                    }
                }
                

            os.write("package " + packageName+";\n");
            os.write("\n"); 
            os.write("public interface " + builderInterfaceName +" { \n");
            os.write(" " + readOnlyInterfaceName +" done(); \n");
            for ( Entry<String, TypeMirror> m: methodsToReturnType.entrySet() ) {
                os.write("\n");
                String valueTypeName= fullyQualifiedNameOf(m.getValue());
                os .write( " " + builderInterfaceName + " " + m.getKey() + "( "+valueTypeName+");\n");
            }
            os.write("}\n");
        }
        else{
            throw new UnsupportedOperationException("Processing inner classes not implemented yet");
        }
        
    }

    private String fullyQualifiedNameOf(TypeMirror value) {
        {
            if (value.getKind()== TypeKind.UNION) {
                
            }
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private boolean isTopLevel(Element annotatedElement) throws IOException {
        Element enclosing= annotatedElement.getEnclosingElement();
        return (enclosing != null) && (enclosing.getKind() == ElementKind.PACKAGE);
    }

    @Override
    public Set<String> getSupportedOptions() {
        // TODO Auto-generated method stub
        return super.getSupportedOptions();
    }
    
    private void reportError(Element annotatedElement, String msg, String ... args) {
        processingEnv.getMessager().printMessage(
                Kind.WARNING, String.format(msg, args), annotatedElement);
    }

}
