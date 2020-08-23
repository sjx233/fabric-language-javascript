package io.github.sjx233.fabricjs;

import java.lang.reflect.Field;
import java.util.function.Predicate;

public enum Util {
  ;
  public static String escape(String str) {
    return str
      .replace("\\", "\\\\")
      .replace("\'", "\\'")
      .replace("\n", "\\n")
      .replace("\r", "\\r");
  }

  public static <T> T findInArray(T[] arr, Predicate<? super T> pred) {
    for (T obj : arr)
      if (pred.test(obj)) return obj;
    return null;
  }

  public static Field findField(Object obj, String name) throws NoSuchFieldException {
    Field field = obj.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  public static Object getFieldValue(Object obj, String name) throws NoSuchFieldException, IllegalAccessException {
    return findField(obj, name).get(obj);
  }

  public static void setFieldValue(Object obj, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
    findField(obj, name).set(obj, value);
  }
}
