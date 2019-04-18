使用百度WebUploader js框架进行大文件分片上传
WebUploader的api文档地址： http://fex.baidu.com/webuploader/doc/index.html

1、边上传边合并
前端将大文件分片上传，后端服务接收分片文件直接合并到最终文件

地址：http://localhost:8082/filewithmerge.html

2、先上传，再合并
大文件上传的主要思路是将文件在前端进行分片，然后将分片后的各部分文件分别按顺序上传到服务端(api:/upload/part)，待全部分片的文件上传完成之后，调用合并接口(api:/upload/merge)把文件所有part文件合并成最终文件

地址：http://localhost:8082/filewithpart.html
