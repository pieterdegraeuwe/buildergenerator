package be.systemworks.buildergenerator;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Entity;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class EntityBuilderGeneratorAptProcessor extends AbstractProcessor {


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            Entity complexity = elem.getAnnotation(Entity.class);
            String message = "annotation found in " + elem.getSimpleName()
                    + " with annotation entity";

            if (elem.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) elem;
                PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();

                boolean abstractClass = classElement.getModifiers().contains(Modifier.ABSTRACT);

                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.NOTE,
                        "annotated class: " + classElement.getQualifiedName(), elem);


                List<FieldDescriptor> fields = new ArrayList<FieldDescriptor>();
                readFields(classElement,fields);

                String fqClassName = classElement.getQualifiedName().toString();
                String className = classElement.getSimpleName().toString();
                String packageName = packageElement.getQualifiedName().toString();


                if (fqClassName != null) {

                    Properties props = new Properties();
                    URL url = this.getClass().getClassLoader().getResource("velocity.properties");
                    try {
                        props.load(url.openStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    VelocityEngine ve = new VelocityEngine(props);
                    ve.init();

                    VelocityContext vc = new VelocityContext();

                    vc.put("className", className);
                    vc.put("packageName", packageName);
                    vc.put("fields", fields);
                    vc.put("abstract", abstractClass);
                    Template vt = ve.getTemplate("builder.vm");
                    try {
                        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                                fqClassName + "Builder",elem);

                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.NOTE,
                                "creating source file: " + jfo.toUri());

                        Writer writer = jfo.openWriter();

                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.NOTE,
                                "applying velocity template: " + vt.getName());

                        vt.merge(vc, writer);

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }



            }


            //processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
        }
        return false;
    }

    protected void readFields(TypeElement type, List<FieldDescriptor> fields) {
        // Read fields from superclass if any
        TypeMirror superClass = type.getSuperclass();
        if (TypeKind.DECLARED.equals(superClass.getKind())) {
            DeclaredType superType = (DeclaredType) superClass;
            readFields((TypeElement) superType.asElement(), fields);
        }
        for (Element child : type.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) child;
                Set modifiers = field.getModifiers();
                if (modifiers.contains(Modifier.PRIVATE) && !modifiers.contains(Modifier.STATIC)) {
                    String javaType = getFieldType(field);
                    String simpleName = field.getSimpleName().toString();
                    String fieldNameCapitalized = simpleName.substring(0,1).toUpperCase()+simpleName.substring(1);

                    if(hasMethod(type, "set"+fieldNameCapitalized)) {
                        fields.add(new FieldDescriptor(simpleName, fieldNameCapitalized, javaType));
                    }
                }
            }
        }
    }

    protected String getFieldType(VariableElement field) {
        TypeMirror fieldType = field.asType();
        return fieldType.toString();
    }

    private boolean hasMethod(TypeElement classElement, String s) {
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                Name simpleName = enclosedElement.getSimpleName();
                boolean equals = simpleName.toString().equals(s);
                if (equals) {
                    return true;
                }
            }
        }
        return false;
    }
}

