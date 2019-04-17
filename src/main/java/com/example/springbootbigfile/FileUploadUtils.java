package com.example.springbootbigfile;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author cc
 */
@Component
public class FileUploadUtils {

    public static final ScheduledExecutorService SCHEDULED_THREAD_POOL = Executors.newScheduledThreadPool(10);

    @Value("${uploadTmpFolder}")
    private String                               tmpPath;
    @Value("${uploadSaveFolder}")
    private String                               savePath;

    /**
     * 分片临时文件路径组合规则
     */
    private String                               tmpFileNameRule       = null;
    private String                               tmpFilePathRule       = null;

    @PostConstruct
    public void init() {
        tmpFileNameRule = tmpPath + "%s" + File.separator + "%s_%s.part";
        tmpFilePathRule = tmpPath + "%s" + File.separator;
    }

    /**
     * 分片文件上传成功之后立刻合并到主文件，并删除分片文件，这种方式仅允许单次连续上传，若上传出现中断，则需要重新上传
     *
     * @param inputStream
     * @param destPath
     * @param destFileName
     */
    public void uploadWithMerge(InputStream inputStream, String destPath, String destFileName) {
        System.out.println("**********uploadWithMerge**********");
        try {
            // 临时目录用来存放分片文件
            String tmpFileName = UUID.randomUUID().toString();
            String tmpFilePath = String.format(tmpFilePathRule, destPath);
            File tempPartFile = new File(tmpFilePath + tmpFileName);
            if (!tempPartFile.getParentFile().exists()) {
                tempPartFile.getParentFile().mkdirs();
            }
            System.out.println("=======tmpFile======" + tmpFilePath + tmpFileName);
            FileUtils.copyInputStreamToFile(inputStream, tempPartFile);
            // 合并到目标文件
            String destFile = savePath + destPath + File.separator + destFileName;
            System.out.println(destFile);
            File destTempFile = new File(destFile);
            if (!destTempFile.exists()) {
                //创建文件的上级目录
                if (!destTempFile.getParentFile().exists()) {
                    destTempFile.getParentFile().mkdirs();
                }
                //创建文件
                destTempFile.createNewFile();
            }

            //将分片文件累加到最终文件中
            FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
            FileUtils.copyFile(tempPartFile, destTempfos);
            // 关闭写入文件流
            destTempfos.close();

            // 删除临时目录中的分片文件
            FileUtils.deleteQuietly(tempPartFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传分片文件
     *
     * @param inputStream
     * @param partGroupName 分片文件所属组
     * @param partIndex 分片文件序号(按照上传顺序进行)
     */
    public void uploadTmpFilePart(final InputStream inputStream, final String partGroupName, final int partIndex) {
        System.out.println("**********uploadTmpFilePart**********");
        SCHEDULED_THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 临时目录用来存放分片文件
                    String tmpFilePath = String.format(tmpFileNameRule, partGroupName, partGroupName, partIndex);
                    File tempPartFile = new File(tmpFilePath);
                    if (!tempPartFile.getParentFile().exists()) {
                        tempPartFile.getParentFile().mkdirs();
                    }
                    System.out.println("=======uploadTmpFilePart-tmpFile======" + tmpFilePath);
                    FileUtils.copyInputStreamToFile(inputStream, tempPartFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 合并分片的临时文件到主文件
     *
     * @param partGroupName 分片文件所属组(分片上传时指定的组名)
     * @param destPath 目标合成文件所在目录(相对路径)
     * @param destFileName 目标合成文件名称(带后缀的完整文件名称,eg:cc.zip)
     */
    public void mergeTmpFilePart(final String partGroupName, final String destPath, final String destFileName) {
        System.out.println("**********mergeTmpFilePart**********");
        SCHEDULED_THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {

                String tmpFilePath = String.format(tmpFilePathRule, partGroupName);
                File tmpFileDir = new File(tmpFilePath);
                if (!tmpFileDir.exists() || !tmpFileDir.isDirectory()) {
                    throw new RuntimeException();
                }
                // 扫描出.part结尾的临时文件
                File[] partFiles = tmpFileDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String name) {
                        return name.endsWith(".part");
                    }
                });
                if (partFiles.length == 0) {
                    return;
                }
                List<File> fileList = Arrays.asList(partFiles);
                // 对文件进行排序
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        String fileName1 = file1.getName();
                        String fileName2 = file2.getName();
                        String index1 = fileName1.substring(fileName1.lastIndexOf("_") + 1, fileName1.lastIndexOf("."));
                        String index2 = fileName2.substring(fileName2.lastIndexOf("_") + 1, fileName2.lastIndexOf("."));
                        return Integer.parseInt(index1) > Integer.parseInt(index2) ? 1 : -1;
                    }
                });
                // 目标文件存储路径
                String destFilePath = savePath + destPath + File.separator + destFileName;
                System.out.println("mergeTmpFilePart====destFilePath = " + destFilePath);
                try {
                    File destFile = new File(destFilePath);
                    if (!destFile.exists()) {
                        //创建文件的上级目录
                        if (!destFile.getParentFile().exists()) {
                            destFile.getParentFile().mkdirs();
                        }
                        //创建文件
                        destFile.createNewFile();
                    }
                    FileOutputStream destTempfos;
                    for (int i = 0; i < fileList.size(); i++) {
                        File partFile = fileList.get(i);
                        destTempfos = new FileOutputStream(destFile, true);
                        //遍历"所有分片文件"到"最终文件"中
                        FileUtils.copyFile(partFile, destTempfos);
                        destTempfos.close();
                    }
                    // 删除临时目录中的分片文件
                    FileUtils.deleteDirectory(tmpFileDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
