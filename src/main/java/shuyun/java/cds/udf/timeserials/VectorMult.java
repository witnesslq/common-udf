package shuyun.java.cds.udf.timeserials;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.*;

import java.util.Map;

/**
 * Created by endy on 2015/10/13.
 */
@Description(
        name = "vector_scalar_mult",
        value = " Multiply a vector times a scalar"
)
public class VectorMult extends GenericUDF{
    private ListObjectInspector listInspector;
    private MapObjectInspector mapInspector;
    private PrimitiveObjectInspector valueInspector;
    private PrimitiveObjectInspector scalarInspector;

    private StandardListObjectInspector retListInspector;
    private StandardMapObjectInspector retMapInspector;

    public Object evaluateList(Object listObj, double scalar) {
        Object retList = retListInspector.create(0);
        for (int i = 0; i < listInspector.getListLength(listObj); ++i) {
            Object listVal = this.listInspector.getListElement(listObj, i);
            double listDbl = NumericUtil.getNumericValue(valueInspector, listVal);
            double newVal = listDbl * scalar;
            retListInspector.set(retList, i, NumericUtil.castToPrimitiveNumeric(newVal,
                    ((PrimitiveObjectInspector) retListInspector.getListElementObjectInspector()).getPrimitiveCategory()));
        }
        return retList;
    }

    public Object evaluateMap(Object uninspMapObj, double scalar) {
        Object retMap = retMapInspector.create();
        Map map = mapInspector.getMap(uninspMapObj);
        for (Object mapKey : map.keySet()) {
            Object mapValObj = map.get(mapKey);
            double mapValDbl = NumericUtil.getNumericValue(valueInspector, mapValObj);

            double newVal = mapValDbl * scalar;
            Object stdKey = ObjectInspectorUtils.copyToStandardJavaObject(mapKey,
                    mapInspector.getMapKeyObjectInspector());
            Object stdVal = NumericUtil.castToPrimitiveNumeric(newVal,
                    ((PrimitiveObjectInspector) retMapInspector.getMapValueObjectInspector()).getPrimitiveCategory());
            retMapInspector.put(retMap, stdKey, stdVal);

        }
        return retMap;
    }
    @Override
    public ObjectInspector initialize(ObjectInspector[] objectInspectors) throws UDFArgumentException {
        if (objectInspectors.length != 2)
            usage("Must have two arguments.");

        if (objectInspectors[0].getCategory() == ObjectInspector.Category.MAP) {
            this.mapInspector = (MapObjectInspector) objectInspectors[0];

            if (mapInspector.getMapKeyObjectInspector().getCategory() != ObjectInspector.Category.PRIMITIVE)
                usage("Vector map key must be a primitive");

            if (mapInspector.getMapValueObjectInspector().getCategory() != ObjectInspector.Category.PRIMITIVE)
                usage("Vector map value must be a primitive");

            this.valueInspector = (PrimitiveObjectInspector) mapInspector.getMapValueObjectInspector();
        } else if (objectInspectors[0].getCategory() == ObjectInspector.Category.LIST) {
            this.listInspector = (ListObjectInspector) objectInspectors[0];

            if (listInspector.getListElementObjectInspector().getCategory() != ObjectInspector.Category.PRIMITIVE)
                usage("Vector array value must be a primitive");

            this.valueInspector = (PrimitiveObjectInspector) listInspector.getListElementObjectInspector();
        } else {
            usage("First argument must be an array or map");
        }

        if (!NumericUtil.isNumericCategory(valueInspector.getPrimitiveCategory())) {
            usage(" Vector values must be numeric");
        }
        if (objectInspectors[1].getCategory() != ObjectInspector.Category.PRIMITIVE) {
            usage(" scalar needs to be a primitive type.");
        }
        this.scalarInspector = (PrimitiveObjectInspector) objectInspectors[1];
        if (!NumericUtil.isNumericCategory(scalarInspector.getPrimitiveCategory())) {
            usage(" Scalar needs to be a numeric type");
        }


        if (listInspector != null) {
            retListInspector = ObjectInspectorFactory.getStandardListObjectInspector(
                    ObjectInspectorUtils.getStandardObjectInspector(valueInspector,
                            ObjectInspectorUtils.ObjectInspectorCopyOption.JAVA));
            return retListInspector;
        } else {
            retMapInspector = ObjectInspectorFactory.getStandardMapObjectInspector(
                    ObjectInspectorUtils.getStandardObjectInspector(mapInspector.getMapKeyObjectInspector(),
                            ObjectInspectorUtils.ObjectInspectorCopyOption.JAVA),
                    ObjectInspectorUtils.getStandardObjectInspector(valueInspector,
                            ObjectInspectorUtils.ObjectInspectorCopyOption.JAVA));
            return retMapInspector;
        }
    }

    @Override
    public Object evaluate(DeferredObject[] deferredObjects) throws HiveException {
        double dbl = NumericUtil.getNumericValue(scalarInspector, deferredObjects[1].get());
        if (listInspector != null) {
            return evaluateList(deferredObjects[0].get(), dbl);
        } else {
            return evaluateMap(deferredObjects[0].get(), dbl);
        }
    }

    @Override
    public String getDisplayString(String[] strings) {
        return "vector_scalar_mult";
    }

    private void usage(String message) throws UDFArgumentException {
        throw new UDFArgumentException("vector_scalar_mult: Multiply a vector times a scalar value : " + message);
    }
}
