package com.dfjx.diy.param.reader;

import com.dfjx.diy.param.Param;

import java.lang.reflect.Field;

public class MppReaderParam extends ReaderParam {

    public String getWorkerClass(){
        return "com.dfjx.diy.sync.reader.MppReaderTask";
    }

/*    String driver = "org.postgresql.Driver";
    //        String url = "jdbc:postgresql://localhost:5432/pgsqltest";
    String url = "jdbc:postgresql://10.1.3.40:7300/wayne";
    String username = "wayne";
    String password = "wayne";*/

    public String driver= "org.postgresql.Driver";
    //        String url = "jdbc:postgresql://localhost:5432/pgsqltest";
    public String url;// = "jdbc:postgresql://10.1.3.40:7300/wayne";
    public String username;// = "wayne";
    public String password;// = "wayne";
    public String sql;//"select * from wayne";






//    public static void main(String[] args) {
//        MppReaderParam mppReaderParam = new MppReaderParam();
//        mppReaderParam.setUsername("wayne");
//        Param param = mppReaderParam;
//        Field[] declaredFields = param.getClass().getDeclaredFields();
//        System.out.println("a");
//    }
}
