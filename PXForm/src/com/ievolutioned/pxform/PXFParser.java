package com.ievolutioned.pxform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PXFParser {
    private PXFParserEventHandler eventHandler;

    public interface PXFParserEventHandler{
        public abstract void finish(PXFAdapter adapter, String json);
        public abstract void error(Exception ex, String json);
    }

    public PXFParser(Activity activity, PXFParserEventHandler callback){
        eventHandler = callback;
    }

    public void parseJson(final Activity activity, final String json){
        AsyncTask<Void, Void, Void> t1 = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params1234) {
                JsonElement json_tmp;
                final List<PXWidget> w = new ArrayList<PXWidget>();

                try{
                    json_tmp = new JsonParser().parse(json);
                }catch(final Exception ex){
                    activity.runOnUiThread(new Runnable() { @Override public void run() {
                        ex.printStackTrace();

                        if(eventHandler != null){
                            eventHandler.error(ex, json);
                        }
                    }});
                    return null;
                }

                if(json_tmp.isJsonNull()){
                    activity.runOnUiThread(new Runnable() { @Override public void run() {
                        if(eventHandler != null){
                            eventHandler.error(new Exception("Json is not valid"), json);
                        }
                    }});
                    return null;
                }

                if(json_tmp.isJsonArray()){
                    JsonArray array = json_tmp.getAsJsonArray();

                    for(int i = 0; i < array.size(); ++i){
                        JsonElement element = array.get(i);

                        if(!element.isJsonObject())
                            continue;

                        JsonObject entry = element.getAsJsonObject();
                        final Map<String, Map.Entry<String,JsonElement>> map =
                                new HashMap<String, Map.Entry<String,JsonElement>>();

                        //map all the fields by key
                        for(Map.Entry<String,JsonElement> mej : entry.entrySet()){
                            map.put(mej.getKey(), mej);
                        }

                        PXWidget px = getWidgetFromType(map);
                        w.add(px);
                        try {
                            //let the thread rest
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    activity.runOnUiThread(new Runnable() {  @Override public void run() {
                        if(eventHandler != null){
                            eventHandler.error(new Exception("Json is not valid"), json);
                        }
                    }});
                }

                final JsonElement json_tmp_copy = json_tmp;
                activity.runOnUiThread(new Runnable() { @Override public void run() {
                    if(eventHandler != null){
                        PXFAdapter adapter = new PXFAdapter(activity, w);
                        eventHandler.finish(adapter, json);
                    }
                }});

                return null;
            }
        };
        t1.execute();
    }

    public static String parseFileToString(Context context, String filename )
    {
        try
        {
            InputStream stream = context.getAssets().open( filename );
            int size = stream.available();

            byte[] bytes = new byte[size];
            stream.read(bytes);
            stream.close();

            return new String( bytes, "UTF-8" );

        } catch ( IOException e ) {
            Log.i("PXForm", "IOException: " + e.getMessage() );
        }
        return null;
    }

    private static PXWidget getWidgetFromType(final Map<String, Map.Entry<String,JsonElement>> map){
        PXWidget widget = null;

        //we got a well defined field
        if(map.containsKey(PXWidget.FIELD_TYPE)){
            if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
                    .equals(PXWidget.FIELD_TYPE_TEXT)){
                widget = new PXFEdit(map);
            }
            else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
                    .equals(PXWidget.FIELD_TYPE_BOOLEAN)){
                widget = new PXFCheckBox(map);
            }
            else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
                    .equals(PXWidget.FIELD_TYPE_DATE)){
                widget = new PXFDatePicker(map);
            }
            else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
                    .equals(PXWidget.FIELD_TYPE_LONGTEXT)){
                widget = new PXFEdit(map);
            }
            else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
                    .equals(PXWidget.FIELD_TYPE_UNSIGNED)){
                widget = new PXFEdit(map);
            }
        }
        else if(map.containsKey(PXWidget.FIELD_OPTIONS)){
            if (map.containsKey(PXWidget.FIELD_CELL) &&
                    isCELL_OPTION_SEGMENT(map.get(PXWidget.FIELD_CELL).getValue())) {
                widget = new PXFToggleBoolean(map);
            } else {
                widget = new PXFSpinner(map);
            }
        }
        else if(map.containsKey(PXWidget.FIELD_ACTION)){
            widget = new PXFButton(map);
        }

        return widget == null ? new PXFUnknownControlType(map) : widget;
    }

    private static boolean isCELL_OPTION_SEGMENT(JsonElement cell){
        return PXWidget.FIELD_CELL_OPTION_SEGMENT.equalsIgnoreCase(cell.getAsString()) ||
                PXWidget.FIELD_CELL_OPTION_SEGMENT_CUSTOM.equalsIgnoreCase(cell.getAsString());
    }

    //private static PXWidget getWidgetFromType(final Activity context,
    //                                          final Map<String, Map.Entry<String,JsonElement>> map){
    //    PXWidget widget = null;
    //
    //    //we got a well defined field
    //    if(map.containsKey(PXWidget.FIELD_TYPE)){
    //        if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
    //                .equals(PXWidget.FIELD_TYPE_TEXT)){
    //            widget = new PXFEdit(map);
    //        }
    //        else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
    //                .equals(PXWidget.FIELD_TYPE_BOOLEAN)){
    //            widget = new PXFCheckBox(map);
    //        }
    //        else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
    //                .equals(PXWidget.FIELD_TYPE_DATE)){
    //            widget = new PXFDatePicker(map);
    //        }
    //        else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
    //                .equals(PXWidget.FIELD_TYPE_LONGTEXT)){
    //            widget = new PXFEdit(map);
    //        }
    //        else if(map.get(PXWidget.FIELD_TYPE).getValue().getAsString()
    //                .equals(PXWidget.FIELD_TYPE_UNSIGNED)){
    //            widget = new PXFEdit(map);
    //        }
    //    }
    //    else if(map.containsKey(PXWidget.FIELD_OPTIONS)){
    //        if (map.containsKey(PXWidget.FIELD_CELL) &&
    //                isCELL_OPTION_SEGMENT(map.get(PXWidget.FIELD_CELL).getValue())) {
    //            widget = new PXFToggleBoolean(map);
    //        } else {
    //            widget = new PXFSpinner(map);
    //        }
    //    }
    //    else if(map.containsKey(PXWidget.FIELD_ACTION)){
    //        widget = new PXFButton(map);
    //    }
    //
    //    return widget == null ? new PXFUnknownControlType(map) : widget;
    //}
    //
    //private static boolean isCELL_OPTION_SEGMENT(JsonElement cell){
    //    return PXWidget.FIELD_CELL_OPTION_SEGMENT.equalsIgnoreCase(cell.getAsString()) ||
    //            PXWidget.FIELD_CELL_OPTION_SEGMENT_CUSTOM.equalsIgnoreCase(cell.getAsString());
    //}
}
