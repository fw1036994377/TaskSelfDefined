package com.dfjx.diy.sync.writer;


import com.alibaba.fastjson.JSONObject;
import com.dfjx.diy.param.Param;
import com.dfjx.diy.param.writer.HdfsWriterParam;
import com.dfjx.diy.sync.Sync;
import com.tencent.taskrunner.FieldsModel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 打成jar包放到49上运行
 */
public class HdfsWriterTask extends WriterTask {
    String time;
    final AtomicInteger order = new AtomicInteger(0);

    public  void init(Param param, BlockingQueue<Object> queue, Sync sync){
        super.init(param,queue,sync);
        time = String.valueOf(System.currentTimeMillis());
    };

    @Override
    void consume(Object object) {
        //do nothing
    }

    @Override
    void consumeBatch(Object[] objects) {
        //这里需要适配MppHdfsHandler来转换类型，暂时不适配,写死
        List<JSONObject> jsonObjectList = null;
        for (Object object : objects) {
            jsonObjectList.add((JSONObject)object);
        }
        HdfsWriterParam hdfsWriterParam = (HdfsWriterParam) this.param;

        String path = hdfsWriterParam.path;
        String dataFile = createDataFile(path);
        System.out.println("hdfs consume: createDataFile:dataFile :"+dataFile);
        printDataToDisk(jsonObjectList,dataFile);
        System.out.println("hdfs consume: printDataToDisk success");

        URI uri = null;
        try {
//            uri = new URI("hdfs://172.19.1.14:9000");
//            uri = new URI("hdfs://172.19.1.49:8020");
//            uri = new URI("hdfs://172.19.1.14:8020");
            uri = new URI(hdfsWriterParam.url);
        } catch (URISyntaxException e) {
            System.out.println("uri exception");
            e.printStackTrace();
        }

        Configuration conf = new Configuration();
//      conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
//      conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");

//        conf.set("hadoop.security.authentication","tbds");
//        conf.set("hadoop_security_authentication_tbds_secureid","0Hr0is06in4hxBu2V6vU0DAMFw7BT50pC4V5");
//        conf.set("hadoop_security_authentication_tbds_username","wayne");
//        conf.set("hadoop_security_authentication_tbds_securekey","ItWu35remPNQFYhe4Szr58Id2PVbg7cM");
        conf.set("hadoop.security.authentication","tbds");
        conf.set("hadoop_security_authentication_tbds_secureid",hdfsWriterParam.secureid);
        conf.set("hadoop_security_authentication_tbds_username",hdfsWriterParam.username);
        conf.set("hadoop_security_authentication_tbds_securekey",hdfsWriterParam.securekey);

        //kerberos
        UserGroupInformation.setConfiguration(conf);
        try {
            UserGroupInformation.loginUserFromSubject(null);
            syncDataToHdfsForCtsdb(dataFile, dataFile,
                    uri,conf);
            System.out.println("hdfs consume: syncDataToHdfsForCtsdb success");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }



    /*
     * 生成文件路径
     */
    public String  createDataFile(String path){
        String filePath = "";
        try {
            File parentFile =new File(path);
            if(!parentFile.exists()){//如果文件夹不存在
                parentFile.mkdirs();//创建文件夹
            }
            filePath = path+"/mpp_"+time+"_"+ order.get() +".txt";
            File f = new File(filePath);
            if(!f.exists()){//如果文件夹不存在
                f.createNewFile();
                order.incrementAndGet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }


    public void printDataToDisk(List<JSONObject> result, String path){

        PrintWriter w = null;
        FileOutputStream out = null;
        try {
//			w = new PrintWriter("/root/kafkatest/db_data.txt");
            out = new FileOutputStream(path,true);
            w = new PrintWriter(out);
            for(JSONObject jsonObject:result){
                w.println(jsonObject.toJSONString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(w!=null){
                w.close();
            }
            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将生成的所有文件put到hdfs里面
     * @param hdfsOutput
     * @param dataInput
     * @param uri
     * @param conf
     * @throws Exception
     */
    public static void syncDataToHdfsForCtsdb(String hdfsOutput, String dataInput,
                                              URI uri,
                                              Configuration conf) throws Exception{

        FileSystem fs = null ;
        try {
//            Configuration conf = new Configuration();
//            conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
//            conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
//
//            String tbds_username = params.get("tbds_username");
//            String tbds_secureid = params.get("tbds_secureid");
//            String tbds_securekey = params.get("tbds_securekey");
//            if(tbds_username != null && tbds_username.trim().length() > 0){
//                //加入tbds的认证参数
//                conf.set("hadoop.security.authentication", "tbds");
//                conf.set("hadoop_security_authentication_tbds_username",tbds_username);
//                conf.set("hadoop_security_authentication_tbds_secureid",tbds_secureid);
//                conf.set("hadoop_security_authentication_tbds_securekey",tbds_securekey);
//                UserGroupInformation.setConfiguration(conf);
//                UserGroupInformation.loginUserFromSubject(null);
//            }
            //拿到一个文件系统操作的客户端实例对象
//            fs = FileSystem.get(conf) ;   //demo
            fs = FileSystem.get(uri, conf);  // 我改的


            Path src = new Path(new File(dataInput).getPath());
            // 要上传到hdfs的目标路径
            Path dst = new Path(hdfsOutput);
            fs.copyFromLocalFile(src, dst);

            fs.close();
        } catch (Exception e){
            System.out.println("------------------------------数据文件上传到hdfs异常！");
            e.printStackTrace();
            throw new Exception(e);
        }
    }



/*
    conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));

//参数 tbds_username，tbds_username，tbds_securekey通过工作流自定义参数传入

conf.set("hadoop.security.authentication", "tbds");
conf.set("hadoop_security_authentication_tbds_username",tbds_username);
conf.set("hadoop_security_authentication_tbds_secureid",tbds_secureid);
conf.set("hadoop_security_authentication_tbds_securekey",tbds_securekey);
UserGroupInformation.setConfiguration( conf );
UserGroupInformation.loginUserFromSubject(null);
*/

}
