package me.noxerek.scuti.configuration.io;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import me.noxerek.scuti.configuration.Configuration;
import me.noxerek.scuti.configuration.option.obfuscation.*;
import me.noxerek.scuti.configuration.option.obfuscation.StringEncryption.EncryptionType;
import me.noxerek.scuti.configuration.option.shrinking.UnusedMembers;
import me.noxerek.scuti.dictionary.Types;
import me.noxerek.scuti.transform.Transformer;
import me.noxerek.scuti.transform.Transformers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author netindev
 */
public class Parser implements Configuration {

    File input, output;
    Set<Class<? extends Transformer>> transformers;
    List<File> dependencies;

    boolean corruptNames, corruptCRC32;

    /* transformers */
    ClassEncrypt classEncrypt;
    RenameMembers renameMembers;
    MiscellaneousObfuscation miscellaneousObfuscation;
    NumberObfuscation numberObfuscation;
    StringEncryption stringEncryption;

    UnusedMembers unusedMembers;

    /* init */ {
        this.input = null;
        this.output = null;

        this.transformers = new HashSet<>();
        this.dependencies = new ArrayList<>();

        this.classEncrypt = new ClassEncrypt();
        this.miscellaneousObfuscation = new MiscellaneousObfuscation();
        this.numberObfuscation = new NumberObfuscation();
        this.renameMembers = new RenameMembers();
        this.stringEncryption = new StringEncryption();

        this.unusedMembers = new UnusedMembers();
    }

    public Parser(final File file) throws Exception {
        this.parse(file);
    }

    void parse(final File file) throws Exception {
        try {
            JsonObject.readFrom(new FileReader(file)).forEach(generalMember -> {
                JsonObject jsonObject = null;
                if (generalMember.getValue().isObject()) {
                    jsonObject = generalMember.getValue().asObject();
                }
                switch (generalMember.getName()) {
                    case "input":
                        this.input = new File(generalMember.getValue().asString());
                        return;
                    case "output":
                        this.output = new File(generalMember.getValue().asString());
                        return;
                    case "libraries":
                        final JsonArray jsonArray = generalMember.getValue().asArray();
                        jsonArray.forEach(value -> this.dependencies.add(new File(value.asString())));
                        return;
                    case "corrupt_output_stream":
                        this.corruptCRC32 = generalMember.getValue().asBoolean();
                        return;
                    case "corrupt_class_names":
                        this.corruptNames = generalMember.getValue().asBoolean();
                        return;
                    case "shrinking":
                        assert jsonObject != null;
                        jsonObject.forEach(shrinkingTable -> {
                            switch (shrinkingTable.getName()) {
                                case "unused_members":
                                    shrinkingTable.getValue().asObject().forEach(unusedTable -> {
                                        if (unusedTable.getName().equals("enable_transformer")
                                                && unusedTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Shrinking.UNUSED_MEMBER_TRANSFORMER);
                                        } else if (unusedTable.getName().equals("remove_classes")) {
                                            this.unusedMembers.setClasses(unusedTable.getValue().asBoolean());
                                        } else if (unusedTable.getName().equals("remove_methods")) {
                                            this.unusedMembers.setMethods(unusedTable.getValue().asBoolean());
                                        } else if (unusedTable.getName().equals("remove_fields")) {
                                            this.unusedMembers.setFields(unusedTable.getValue().asBoolean());
                                        } else if (unusedTable.getName().equals("keep_classes")) {
                                            this.unusedMembers.setKeepClasses(unusedTable.getValue().asArray().values().stream()
                                                    .map(JsonValue::asString).collect(Collectors.toList()));
                                        }
                                    });
                                    break;
                                case "inner_class":
                                    shrinkingTable.getValue().asObject().forEach(unusedTable -> {
                                        if (unusedTable.getName().equals("enable_transformer")
                                                && unusedTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Shrinking.INNER_CLASS_TRANSFORMER);
                                        }
                                    });
                                    break;
                            }
                        });
                    case "optimization":
                        assert jsonObject != null;
                        jsonObject.forEach(optimizationTable -> {
                            switch (optimizationTable.getName()) {
                                case "dead_code":
                                    optimizationTable.getValue().asObject().forEach(nopTable -> {
                                        if (nopTable.getName().equals("enable_transformer") && nopTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Optimization.DEAD_CODE_TRANSFORMER);
                                        }
                                    });
                                case "no_operation":
                                    optimizationTable.getValue().asObject().forEach(nopTable -> {
                                        if (nopTable.getName().equals("enable_transformer") && nopTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Optimization.NO_OPERATION_TRANSFORMER);
                                        }
                                    });
                            }
                        });
                        break;
                    case "obfuscation":
                        assert jsonObject != null;
                        jsonObject.forEach(obfuscationTable -> {
                            switch (obfuscationTable.getName()) {
                                case "class_encrypt":
                                    obfuscationTable.getValue().asObject().forEach(encryptTable -> {
                                        if (encryptTable.getName().equals("enable_transformer")
                                                && encryptTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.CLASS_ENCRYPT_TRANSFORMER);
                                        } else if (encryptTable.getName().equals("loader_name")) {
                                            this.classEncrypt.setLoaderName(encryptTable.getValue().asString());
                                        } else if (encryptTable.getName().equals("main_class")) {
                                            this.classEncrypt.setMainClass(encryptTable.getValue().asString());
                                        } else if (encryptTable.getName().equals("string_key")) {
                                            this.classEncrypt.setStringKey(encryptTable.getValue().asInt());
                                        } else if (encryptTable.getName().equals("class_key")) {
                                            this.classEncrypt.setClassKey(encryptTable.getValue().asInt());
                                        }
                                    });
                                    break;
                                case "control_flow":
                                    obfuscationTable.getValue().asObject().forEach(flowTable -> {
                                        if (flowTable.getName().equals("enable_transformer") && flowTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.CONTROL_FLOW_TRANSFORMER);
                                        }
                                    });
                                    break;
                                case "hide_code":
                                    obfuscationTable.getValue().asObject().forEach(hideTable -> {
                                        if (hideTable.getName().equals("enable_transformer") && hideTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.HIDE_CODE_TRANSFORMER);
                                        }
                                    });
                                    break;
                                case "invoke_dynamic":
                                    obfuscationTable.getValue().asObject().forEach(indyTable -> {
                                        if (indyTable.getName().equals("enable_transformer") && indyTable.getValue().asBoolean()) {
                                            //this.transformers.add(Transformers.Obfuscation.INVOKE_DYNAMIC_TRANSFORMER);
                                        }
                                    });
                                    break;
                                case "miscellaneous":
                                    obfuscationTable.getValue().asObject().forEach(miscellaneousTable -> {
                                        switch (miscellaneousTable.getName()) {
                                            case "enable_transformer":
                                                if (miscellaneousTable.getValue().asBoolean())
                                                    this.transformers.add(Transformers.Obfuscation.MISCELLANEOUS_OBFUSCATION_TRANSFORMER);
                                                break;

                                            case "massive_signature":
                                                this.miscellaneousObfuscation
                                                        .setMassiveSignature(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "massive_source":
                                                this.miscellaneousObfuscation
                                                        .setMassiveSource(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "push_transient":
                                                this.miscellaneousObfuscation
                                                        .setPushTransient(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "push_varargs":
                                                this.miscellaneousObfuscation.setPushVarargs(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "variable_descriptor":
                                                this.miscellaneousObfuscation
                                                        .setVariableDescritor(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "variable_descriptor_list":
                                                this.miscellaneousObfuscation
                                                        .setVariableDescriptorList(miscellaneousTable.getValue().asArray().values()
                                                                .stream().map(JsonValue::asString).collect(Collectors.toList()));
                                                break;

                                            case "invalid_annotation":
                                                this.miscellaneousObfuscation
                                                        .setInvalidAnnotation(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "duplicate_variables":
                                                this.miscellaneousObfuscation
                                                        .setDuplicateVariables(miscellaneousTable.getValue().asBoolean());
                                                break;

                                            case "random_exceptions":
                                                this.miscellaneousObfuscation
                                                        .setRandomExceptions(miscellaneousTable.getValue().asBoolean());
                                                break;

                                        }
                                    });
                                    break;
                                case "number_obfuscation":
                                    obfuscationTable.getValue().asObject().forEach(numberTable -> {
                                        if (numberTable.getName().equals("enable_transformer")
                                                && numberTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.NUMBER_OBFUSCATION_TRANSFORMER);
                                        } else if (numberTable.getName().equals("execute_twice")) {
                                            this.numberObfuscation.setExecuteTwice(numberTable.getValue().asBoolean());
                                        }
                                    });
                                    break;
                                case "rename_members":
                                    obfuscationTable.getValue().asObject().forEach(renamerTable -> {
                                        switch (renamerTable.getName()) {
                                            case "enable_transformer":
                                                if (renamerTable.getValue().asBoolean())
                                                    this.transformers.add(Transformers.Obfuscation.RENAME_MEMBERS_TRANSFORMER);
                                                break;

                                            case "rename_classes":
                                                this.renameMembers.setRenameClasses(renamerTable.getValue().asBoolean());
                                                break;

                                            case "rename_methods":
                                                this.renameMembers.setRenameMethods(renamerTable.getValue().asBoolean());
                                                break;

                                            case "rename_fields":
                                                this.renameMembers.setRenameFields(renamerTable.getValue().asBoolean());
                                                break;

                                            case "remove_packages":
                                                this.renameMembers.setRemovePackages(renamerTable.getValue().asBoolean());
                                                break;

                                            case "internal_dictionary":
                                                for (final Types types : Types.values()) {
                                                    if (renamerTable.getValue().asString().toUpperCase().equals(types.toString())) {
                                                        this.renameMembers.setRandomize(types);
                                                    }
                                                }
                                                break;

                                            case "keep_classes":
                                                this.renameMembers.setExcludeClasses(renamerTable.getValue().asArray().values().stream()
                                                        .map(JsonValue::asString).collect(Collectors.toList()));
                                                break;

                                            case "keep_methods":
                                                this.renameMembers.setExcludeMethods(renamerTable.getValue().asArray().values().stream()
                                                        .map(JsonValue::asString).collect(Collectors.toList()));
                                                break;

                                            case "keep_fields":
                                                this.renameMembers.setExcludeFields(renamerTable.getValue().asArray().values().stream()
                                                        .map(JsonValue::asString).collect(Collectors.toList()));
                                                break;

                                            case "packages_dictionary":
                                                this.renameMembers.setPackagesDictionary(new File(renamerTable.getValue().asString()));
                                                break;

                                            case "classes_dictionary":
                                                this.renameMembers.setClassesDictionary(new File(renamerTable.getValue().asString()));
                                                break;

                                            case "methods_dictionary":
                                                this.renameMembers.setMethodsDictionary(new File(renamerTable.getValue().asString()));
                                                break;

                                            case "fields_dictionary":
                                                this.renameMembers.setFieldsDictionary(new File(renamerTable.getValue().asString()));
                                                break;
                                        }
                                    });
                                    break;
                                case "shuffle_members":
                                    obfuscationTable.getValue().asObject().forEach(shuffleMemberTable -> {
                                        if (shuffleMemberTable.getName().equals("enable_transformer")
                                                && shuffleMemberTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.SHUFFLE_MEMBERS_TRANSFORMER);
                                        }
                                    });
                                    break;
                                case "string_encryption":
                                    obfuscationTable.getValue().asObject().forEach(stringTable -> {
                                        if (stringTable.getName().equals("enable_transformer")
                                                && stringTable.getValue().asBoolean()) {
                                            this.transformers.add(Transformers.Obfuscation.STRING_ENCRYPTION_TRANSFORMER);
                                        } else if (stringTable.getName().equals("type")) {
                                            for (final EncryptionType type : EncryptionType.values()) {
                                                if (stringTable.getValue().asString().toUpperCase().equals(type.toString())) {
                                                    this.stringEncryption.setEncryptionType(type);
                                                }
                                            }
                                        }
                                    });
                                    break;
                            }

                        });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        @Override
    public File getInput() {
        return this.input;
    }

    @Override
    public File getOutput() {
        return this.output;
    }

    @Override
    public List<File> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Set<Class<? extends Transformer>> getTransformers() {
        return this.transformers;
    }

    @Override
    public boolean corruptCRC32() {
        return this.corruptCRC32;
    }

    @Override
    public boolean corruptNames() {
        return this.corruptNames;
    }

    @Override
    public ClassEncrypt getClassEncrypt() {
        return this.classEncrypt;
    }

    @Override
    public RenameMembers getRenameMembers() {
        return this.renameMembers;
    }

    @Override
    public MiscellaneousObfuscation getMiscellaneousObfuscation() {
        return this.miscellaneousObfuscation;
    }

    @Override
    public NumberObfuscation getNumberObfuscation() {
        return this.numberObfuscation;
    }

    @Override
    public StringEncryption getStringEncryption() {
        return this.stringEncryption;
    }

    @Override
    public UnusedMembers getUnusedMembers() {
        return this.unusedMembers;
    }

}
