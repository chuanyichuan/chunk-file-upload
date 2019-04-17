package com.example.springbootbigfile;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FileUploadController {
    @Resource
    private FileUploadUtils fileUploadUtils;

    @RequestMapping(value = "/upload/withmerge")
    public void uploadWithMerge(HttpServletRequest request, HttpServletResponse response, MultipartFile file, String id,
                                String fileName) {
        if (file != null) {
            try {
                fileUploadUtils.uploadWithMerge(file.getInputStream(), id, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(value = "/upload/part")
    public void uploadPart(HttpServletRequest request, HttpServletResponse response, MultipartFile file, String sysId,
                           Integer chunk) {
        if (file != null) {
            try {
                fileUploadUtils.uploadTmpFilePart(file.getInputStream(), sysId, chunk == null ? 0 : chunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(value = "/upload/merge")
    @ResponseBody
    public JsonResult<Object> uploadPart(String sysId, String fileName) {
        fileUploadUtils.mergeTmpFilePart(sysId, sysId, fileName);
        return JsonResult.success();
    }

}
