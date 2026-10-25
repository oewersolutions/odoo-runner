package dev.odoorunner;

import com.google.gson.JsonParser;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Official exact-project lookup; PyPI does not offer the former pip-search endpoint. */
final class PyPiLookup {
    record Result(String name,String version,String summary,String python){}
    static Result find(String name)throws Exception{
        if(!name.matches("[A-Za-z0-9][A-Za-z0-9._-]*"))throw new IllegalArgumentException("Enter an exact PyPI package name, such as requests.");
        HttpURLConnection connection=(HttpURLConnection)URI.create("https://pypi.org/pypi/"+name+"/json").toURL().openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(10000);connection.setRequestProperty("Accept","application/json");
        try{int code=connection.getResponseCode();if(code==404)throw new IllegalArgumentException("No PyPI package found with that exact name.");if(code!=200)throw new IllegalArgumentException("PyPI returned HTTP "+code);
            try(var input=connection.getInputStream()){byte[] bytes=input.readNBytes(4*1024*1024+1);if(bytes.length>4*1024*1024)throw new IllegalArgumentException("Package metadata is too large; enter its package/version directly below.");var info=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("info");return new Result(info.get("name").getAsString(),info.get("version").getAsString(),info.get("summary").isJsonNull()?"":info.get("summary").getAsString(),info.get("requires_python").isJsonNull()?"":info.get("requires_python").getAsString());}
        }finally{connection.disconnect();}
    }
}
