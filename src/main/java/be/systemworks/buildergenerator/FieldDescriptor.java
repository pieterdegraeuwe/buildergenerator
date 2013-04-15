package be.systemworks.buildergenerator;

public class FieldDescriptor {
    String fieldVarName;
    String fieldNameCapitalized;
    String fieldType;

    public FieldDescriptor(String fieldVarName, String fieldNameCapitalized, String fieldType) {
        this.fieldVarName = fieldVarName;
        this.fieldNameCapitalized = fieldNameCapitalized;
        this.fieldType = fieldType;
    }

    public String getFieldVarName() {
        return fieldVarName;
    }

    public String getFieldNameCapitalized() {
        return fieldNameCapitalized;
    }

    public String getFieldType() {
        return fieldType;
    }
}
