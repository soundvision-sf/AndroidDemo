package com.soundvision.demo.location.ffgeojson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class GeometryDeserializer <T extends IGeometry> implements JsonDeserializer<T> {

    public T deserialize(final JsonElement jsonElement, final Type type,
                         final JsonDeserializationContext deserializationContext
    ) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement className = jsonObject.get("type");
        //final JsonPrimitive prim = (JsonPrimitive) jsonObject.get(elem.toString());
        //final String className = prim.getAsString();
        final Class<T> clazz = getClassInstance(className.toString());
        return deserializationContext.deserialize(jsonObject, clazz);
    }

    @SuppressWarnings("unchecked")
    public Class<T> getClassInstance(String className) {
        try {
            switch (className)
            {
                case "\"Point\"":
                    return (Class<T>) GeometryPoint.class;
                case "\"LineString\"":
                    return  (Class<T>) GeometryLineString.class;
                case "\"Polygon\"":
                    return  (Class<T>) GeometryPolygon.class;
            }
            return (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw new JsonParseException(cnfe.getMessage());
        }
    }
}
