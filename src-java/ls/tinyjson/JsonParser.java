package ls.tinyjson;

import com.fasterxml.jackson.core.JsonToken;

import clojure.lang.IFn;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class JsonParser {

    private final com.fasterxml.jackson.core.JsonParser jp;
    private final IFn arrayReader;
    private final IFn mapReader;

    public JsonParser(com.fasterxml.jackson.core.JsonParser jp,
                      IFn arrayReader, IFn mapReader) {
        this.jp = jp;
	this.arrayReader = arrayReader;
	this.mapReader = mapReader;
    }

    private Object parseLong() throws IOException {

        Object val;
        try {
            val = jp.getLongValue();
        }catch(IOException e) {
            val = new BigInteger(jp.getText());
        }

        return val;
    }

    public Object parse() throws IOException {
        if(jp.nextToken() == null)
            throw new EOFException();
        else
            return parseVal();
    }

    public Object parseVal() throws IOException {

        switch(jp.getCurrentToken()) {
            case START_OBJECT:
                return parseMap();
            case START_ARRAY:
                return parseArray();
            case FIELD_NAME:
                return jp.getText();
            case VALUE_STRING:
                return jp.getText();
            case VALUE_NUMBER_INT:
                return parseLong();
            case VALUE_NUMBER_FLOAT:
                return jp.getDoubleValue();
            case VALUE_TRUE:
                return true;
            case VALUE_FALSE:
                return false;
            case VALUE_NULL:
                return null;
            default: return null;
        }
    }

    public Object parseMap() throws IOException {

        Object m = mapReader.invoke();

        while(jp.nextToken() != JsonToken.END_OBJECT) {
            Object key = parseVal();
	    jp.nextToken();
	    Object val = parseVal();
            m = mapReader.invoke(m, key, val);
        }

        return mapReader.invoke(m);
    }

    public Object parseArray() throws IOException {
	Object a = arrayReader.invoke();

        while (jp.nextToken() != JsonToken.END_ARRAY) {
	    a = arrayReader.invoke(a, parseVal());
	}

	return arrayReader.invoke(a);
    }
}
