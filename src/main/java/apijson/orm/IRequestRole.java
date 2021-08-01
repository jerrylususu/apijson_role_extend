package apijson.orm;

import apijson.RequestRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface IRequestRole {

    public static final List<Class> classes = new ArrayList<>();
    public static final List<IRequestRole> values = new ArrayList<>();

    static <T extends Enum<T> & IRequestRole> void register(Class<T> clazz) {
        classes.add(clazz);
        values.addAll(Arrays.asList(clazz.getEnumConstants()));
        System.out.println(values);
    }

    static IRequestRole get(String name){
        if (name == null){
            return RequestRole.UNKNOWN;
        }
        for (Class clazz:classes){
            try {
                Enum anEnum = Enum.valueOf(clazz, name);
                return (IRequestRole) anEnum;
            } catch (IllegalArgumentException | NullPointerException e){
                // do nothing
                // IllegalArgumentException: name not in this enumType
                // NullPointerException: enumType or name is null
            }
        }
        return null;
    }
}
