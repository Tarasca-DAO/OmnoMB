package concept.utility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class JsonFunction {

    public static void put(JSONObject jsonObject, String key, Object object) {

        if (jsonObject == null || object == null || key == null || key.length() == 0) {
            return;
        }

        jsonObject.put(key, object);
    }

    public static void add(JSONArray jsonArray, Object object) {

        if (jsonArray == null || object == null) {
            return;
        }

        jsonArray.add(object);
    }

    public static JSONObject getJSONObject(JSONObject jsonObject, String key, JSONObject defaultValue) {
        JSONObject result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONObject) {
                    result = (JSONObject) o;
                }
            }
        }

        return result;
    }

    public static JSONArray getJSONArray(JSONObject jsonObject, String key, JSONArray defaultValue) {
        JSONArray result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {
                    result = (JSONArray) o;
                }
            }
        }

        return result;
    }

    public static List<JSONObject> getListJSONObject(JSONObject jsonObject, String key, List<JSONObject> defaultValue) {
        List<JSONObject> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {

                    result = new ArrayList<>();
                    JSONArray jsonArray = (JSONArray) o;

                    for (Object object: jsonArray) {
                        if (object instanceof JSONObject) {
                            result.add((JSONObject) object);
                        }
                    }
                }
            }
        }

        return result;
    }


    public static String getString(JSONObject jsonObject, String key, String defaultValue) {
        String result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof String && ((String) o).length() != 0) {
                    result = (String) o;
                }
            }
        }

        return result;
    }

    public static boolean getBoolean(JSONObject jsonObject, String key, boolean defaultValue) {
        boolean result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof Boolean) {
                    result = (Boolean) o;
                }
            }
        }

        return result;
    }

    public static int getInt(JSONObject jsonObject, String key, int defaultValue) {

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof Integer) {
                    return (int) o;
                } else if (o instanceof Long) {
                    return (int) (long) o;
                } else if (o instanceof String) {
                    try {
                        return Integer.parseInt((String) o);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
            }
        }

        return defaultValue;
    }

    public static Long getLong(JSONObject jsonObject, String key, long defaultValue) {

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                Object o = jsonObject.get(key);

                if (o instanceof Integer) {
                    return  (long) (int) o;
                } else if (o instanceof Long) {
                    return (long) o;
                } else if (o instanceof String) {
                    try {
                        return Long.parseLong((String) o);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
            }
        }

        return defaultValue;
    }

    public static double getDoubleFromString(JSONObject jsonObject, String key, double defaultValue) {
        double result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                Object o = jsonObject.get(key);

                if (o instanceof String && ((String) o).length() != 0) {
                    result = Double.parseDouble((String) o);
                }
            }
        }

        return result;
    }

    public static long getLongFromStringUnsigned(JSONObject jsonObject, String key, long defaultValue) {
        long result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                Object o = jsonObject.get(key);
                if (o instanceof String && ((String) o).length() != 0) {
                    try {
                        result = Long.parseUnsignedLong((String) o);
                    } catch (Exception e) { /* empty */ }
                }
            }
        }

        return result;
    }

    public static HashMap<Long, Long> getHashMapLongLongFromUnsignedStringKeyValuePairs(JSONObject jsonObject, String key, HashMap<Long, Long> defaultValue) {
        HashMap<Long, Long> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                try {
                    result = new HashMap<>();
                    JSONObject object = (JSONObject) jsonObject.get(key);
                    int count = object.size();

                    List<String> listKey = new ArrayList<String>(object.keySet()); // JSON library implementation specific?
                    List<String> listValue = new ArrayList<String>(object.values());

                    for (int i = 0; i < count; i++) {
                        result.put(Long.parseUnsignedLong(listKey.get(i)), Long.parseUnsignedLong(listValue.get(i)));
                    }
                } catch (Exception e) {
                    result = defaultValue;
                }
            }
        }

        return result;
    }

    public static HashMap<Long, Double> getHashMapLongDoubleFromUnsignedStringKeyValuePairs(JSONObject jsonObject, String key, HashMap<Long, Double> defaultValue) {
        HashMap<Long, Double> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                try {
                    result = new HashMap<>();
                    JSONObject object = (JSONObject) jsonObject.get(key);
                    int count = object.size();

                    List<String> listKey = new ArrayList<String>(object.keySet());
                    List<String> listValue = new ArrayList<String>(object.values());

                    for (int i = 0; i < count; i++) {
                        result.put(Long.parseUnsignedLong(listKey.get(i)), Double.parseDouble(listValue.get(i)));
                    }
                } catch (Exception e) {
                    result = defaultValue;
                }
            }
        }

        return result;
    }

    public static HashMap<Integer, Integer> getHashMapIntIntFromKeyValuePairs(JSONObject jsonObject, String key, HashMap<Integer, Integer> defaultValue) {
        HashMap<Integer, Integer> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                try {
                    result = new HashMap<>();
                    JSONObject object = (JSONObject) jsonObject.get(key);
                    int count = object.size();

                    List<String> listKey = new ArrayList<String>(object.keySet()); // JSON library implementation specific?
                    List<Integer> listValue = new ArrayList<Integer>(object.values());

                    for (int i = 0; i < count; i++) {
                        result.put(Integer.parseInt(listKey.get(i)), listValue.get(i));
                    }
                } catch (Exception e) {
                    result = defaultValue;
                }
            }
        }

        return result;
    }

    public static SortedSet<Long> setOfLongFromJsonStringArrayUnsigned(JSONArray jsonStringArray) {

        if ( jsonStringArray == null || jsonStringArray.size() == 0) {
            return null;
        }

        SortedSet<Long> list = new TreeSet<>();

        for (Object o: jsonStringArray) {
            if (o instanceof String && ((String) o).length() != 0) {
                list.add(Long.parseUnsignedLong(o.toString()));
            }
        }

        return list;
    }

    public static List<Long> listLongFromJsonStringArrayUnsigned(JSONArray jsonStringArray) {

        List<Long> list = new ArrayList<>();

        if (jsonStringArray == null || jsonStringArray.size() == 0) {
            return null;
        }

        for (Object o: jsonStringArray) {
            if (o instanceof String && ((String) o).length() != 0) {
                list.add(Long.parseUnsignedLong((String) o));
            }
        }

        return list;
    }

    public static List<Long> getListLongFromJsonArrayStringUnsigned(JSONObject jsonObject, String key, List<Long> defaultValue) {

        List<Long> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {
                    result = listLongFromJsonStringArrayUnsigned((JSONArray) o);
                }
            }
        }

        return result;
    }

    public static HashSet<byte[]> getHashSetBytesFromJsonArrayStringHexadecimal(JSONObject jsonObject, String key, HashSet<byte[]> defaultValue) {

        HashSet<byte[]> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {

                    result = new HashSet<>();

                    for (Object o1: (JSONArray)o) {

                        if (!(o1 instanceof String)) {
                            continue;
                        }

                        String string = (String) o1;

                        if (string.length() != 0) {
                            try {
                                result.add(bytesFromHexString(string));
                            } catch (Exception e) {
                                return defaultValue;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static HashSet<String> getHashSetStringFromJsonArray(JSONObject jsonObject, String key, HashSet<String> defaultValue) {

        HashSet<String> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {

                    result = new HashSet<>();

                    for (Object o1: (JSONArray)o) {

                        if (!(o1 instanceof String)) {
                            continue;
                        }

                        String string = (String) o1;

                        if (string.length() != 0) {
                            try {
                                result.add(string);
                            } catch (Exception e) {
                                return defaultValue;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static List<Integer> listIntegerFromJsonArray(JSONArray jsonArray) {

        List<Integer> list = new ArrayList<>();

        if ( jsonArray == null || jsonArray.size() == 0) {
            return list;
        }

        for (Object o: jsonArray) {
            if (o instanceof Integer) {
                list.add((int) o);
            } else if (o instanceof Long) {
                list.add((int) (long) o);
            }
        }

        return list;
    }

    public static List<Integer> getListIntegerFromJsonArray(JSONObject jsonObject, String key, List<Integer> defaultValue) {

        List<Integer> result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {

                Object o = jsonObject.get(key);

                if (o instanceof JSONArray) {
                    result = listIntegerFromJsonArray((JSONArray) o);
                }
            }
        }

        return result;
    }

    public static JSONArray jsonArrayFromArrayInt(int[] array) {
        JSONArray jsonArray = new JSONArray();

        if (array == null || array.length == 0) {
            return jsonArray;
        }

        for (int value: array) {
            jsonArray.add(value);
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayFromArrayLong(long[] array) {
        JSONArray jsonArray = new JSONArray();

        if (array == null || array.length == 0) {
            return jsonArray;
        }

        for (long value: array) {
            jsonArray.add(value);
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayStringUnsignedFromListLong(List<Long> list) {
        JSONArray jsonArray = new JSONArray();

        if (list == null || list.size() == 0) {
            return jsonArray;
        }

        for (long value: list) {
            jsonArray.add(Long.toUnsignedString(value));
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayStringHexadecimalFromByteHashSet(HashSet<byte[]> list) {
        JSONArray jsonArray = new JSONArray();

        if (list == null || list.size() == 0) {
            return jsonArray;
        }

        for (byte[] value: list) {
            if (value != null && value.length != 0) {
                jsonArray.add(hexStringFromBytes(value));
            }
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayFromStringHashSet(HashSet<String> list) {
        JSONArray jsonArray = new JSONArray();

        if (list == null || list.size() == 0) {
            return jsonArray;
        }

        for (String value: list) {
            if (value != null) {
                jsonArray.add(value);
            }
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayFromTreeMapIntegerLong(TreeMap<Integer, Long> treeMap){
        JSONArray jsonArray = new JSONArray();

        if (treeMap == null) {
            return jsonArray;
        }

        List<Integer> listKeys = new ArrayList<>(treeMap.keySet());
        List<Long> listValues = new ArrayList<>(treeMap.values());

        int count = listKeys.size();

        for (int i = 0; i < count; i++) {
            jsonArray.add(Long.toUnsignedString(listKeys.get(i)));
            jsonArray.add(Long.toUnsignedString(listValues.get(i)));
        }

        return jsonArray;
    }


    public static JSONObject jsonObjectStringUnsignedLongPairsFromMap(Map<Long, Long> map, boolean includeZeroValue) {
        JSONObject jsonObject = new JSONObject();

        if (map == null || map.size() == 0) {
            return jsonObject;
        }

        try {

            List<Long> listKey = new ArrayList<>(map.keySet());
            List<Long> listValue = new ArrayList<>(map.values());

            int count = listKey.size();

            for (int i = 0; i < count; i++) {
                long key = listKey.get(i);
                long value = listValue.get(i);

                if (includeZeroValue || value != 0) {
                    jsonObject.put(Long.toUnsignedString(key), Long.toUnsignedString(value));
                }
            }

        } catch (Exception e) {
            jsonObject = new JSONObject();
        }

        return jsonObject;
    }

    public static JSONObject jsonObjectStringUnsignedLongDoublePairsFromMap(Map<Long, Double> map, boolean includeZeroValue) {
        JSONObject jsonObject = new JSONObject();

        if (map == null || map.size() == 0) {
            return jsonObject;
        }

        try {

            List<Long> listKey = new ArrayList<>(map.keySet());
            List<Double> listValue = new ArrayList<>(map.values());

            int count = listKey.size();

            for (int i = 0; i < count; i++) {
                long key = listKey.get(i);
                double value = listValue.get(i);

                if (includeZeroValue || value != 0) {
                    jsonObject.put(Long.toUnsignedString(key), Double.toString(value));
                }
            }

        } catch (Exception e) {
            jsonObject = new JSONObject();
        }

        return jsonObject;
    }

    public static JSONObject jsonObjectKeyValuePairsFromMapIntInt(Map<Integer, Integer> map) {
        JSONObject jsonObject = new JSONObject();

        if (map == null || map.size() == 0) {
            return jsonObject;
        }

        try {

            List<Integer> listKey = new ArrayList<>(map.keySet());
            List<Integer> listValue = new ArrayList<>(map.values());

            int count = listKey.size();

            for (int i = 0; i < count; i++) {
                jsonObject.put(Integer.toString(listKey.get(i)), listValue.get(i));
            }
        } catch (Exception e) {
            jsonObject = new JSONObject();
        }

        return jsonObject;
    }

    public static JSONArray jsonArrayFromTreeMapLongLong(TreeMap<Long, Long> map){
        JSONArray jsonArray = new JSONArray();

        if (map == null || map.size() == 0) {
            return jsonArray;
        }

        try {

            List<Long> listKeys = new ArrayList<>(map.keySet());
            List<Long> listValues = new ArrayList<>(map.values());

            int count = listKeys.size();

            for (int i = 0; i < count; i++) {
                jsonArray.add(Long.toUnsignedString(listKeys.get(i)));
                jsonArray.add(Long.toUnsignedString(listValues.get(i)));
            }
        } catch (Exception e) {
            jsonArray = new JSONArray();
        }

        return jsonArray;
    }

    public static JSONArray jsonArrayFromListInteger(List<Integer> list){
        JSONArray jsonArray = new JSONArray();

        if (list == null || list.size() == 0) {
            return jsonArray;
        }

        jsonArray.addAll(list);

        return jsonArray;
    }

    public static JSONArray jsonArrayStringsFromListLongUnsigned(List<Long> list){
        JSONArray jsonArray = new JSONArray();

        if (list == null) {
            return jsonArray;
        }

        for (Long value: list) {
            jsonArray.add(Long.toUnsignedString(value));
        }

        return jsonArray;
    }

    public static TreeMap<Long, Long> treeMapLongLongFromJsonArrayStringPairsUnsigned(JSONArray jsonArray) {
        TreeMap<Long, Long> treeMap = new TreeMap<>();

        if (jsonArray == null || jsonArray.size() == 0) {
            return  treeMap;
        }

        try {

            int count = jsonArray.size();

            for (int i = 0; i < count; i += 2) {
                long key = Long.parseUnsignedLong((String) jsonArray.get(i));
                long value = Long.parseUnsignedLong((String) jsonArray.get(i + 1));
                treeMap.put(key, value);
            }
        } catch (Exception e) {
            treeMap = new TreeMap<>();
        }

        return treeMap;
    }

    public static TreeMap<Integer, Long> treeMapIntegerLongFromJsonArrayStringPairsUnsigned(JSONArray jsonArray) {
        TreeMap<Integer, Long> treeMap = new TreeMap<>();

        if (jsonArray == null || jsonArray.size() == 0) {
            return  treeMap;
        }

        int count = jsonArray.size();

        try {

            for (int i = 0; i < count; i += 2) {
                long key = Long.parseUnsignedLong((String) jsonArray.get(i));
                long value = Long.parseUnsignedLong((String) jsonArray.get(i + 1));
                treeMap.put((int) key, value);
            }
        } catch (Exception e) {
            treeMap = new TreeMap<>();
        }

        return treeMap;
    }

    public static byte[] getBytesFromHexString(JSONObject jsonObject, String key, byte[] defaultValue) {
        byte[] result = defaultValue;

        if (jsonObject != null) {
            if (jsonObject.containsKey(key)) {
                result = bytesFromHexString((String) jsonObject.get(key));
            }
        }

        return result;
    }

    public static HashMap<Long, Long> hashMapFromList(List<Long> list) {
        HashMap<Long, Long> result = new HashMap<>();

        if (list == null || list.size() == 0 || list.size() < 2 || list.size() % 2 == 1) {
            return result;
        }

        int count = list.size();

        for (int index = 0; index < count; index += 2) {
            long key = list.get(index);
            long value = list.get(index + 1);
            result.put(key, value);
        }

        return result;
    }

    public static byte[] bytesFromHexString(String s) {

        if (s == null || s.length() < 2) {
            return null;
        }

        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    public static String hexStringFromBytes(byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            return null;
        }

        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }
}