Photo_Watermark_1模块：是一个命令行程序，为图片添加水印，生成的文件夹放在resource目录下，文件名为原文件名＋watermark后缀

tips:使用IDEA连接github时候可能出现网络问题，整个流程以及解决方法有:
    1.假设IDEA里有(开发完毕)了一个项目为Large-language-models.
    2.打开IDEA,左上角FIle -> Settings ,搜索github,点击+，添加github账户(可能需要梯子)
    3.在IDEA里设置版本控制器,打开需要上传的项目，点击VCS -> import into Version Control -> share project on github(这时会自动生成一个远程仓库,并建立链接)
    4.此后就可以正常commit和push,update-project了,如果出现网络问题，可以点击电脑右下角网络点击右键 -> 打开网络和Internet设置，复制代理地址，端口。打开git-bash，输入git config --global http.proxy http://127.0.0.1:7890 设置代理。

