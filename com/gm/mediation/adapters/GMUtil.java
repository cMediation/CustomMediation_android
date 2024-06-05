package com.gm.mediation.adapters;

import android.content.Context;

import com.bykv.vk.openvk.api.proto.Bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GMUtil {

    public static int dip2px(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue * scale + 0.5f);

    }

    public static double getBestPrice(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        double cpm = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object newmbAdObject = field.get(object);
                if (newmbAdObject != null) {
                    double findCpm = getBestPriceInCache(newmbAdObject);
                    if (findCpm > cpm) {
                        cpm = findCpm;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return cpm / 100.0;

    }

    private static double getBestPriceInCache(Object mbADObject) {
        try {
            if (mbADObject != null) {
                List coreObjectList = getMBCoreObject(mbADObject);
                if (coreObjectList == null || coreObjectList.size() == 0) {
                    return 0;
                }

                for (Object coreObject : coreObjectList) {
                    if (coreObject != null) {
                        Class tempClass = coreObject.getClass();
                        Class targetClass = null;
                        while (tempClass != null && !tempClass.getName().equals("java.lang.Object")) {
                            if (tempClass.getSuperclass().getName().equals("java.lang.Object")) {
                                targetClass = tempClass;
                            }
                            tempClass = tempClass.getSuperclass();
                        }

                        if (targetClass != null) {
                            double price = 0;
                            price = fetchBestPriceByCacheList(targetClass, coreObject);
                            if (price > 0) {
                                return price;
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return 0.0;
    }

    private static List<Object> getMBCoreObject(Object mbADObject) {
        List<Object> coreObjectList = new ArrayList<>();
        try {
            Class tempClass = mbADObject.getClass();
            Field[] fields = tempClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object object = field.get(mbADObject);
                if (object == null) {
                    continue;
                }
                coreObjectList.add(object);
                Class fieldObjectClass = object.getClass();
                Field[] objectClassFields = fieldObjectClass.getDeclaredFields();
                for (Field nestField : objectClassFields) {
                    nestField.setAccessible(true);
                    Object nestObject = nestField.get(object);
                    coreObjectList.add(nestObject);
                }
            }
            return coreObjectList;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return coreObjectList;
    }

    private static double fetchBestPriceByCacheList(Class targetClass, Object coreObject) {
        double bestPrice = 0;
        try {
            Class tempClass = coreObject.getClass().getSuperclass();
            while (!(tempClass instanceof Object)) {
                tempClass = tempClass.getSuperclass();
            }
        } catch (Throwable e) {

        }

        try {
            Field[] fields = targetClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object object = field.get(coreObject);
                if (object instanceof List<?>) {
                    if (object != null && (((List<?>) object).size()) > 0) {
                        for (Object listObject : ((List<?>) object)) {
                            double currentPrice = findEcpm(listObject);
                            if (currentPrice > bestPrice) {
                                bestPrice = currentPrice;
                            }

                        }

                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return bestPrice;
    }

    private static double findEcpm(Object bridgeListObject) {
        double ecpm = 0;
        try {
            if (bridgeListObject != null) {
                Class<?> priceClass = bridgeListObject.getClass();
                Method getCpmMethod = priceClass.getMethod("getCpm");
                getCpmMethod.setAccessible(true);
                Object result = getCpmMethod.invoke(bridgeListObject);
                if (result instanceof Double) {
                    ecpm = (double) result;
                    if (ecpm > 0) {
                        return ecpm;
                    }
                }
            }
        } catch (Throwable e) {
        }

        if (bridgeListObject instanceof Bridge) {
            ArrayList<Field> fieldArrayList = new ArrayList<>();
            fillObjectField(bridgeListObject.getClass(), fieldArrayList);
            for (Field field : fieldArrayList) {
                if (field.getType() == double.class) {
                    try {
                        field.setAccessible(true);
                        double tempEcpm = (double) field.get(bridgeListObject);
                        if (ecpm <= tempEcpm) {
                            ecpm = tempEcpm;
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return ecpm;
    }

    private static void fillObjectField(Class objectClass, List<Field> fieldList) {
        try {
            if (objectClass == null || objectClass.getName().equals(Object.class.getName())) {
                return;
            }
            Field[] field = objectClass.getDeclaredFields();
            if (field != null && field.length > 0) {
                fieldList.addAll(Arrays.asList(field));
            }

            fillObjectField(objectClass.getSuperclass(), fieldList);

        } catch (Throwable e) {

        }
    }


}
